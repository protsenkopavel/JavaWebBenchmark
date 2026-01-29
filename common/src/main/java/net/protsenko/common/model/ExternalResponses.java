package net.protsenko.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class ExternalResponses {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryResponse {
        private Long productId;
        private Integer stockCount;
        private String warehouseLocation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricingResponse {
        private Long productId;
        private BigDecimal currentPrice;
        private BigDecimal discountPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewsResponse {
        private Long productId;
        private Double averageRating;
        private Integer reviewCount;
    }
}