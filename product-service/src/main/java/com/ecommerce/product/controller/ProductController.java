package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.product.dto.ProductDtos.CreateProductRequest;
import com.ecommerce.product.dto.ProductDtos.SearchResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> browseProducts(
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok("Products retrieved", productService.getAllProducts(category)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<Map<String, Long>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.ok("Product categories retrieved", productService.getCategoriesWithCounts()));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<Product>> getProductDetails(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(ApiResponse.ok("Product details retrieved", productService.getProductById(productId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Product created and indexed in Elasticsearch", productService.createProduct(request)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> searchProducts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Elasticsearch query executed",
                productService.searchProducts(query, category, minPrice, maxPrice)
        ));
    }
}
