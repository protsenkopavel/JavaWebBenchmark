package net.protsenko.syncmodule.mapper;

import net.protsenko.common.model.Product;
import net.protsenko.syncmodule.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDto(ProductEntity entity) {
        if (entity == null) return null;

        return Product.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ProductEntity toEntity(Product dto) {
        if (dto == null) return null;

        return ProductEntity.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .createdAt(dto.getCreatedAt())
                .build();
    }
}
