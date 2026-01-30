package net.protsenko.loommodule.controller;

import lombok.RequiredArgsConstructor;
import net.protsenko.common.model.Product;
import net.protsenko.common.model.ProductAggregation;
import net.protsenko.loommodule.service.LoomProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final LoomProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@RequestBody Product product) {
        return productService.saveProduct(product);
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}/aggregation")
    public ProductAggregation getAggregation(@PathVariable Long id) {
        return productService.getProductAggregation(id);
    }

    @PostMapping("/aggregations")
    public List<ProductAggregation> getAggregations(@RequestBody List<Long> ids) {
        return productService.getProductAggregations(ids);
    }
}