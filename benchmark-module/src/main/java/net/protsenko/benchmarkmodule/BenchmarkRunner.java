package net.protsenko.benchmarkmodule;

import net.protsenko.benchmarkmodule.service.BenchmarkExecutor;
import net.protsenko.common.benchmark.BenchmarkResult;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "benchmark",
        description = "Run I/O benchmark across sync, webflux, and loom modules",
        mixinStandardHelpOptions = true,
        version = "1.0"
)
public class BenchmarkRunner implements Callable<Integer> {

    @CommandLine.Option(names = {"-r", "--requests"}, description = "Total requests per module", defaultValue = "1000")
    private int totalRequests;

    @CommandLine.Option(names = {"-c", "--concurrency"}, description = "Concurrent requests", defaultValue = "100")
    private int concurrency;

    @CommandLine.Option(names = {"-s", "--scenario"}, description = "Scenario: aggregation, db, all", defaultValue = "all")
    private String scenario;

    @CommandLine.Option(names = {"--sync-port"}, description = "Sync module port", defaultValue = "8081")
    private int syncPort;

    @CommandLine.Option(names = {"--webflux-port"}, description = "WebFlux module port", defaultValue = "8082")
    private int webfluxPort;

    @CommandLine.Option(names = {"--loom-port"}, description = "Loom module port", defaultValue = "8083")
    private int loomPort;

    @CommandLine.Option(names = {"--seed"}, description = "Seed initial products", defaultValue = "true")
    private boolean seed;

    @CommandLine.Option(names = {"--seed-count"}, description = "Number of products to seed", defaultValue = "100")
    private int seedCount;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BenchmarkRunner()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("""
            ╔═══════════════════════════════════════════════════════════════╗
            ║           I/O Benchmark: Sync vs WebFlux vs Loom              ║
            ╚═══════════════════════════════════════════════════════════════╝
            """);

        System.out.printf("Configuration: %d requests, %d concurrent%n", totalRequests, concurrency);
        System.out.printf("Scenarios: %s%n%n", scenario);

        List<BenchmarkResult> allResults = new ArrayList<>();

        record ModuleConfig(String name, String url) {}
        List<ModuleConfig> modules = List.of(
                new ModuleConfig("sync", "http://localhost:" + syncPort),
                new ModuleConfig("webflux", "http://localhost:" + webfluxPort),
                new ModuleConfig("loom", "http://localhost:" + loomPort)
        );

        for (ModuleConfig module : modules) {
            System.out.println("\n" + "═".repeat(60));
            System.out.printf("Testing module: %s (%s)%n", module.name, module.url);
            System.out.println("═".repeat(60));

            BenchmarkExecutor executor = new BenchmarkExecutor(module.url, module.name);

            if (seed) {
                try {
                    System.out.println("Seeding data...");
                    executor.seedData(seedCount);
                } catch (Exception e) {
                    System.err.println("Warning: Could not seed data - " + e.getMessage());
                }
            }

            if (scenario.equals("all") || scenario.equals("aggregation")) {
                System.out.println("\n▶ Running aggregation benchmark...");
                BenchmarkResult result = executor.runAggregationBenchmark(totalRequests, concurrency);
                result.printSummary();
                allResults.add(result);
            }

            if (scenario.equals("all") || scenario.equals("db")) {
                System.out.println("\n▶ Running DB benchmark...");
                BenchmarkResult result = executor.runDbBenchmark(totalRequests, concurrency);
                result.printSummary();
                allResults.add(result);
            }
        }

        printComparison(allResults);

        return 0;
    }

    private void printComparison(List<BenchmarkResult> results) {
        System.out.println("\n");
        System.out.println("╔═══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                              COMPARISON SUMMARY                               ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-12s │ %-20s │ %10s │ %10s │ %10s ║%n",
                "Module", "Scenario", "Throughput", "P95 (ms)", "Heap Δ (MB)");
        System.out.println("╠═══════════════════════════════════════════════════════════════════════════════╣");

        for (BenchmarkResult r : results) {
            System.out.printf("║ %-12s │ %-20s │ %10.1f │ %10d │ %10d ║%n",
                    r.getModuleName(),
                    truncate(r.getScenarioName(), 20),
                    r.getOperationsPerSecond(),
                    r.getP95Latency().toMillis(),
                    r.getHeapDelta() / 1024 / 1024);
        }

        System.out.println("╚═══════════════════════════════════════════════════════════════════════════════╝");

        System.out.println("\n🏆 Winners by throughput:");
        results.stream()
                .filter(r -> r.getScenarioName().contains("aggregation"))
                .max(Comparator.comparingDouble(BenchmarkResult::getOperationsPerSecond))
                .ifPresent(r -> System.out.printf("   Aggregation: %s (%.1f ops/sec)%n",
                        r.getModuleName(), r.getOperationsPerSecond()));

        results.stream()
                .filter(r -> r.getScenarioName().contains("db"))
                .max(Comparator.comparingDouble(BenchmarkResult::getOperationsPerSecond))
                .ifPresent(r -> System.out.printf("   DB ops:      %s (%.1f ops/sec)%n",
                        r.getModuleName(), r.getOperationsPerSecond()));
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
