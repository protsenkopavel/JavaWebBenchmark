package net.protsenko.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAggregation {
    private Long productId;

    private Integer stockCount;
    private String warehouseLocation;

    private BigDecimal currentPrice;
    private BigDecimal discountPercent;

    private Double averageRating;
    private Integer reviewCount;
}