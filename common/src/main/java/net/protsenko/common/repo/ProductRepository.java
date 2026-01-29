package net.protsenko.common.repo;

import net.protsenko.common.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    List<Product> findAllByIds(List<Long> ids);

    void deleteById(Long id);

    void deleteAll();

    long count();
}