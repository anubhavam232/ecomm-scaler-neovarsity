package com.ecommerce.product.dto;

import com.ecommerce.product.entity.Product;
import java.util.List;
import java.util.Map;

public class ProductDtos {
    public record CreateProductRequest(
            String sku,
            String name,
            String description,
            String category,
            String brand,
            double price,
            int stockQuantity,
            List<String> imageUrls,
            Map<String, String> specifications
    ) {}

    public record SearchResponse(
            String query,
            String searchEngine,
            boolean fuzzyTypoCorrectionApplied,
            int totalHits,
            List<Product> results
    ) {}
}
