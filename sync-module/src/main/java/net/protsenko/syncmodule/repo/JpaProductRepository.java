package net.protsenko.syncmodule.repo;

import net.protsenko.syncmodule.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findAllByIdIn(List<Long> ids);
}