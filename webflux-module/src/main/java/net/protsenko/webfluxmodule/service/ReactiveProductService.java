package net.protsenko.webfluxmodule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.protsenko.common.model.Product;
import net.protsenko.common.model.ProductAggregation;
import net.protsenko.webfluxmodule.client.ReactiveExternalServiceClient;
import net.protsenko.webfluxmodule.mapper.ProductMapper;
import net.protsenko.webfluxmodule.repo.ReactiveProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactiveProductService {

    private final ReactiveProductRepository repository;
    private final ProductMapper mapper;
    private final ReactiveExternalServiceClient externalClient;

    @Transactional
    public Mono<Product> saveProduct(Product product) {
        var entity = mapper.toEntity(product);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return repository.save(entity)
                .map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Mono<Product> getProduct(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Product not found: " + id)));
    }

    @Transactional(readOnly = true)
    public Flux<Product> getAllProducts() {
        return repository.findAll()
                .map(mapper::toDto);
    }

    public Mono<ProductAggregation> getProductAggregation(Long productId) {
        log.debug("Aggregating data for product {} using flatMap chain", productId);

        return externalClient.getInventory(productId)
                .flatMap(inventory ->
                        Mono.zip(
                                Mono.just(inventory),
                                externalClient.getPricing(productId),
                                externalClient.getReviews(productId)
                        )
                )
                .map(tuple -> ProductAggregation.builder()
                        .productId(productId)
                        .stockCount(tuple.getT1().getStockCount())
                        .warehouseLocation(tuple.getT1().getWarehouseLocation())
                        .currentPrice(tuple.getT2().getCurrentPrice())
                        .discountPercent(tuple.getT2().getDiscountPercent())
                        .averageRating(tuple.getT3().getAverageRating())
                        .reviewCount(tuple.getT3().getReviewCount())
                        .build());
    }

    public Flux<ProductAggregation> getProductAggregations(List<Long> productIds) {
        return Flux.fromIterable(productIds)
                .flatMap(this::getProductAggregation, 100);
    }
}