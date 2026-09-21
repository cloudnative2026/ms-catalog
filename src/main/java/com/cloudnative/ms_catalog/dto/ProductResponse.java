package com.cloudnative.ms_catalog.dto;

import com.cloudnative.ms_catalog.entity.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description,
        BigDecimal price, Integer stock, String imageUrl, Boolean active) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
            product.getPrice(), product.getStock(), product.getImageUrl(), product.getActive());
    }
}
