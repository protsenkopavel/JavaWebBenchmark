package net.protsenko.webfluxmodule.controller;

import lombok.RequiredArgsConstructor;
import net.protsenko.common.model.Product;
import net.protsenko.common.model.ProductAggregation;
import net.protsenko.webfluxmodule.service.ReactiveProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ReactiveProductController {

    private final ReactiveProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> createProduct(@RequestBody Product product) {
        return productService.saveProduct(product);
    }

    @GetMapping("/{id}")
    public Mono<Product> getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @GetMapping
    public Flux<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}/aggregation")
    public Mono<ProductAggregation> getAggregation(@PathVariable Long id) {
        return productService.getProductAggregation(id);
    }

    @PostMapping("/aggregations")
    public Flux<ProductAggregation> getAggregations(@RequestBody List<Long> ids) {
        return productService.getProductAggregations(ids);
    }
}