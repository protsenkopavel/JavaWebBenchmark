package net.protsenko.common.benchmark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkResult {

    private String moduleName;
    private String scenarioName;

    private Duration totalDuration;
    private Duration averageLatency;
    private Duration p50Latency;
    private Duration p95Latency;
    private Duration p99Latency;

    private long totalOperations;
    private double operationsPerSecond;

    private long heapUsedBefore;
    private long heapUsedAfter;
    private long heapDelta;
    private long peakThreadCount;

    private Map<String, Object> additionalMetrics;

    public void printSummary() {
        System.out.println("═".repeat(60));
        System.out.printf("Module: %s | Scenario: %s%n", moduleName, scenarioName);
        System.out.println("─".repeat(60));
        System.out.printf("Total duration:     %,d ms%n", totalDuration.toMillis());
        System.out.printf("Operations:         %,d%n", totalOperations);
        System.out.printf("Throughput:         %.2f ops/sec%n", operationsPerSecond);
        System.out.println("─".repeat(60));
        System.out.printf("Avg latency:        %,d ms%n", averageLatency.toMillis());
        System.out.printf("P50 latency:        %,d ms%n", p50Latency.toMillis());
        System.out.printf("P95 latency:        %,d ms%n", p95Latency.toMillis());
        System.out.printf("P99 latency:        %,d ms%n", p99Latency.toMillis());
        System.out.println("─".repeat(60));
        System.out.printf("Heap before:        %,d MB%n", heapUsedBefore / 1024 / 1024);
        System.out.printf("Heap after:         %,d MB%n", heapUsedAfter / 1024 / 1024);
        System.out.printf("Heap delta:         %,d MB%n", heapDelta / 1024 / 1024);
        System.out.printf("Peak threads:       %,d%n", peakThreadCount);
        System.out.println("═".repeat(60));
    }
}
