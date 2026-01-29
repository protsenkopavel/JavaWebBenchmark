package net.protsenko.common.service;

import net.protsenko.common.model.Product;
import net.protsenko.common.model.ProductAggregation;

import java.util.List;

public interface ProductService {
    Product saveProduct(Product product);

    Product getProduct(Long id);

    List<Product> getAllProducts();

    ProductAggregation getProductAggregation(Long productId);

    List<ProductAggregation> getProductAggregations(List<Long> productIds);
}
