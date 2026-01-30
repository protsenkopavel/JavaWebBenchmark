package net.protsenko.syncmodule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.common.model.ExternalResponses;
import net.protsenko.common.model.Product;
import net.protsenko.common.model.ProductAggregation;
import net.protsenko.common.service.ProductService;
import net.protsenko.syncmodule.client.ExternalServiceClient;
import net.protsenko.syncmodule.mapper.ProductMapper;
import net.protsenko.syncmodule.repo.JpaProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncProductService implements ProductService {

    private final JpaProductRepository repository;
    private final ProductMapper mapper;
    private final ExternalServiceClient externalClient;

    // Thread pool for parallel HTTP calls
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
    );

    @Override
    @Transactional
    public Product saveProduct(Product product) {
        var entity = mapper.toEntity(product);
        var saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Main benchmark method: parallel HTTP calls using CompletableFuture.allOf()
     */
    @Override
    public ProductAggregation getProductAggregation(Long productId) {
        log.debug("Aggregating data for product {} using CompletableFuture.allOf()", productId);

        CompletableFuture<ExternalResponses.InventoryResponse> inventoryFuture = CompletableFuture
                .supplyAsync(() -> externalClient.getInventory(productId), executor);

        CompletableFuture<ExternalResponses.PricingResponse> pricingFuture = CompletableFuture
                .supplyAsync(() -> externalClient.getPricing(productId), executor);

        CompletableFuture<ExternalResponses.ReviewsResponse> reviewsFuture = CompletableFuture
                .supplyAsync(() -> externalClient.getReviews(productId), executor);

        CompletableFuture.allOf(inventoryFuture, pricingFuture, reviewsFuture).join();

        ExternalResponses.InventoryResponse inventory = inventoryFuture.join();
        ExternalResponses.PricingResponse pricing = pricingFuture.join();
        ExternalResponses.ReviewsResponse reviews = reviewsFuture.join();

        return ProductAggregation.builder()
                .productId(productId)
                .stockCount(inventory.getStockCount())
                .warehouseLocation(inventory.getWarehouseLocation())
                .currentPrice(pricing.getCurrentPrice())
                .discountPercent(pricing.getDiscountPercent())
                .averageRating(reviews.getAverageRating())
                .reviewCount(reviews.getReviewCount())
                .build();
    }

    @Override
    public List<ProductAggregation> getProductAggregations(List<Long> productIds) {
        List<CompletableFuture<ProductAggregation>> futures = productIds.stream()
                .map(id -> CompletableFuture.supplyAsync(
                        () -> getProductAggregation(id), executor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
