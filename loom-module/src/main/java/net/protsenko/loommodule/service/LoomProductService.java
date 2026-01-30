package net.protsenko.loommodule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.common.model.ExternalResponses;
import net.protsenko.common.model.Product;
import net.protsenko.common.model.ProductAggregation;
import net.protsenko.common.service.ProductService;
import net.protsenko.loommodule.client.ExternalServiceClient;
import net.protsenko.loommodule.mapper.ProductMapper;
import net.protsenko.loommodule.repo.JpaProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoomProductService implements ProductService {

    private final JpaProductRepository repository;
    private final ProductMapper mapper;
    private final ExternalServiceClient externalClient;

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


    @Override
    public ProductAggregation getProductAggregation(Long productId) {
        log.debug("Aggregating data for product {} using StructuredTaskScope", productId);

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var inventoryTask = scope.fork(() -> externalClient.getInventory(productId));
            var pricingTask = scope.fork(() -> externalClient.getPricing(productId));
            var reviewsTask = scope.fork(() -> externalClient.getReviews(productId));

            scope.join();
            scope.throwIfFailed();


            ExternalResponses.InventoryResponse inventory = inventoryTask.get();
            ExternalResponses.PricingResponse pricing = pricingTask.get();
            ExternalResponses.ReviewsResponse reviews = reviewsTask.get();

            return ProductAggregation.builder()
                    .productId(productId)
                    .stockCount(inventory.getStockCount())
                    .warehouseLocation(inventory.getWarehouseLocation())
                    .currentPrice(pricing.getCurrentPrice())
                    .discountPercent(pricing.getDiscountPercent())
                    .averageRating(reviews.getAverageRating())
                    .reviewCount(reviews.getReviewCount())
                    .build();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to aggregate product data", e);
        }
    }

    @Override
    public List<ProductAggregation> getProductAggregations(List<Long> productIds) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var tasks = productIds.stream()
                    .map(id -> scope.fork(() -> getProductAggregation(id)))
                    .toList();

            scope.join();
            scope.throwIfFailed();

            return tasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to aggregate products", e);
        }
    }

    public ProductAggregation getProductAggregationSimple(Long productId) {
        log.debug("Aggregating data for product {} using simple virtual threads", productId);

        var inventoryThread = Thread.startVirtualThread(
                () -> externalClient.getInventory(productId));
        var pricingThread = Thread.startVirtualThread(
                () -> externalClient.getPricing(productId));
        var reviewsThread = Thread.startVirtualThread(
                () -> externalClient.getReviews(productId));

        try {
            inventoryThread.join();
            pricingThread.join();
            reviewsThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }

        throw new UnsupportedOperationException(
                "Use getProductAggregation() with StructuredTaskScope instead");
    }
}
