package net.protsenko.benchmarkmodule.service;

import lombok.extern.slf4j.Slf4j;
import net.protsenko.benchmarkmodule.client.BenchmarkClient;
import net.protsenko.common.benchmark.BenchmarkResult;
import net.protsenko.common.model.Product;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

@Slf4j
public class BenchmarkExecutor {

    private final BenchmarkClient client;
    private final String moduleName;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;

    public BenchmarkExecutor(String baseUrl, String moduleName) {
        this.client = new BenchmarkClient(baseUrl);
        this.moduleName = moduleName;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * Run the aggregation benchmark scenario
     */
    public BenchmarkResult runAggregationBenchmark(int totalRequests, int concurrency) {
        log.info("Running aggregation benchmark: {} requests, {} concurrent",
                totalRequests, concurrency);

        List<Long> productIds = LongStream.rangeClosed(1, 100)
                .boxed()
                .toList();

        log.info("Warming up...");
        warmup(productIds, Math.min(50, totalRequests / 10));

        System.gc();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        long heapBefore = memoryBean.getHeapMemoryUsage().getUsed();
        long initialThreadCount = threadBean.getThreadCount();
        long peakThreadCount = initialThreadCount;

        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);

        long startTime = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < totalRequests; i++) {
            Long productId = productIds.get(random.nextInt(productIds.size()));
            futures.add(executor.submit(() -> {
                long reqStart = System.nanoTime();
                try {
                    client.getAggregation(productId);
                    long latency = System.nanoTime() - reqStart;
                    latencies.add(latency);
                } catch (IOException e) {
                    log.error("Request failed", e);
                }
            }));

            if (i % 100 == 0) {
                peakThreadCount = Math.max(peakThreadCount, threadBean.getThreadCount());
            }
        }

        for (Future<?> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Request failed", e);
            }
        }

        long endTime = System.nanoTime();
        executor.shutdown();

        long heapAfter = memoryBean.getHeapMemoryUsage().getUsed();
        peakThreadCount = Math.max(peakThreadCount, threadBean.getThreadCount());

        Duration totalDuration = Duration.ofNanos(endTime - startTime);
        Collections.sort(latencies);

        return BenchmarkResult.builder()
                .moduleName(moduleName)
                .scenarioName("aggregation-" + concurrency + "-concurrent")
                .totalDuration(totalDuration)
                .totalOperations(latencies.size())
                .operationsPerSecond(latencies.size() * 1_000_000_000.0 / (endTime - startTime))
                .averageLatency(Duration.ofNanos((long) latencies.stream()
                        .mapToLong(Long::longValue).average().orElse(0)))
                .p50Latency(Duration.ofNanos(percentile(latencies, 50)))
                .p95Latency(Duration.ofNanos(percentile(latencies, 95)))
                .p99Latency(Duration.ofNanos(percentile(latencies, 99)))
                .heapUsedBefore(heapBefore)
                .heapUsedAfter(heapAfter)
                .heapDelta(heapAfter - heapBefore)
                .peakThreadCount(peakThreadCount)
                .build();
    }

    /**
     * Run DB operations benchmark
     */
    public BenchmarkResult runDbBenchmark(int totalOperations, int concurrency) {
        log.info("Running DB benchmark: {} operations, {} concurrent",
                totalOperations, concurrency);

        warmupDb(Math.min(20, totalOperations / 10));

        System.gc();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        long heapBefore = memoryBean.getHeapMemoryUsage().getUsed();

        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);

        long startTime = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < totalOperations; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                long reqStart = System.nanoTime();
                try {
                    if (idx % 5 == 0) {
                        Product p = Product.builder()
                                .name("BenchProduct-" + idx)
                                .description("Benchmark test product")
                                .price(java.math.BigDecimal.valueOf(random.nextDouble() * 100))
                                .build();
                        client.createProduct(p);
                    } else {
                        client.getProduct((long) (random.nextInt(100) + 1));
                    }
                    latencies.add(System.nanoTime() - reqStart);
                } catch (IOException e) {
                    log.debug("DB operation failed (might be expected): {}", e.getMessage());
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }

        long endTime = System.nanoTime();
        executor.shutdown();

        long heapAfter = memoryBean.getHeapMemoryUsage().getUsed();
        Collections.sort(latencies);

        return BenchmarkResult.builder()
                .moduleName(moduleName)
                .scenarioName("db-ops-" + concurrency + "-concurrent")
                .totalDuration(Duration.ofNanos(endTime - startTime))
                .totalOperations(latencies.size())
                .operationsPerSecond(latencies.size() * 1_000_000_000.0 / (endTime - startTime))
                .averageLatency(Duration.ofNanos((long) latencies.stream()
                        .mapToLong(Long::longValue).average().orElse(0)))
                .p50Latency(Duration.ofNanos(percentile(latencies, 50)))
                .p95Latency(Duration.ofNanos(percentile(latencies, 95)))
                .p99Latency(Duration.ofNanos(percentile(latencies, 99)))
                .heapUsedBefore(heapBefore)
                .heapUsedAfter(heapAfter)
                .heapDelta(heapAfter - heapBefore)
                .peakThreadCount(threadBean.getThreadCount())
                .build();
    }

    private void warmup(List<Long> productIds, int count) {
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            try {
                client.getAggregation(productIds.get(random.nextInt(productIds.size())));
            } catch (IOException ignored) {}
        }
    }

    private void warmupDb(int count) {
        for (int i = 0; i < count; i++) {
            try {
                client.getProduct((long) (i % 100 + 1));
            } catch (IOException ignored) {}
        }
    }

    private long percentile(List<Long> sortedList, int percentile) {
        if (sortedList.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
        return sortedList.get(Math.max(0, Math.min(index, sortedList.size() - 1)));
    }

    public void seedData(int count) throws IOException {
        client.seedProducts(count);
    }
}
