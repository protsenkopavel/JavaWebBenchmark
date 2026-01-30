package net.protsenko.loommodule.client;

import lombok.extern.slf4j.Slf4j;
import net.protsenko.common.model.ExternalResponses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ExternalServiceClient {

    private final RestClient restClient;

    public ExternalServiceClient(
            @Value("${external.service.base-url:http://localhost:8090}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ExternalResponses.InventoryResponse getInventory(Long productId) {
        log.debug("Calling inventory service for product {} on {}",
                productId, Thread.currentThread());
        return restClient.get()
                .uri("/api/inventory/{productId}", productId)
                .retrieve()
                .body(ExternalResponses.InventoryResponse.class);
    }

    public ExternalResponses.PricingResponse getPricing(Long productId) {
        log.debug("Calling pricing service for product {} on {}",
                productId, Thread.currentThread());
        return restClient.get()
                .uri("/api/pricing/{productId}", productId)
                .retrieve()
                .body(ExternalResponses.PricingResponse.class);
    }

    public ExternalResponses.ReviewsResponse getReviews(Long productId) {
        log.debug("Calling reviews service for product {} on {}",
                productId, Thread.currentThread());
        return restClient.get()
                .uri("/api/reviews/{productId}", productId)
                .retrieve()
                .body(ExternalResponses.ReviewsResponse.class);
    }
}
