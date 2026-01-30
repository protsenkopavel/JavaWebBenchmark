package net.prosenko.mockserver.controller;

import lombok.extern.slf4j.Slf4j;
import net.protsenko.common.model.ExternalResponses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/api")
public class MockServiceController {

    @Value("${mock.latency.min:50}")
    private int minLatencyMs;

    @Value("${mock.latency.max:150}")
    private int maxLatencyMs;

    private final Random random = new Random();

    @GetMapping("/inventory/{productId}")
    public Mono<ExternalResponses.InventoryResponse> getInventory(@PathVariable Long productId) {
        return Mono.just(ExternalResponses.InventoryResponse.builder()
                        .productId(productId)
                        .stockCount(random.nextInt(1000))
                        .warehouseLocation("Warehouse-" + (char)('A' + random.nextInt(5)))
                        .build())
                .delayElement(randomDelay());
    }

    @GetMapping("/pricing/{productId}")
    public Mono<ExternalResponses.PricingResponse> getPricing(@PathVariable Long productId) {
        return Mono.just(ExternalResponses.PricingResponse.builder()
                        .productId(productId)
                        .currentPrice(BigDecimal.valueOf(10 + random.nextDouble() * 990)
                                .setScale(2, BigDecimal.ROUND_HALF_UP))
                        .discountPercent(BigDecimal.valueOf(random.nextDouble() * 30)
                                .setScale(1, BigDecimal.ROUND_HALF_UP))
                        .build())
                .delayElement(randomDelay());
    }

    @GetMapping("/reviews/{productId}")
    public Mono<ExternalResponses.ReviewsResponse> getReviews(@PathVariable Long productId) {
        return Mono.just(ExternalResponses.ReviewsResponse.builder()
                        .productId(productId)
                        .averageRating(1 + random.nextDouble() * 4)
                        .reviewCount(random.nextInt(5000))
                        .build())
                .delayElement(randomDelay());
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }

    @PostMapping("/config/latency")
    public Mono<String> setLatency(@RequestParam int min, @RequestParam int max) {
        this.minLatencyMs = min;
        this.maxLatencyMs = max;
        log.info("Latency configured: {}ms - {}ms", min, max);
        return Mono.just("Latency set to " + min + "ms - " + max + "ms");
    }

    private Duration randomDelay() {
        int delay = minLatencyMs + random.nextInt(maxLatencyMs - minLatencyMs + 1);
        return Duration.ofMillis(delay);
    }
}
