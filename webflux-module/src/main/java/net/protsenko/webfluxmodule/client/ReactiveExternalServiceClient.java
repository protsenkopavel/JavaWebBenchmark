package net.protsenko.webfluxmodule.client;

import lombok.extern.slf4j.Slf4j;
import net.protsenko.common.model.ExternalResponses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ReactiveExternalServiceClient {

    private final WebClient webClient;

    public ReactiveExternalServiceClient(
            @Value("${external.service.base-url:http://localhost:8090}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<ExternalResponses.InventoryResponse> getInventory(Long productId) {
        log.debug("Calling inventory service for product {}", productId);
        return webClient.get()
                .uri("/api/inventory/{productId}", productId)
                .retrieve()
                .bodyToMono(ExternalResponses.InventoryResponse.class);
    }

    public Mono<ExternalResponses.PricingResponse> getPricing(Long productId) {
        log.debug("Calling pricing service for product {}", productId);
        return webClient.get()
                .uri("/api/pricing/{productId}", productId)
                .retrieve()
                .bodyToMono(ExternalResponses.PricingResponse.class);
    }

    public Mono<ExternalResponses.ReviewsResponse> getReviews(Long productId) {
        log.debug("Calling reviews service for product {}", productId);
        return webClient.get()
                .uri("/api/reviews/{productId}", productId)
                .retrieve()
                .bodyToMono(ExternalResponses.ReviewsResponse.class);
    }
}
