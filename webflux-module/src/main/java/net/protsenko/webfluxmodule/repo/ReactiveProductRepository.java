package net.protsenko.webfluxmodule.repo;

import net.protsenko.webfluxmodule.entity.ProductEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.Collection;

@Repository
public interface ReactiveProductRepository extends R2dbcRepository<ProductEntity, Long> {

    Flux<ProductEntity> findAllByIdIn(Collection<Long> ids);
}
