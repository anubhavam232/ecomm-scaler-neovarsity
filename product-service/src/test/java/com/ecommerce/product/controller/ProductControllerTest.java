package com.ecommerce.product.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.product.dto.ProductDtos.CreateProductRequest;
import com.ecommerce.product.dto.ProductDtos.SearchResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for ProductController.
 * Tests all REST endpoints with proper HTTP status codes and exception handling.
 * Uses MockMvc for integration testing of controller layer.
 */
@WebMvcTest(ProductController.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private Product testProduct;
    private static final Long TEST_PRODUCT_ID = 1L;
    private static final String TEST_SKU = "TEST-SKU-001";
    private static final String TEST_NAME = "Test Product";
    private static final String TEST_DESCRIPTION = "Test Description";
    private static final String TEST_CATEGORY = "Electronics";
    private static final String TEST_BRAND = "TestBrand";
    private static final double TEST_PRICE = 99.99;
    private static final int TEST_STOCK = 10;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(TEST_PRODUCT_ID);
        testProduct.setSku(TEST_SKU);
        testProduct.setName(TEST_NAME);
        testProduct.setDescription(TEST_DESCRIPTION);
        testProduct.setCategory(TEST_CATEGORY);
        testProduct.setBrand(TEST_BRAND);
        testProduct.setPrice(TEST_PRICE);
        testProduct.setStockQuantity(TEST_STOCK);
        testProduct.setImageUrls(List.of("https://example.com/image.jpg"));
        testProduct.setSpecifications(Map.of("Color", "Black"));
        testProduct.setActive(true);
    }

    @Nested
    @DisplayName("Browse Products Tests")
    class BrowseProductsTests {

        @Test
        @DisplayName("Should return all products successfully")
        void shouldReturnAllProductsSuccessfully() throws Exception {
            // Arrange
            List<Product> products = List.of(testProduct);
            when(productService.getAllProducts(null)).thenReturn(products);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Products retrieved"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(TEST_PRODUCT_ID))
                    .andExpect(jsonPath("$.data[0].name").value(TEST_NAME))
                    .andExpect(jsonPath("$.data[0].category").value(TEST_CATEGORY));

            verify(productService).getAllProducts(null);
        }

        @Test
        @DisplayName("Should return products filtered by category")
        void shouldReturnProductsFilteredByCategory() throws Exception {
            // Arrange
            List<Product> products = List.of(testProduct);
            when(productService.getAllProducts(TEST_CATEGORY)).thenReturn(products);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products")
                            .param("category", TEST_CATEGORY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].category").value(TEST_CATEGORY));

            verify(productService).getAllProducts(TEST_CATEGORY);
        }

        @Test
        @DisplayName("Should return empty list when no products exist")
        void shouldReturnEmptyListWhenNoProducts() throws Exception {
            // Arrange
            when(productService.getAllProducts(null)).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("List Categories Tests")
    class ListCategoriesTests {

        @Test
        @DisplayName("Should return categories with counts successfully")
        void shouldReturnCategoriesWithCountsSuccessfully() throws Exception {
            // Arrange
            Map<String, Long> categories = Map.of(
                    "Electronics", 5L,
                    "Footwear", 3L
            );
            when(productService.getCategoriesWithCounts()).thenReturn(categories);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product categories retrieved"))
                    .andExpect(jsonPath("$.data.Electronics").value(5))
                    .andExpect(jsonPath("$.data.Footwear").value(3));

            verify(productService).getCategoriesWithCounts();
        }

        @Test
        @DisplayName("Should return empty map when no categories")
        void shouldReturnEmptyMapWhenNoCategories() throws Exception {
            // Arrange
            when(productService.getCategoriesWithCounts()).thenReturn(Collections.emptyMap());

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    @Nested
    @DisplayName("Get Product Details Tests")
    class GetProductDetailsTests {

        @Test
        @DisplayName("Should return product details successfully")
        void shouldReturnProductDetailsSuccessfully() throws Exception {
            // Arrange
            when(productService.getProductById(TEST_PRODUCT_ID)).thenReturn(testProduct);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/{productId}", TEST_PRODUCT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product details retrieved"))
                    .andExpect(jsonPath("$.data.id").value(TEST_PRODUCT_ID))
                    .andExpect(jsonPath("$.data.sku").value(TEST_SKU))
                    .andExpect(jsonPath("$.data.name").value(TEST_NAME))
                    .andExpect(jsonPath("$.data.description").value(TEST_DESCRIPTION))
                    .andExpect(jsonPath("$.data.category").value(TEST_CATEGORY))
                    .andExpect(jsonPath("$.data.brand").value(TEST_BRAND))
                    .andExpect(jsonPath("$.data.price").value(TEST_PRICE))
                    .andExpect(jsonPath("$.data.stockQuantity").value(TEST_STOCK));

            verify(productService).getProductById(TEST_PRODUCT_ID);
        }

        @Test
        @DisplayName("Should return 404 when product not found")
        void shouldReturn404WhenProductNotFound() throws Exception {
            // Arrange
            when(productService.getProductById(999L))
                    .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/{productId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Product not found with id: 999"));

            verify(productService).getProductById(999L);
        }
    }

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void shouldCreateProductSuccessfully() throws Exception {
            // Arrange
            CreateProductRequest request = new CreateProductRequest(
                    TEST_SKU,
                    TEST_NAME,
                    TEST_DESCRIPTION,
                    TEST_CATEGORY,
                    TEST_BRAND,
                    TEST_PRICE,
                    TEST_STOCK,
                    List.of("https://example.com/image.jpg"),
                    Map.of("Color", "Black")
            );

            when(productService.createProduct(any(CreateProductRequest.class)))
                    .thenReturn(testProduct);

            // Act & Assert
            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Product created and indexed in Elasticsearch"))
                    .andExpect(jsonPath("$.data.id").value(TEST_PRODUCT_ID))
                    .andExpect(jsonPath("$.data.sku").value(TEST_SKU))
                    .andExpect(jsonPath("$.data.name").value(TEST_NAME));

            verify(productService).createProduct(any(CreateProductRequest.class));
        }

        @Test
        @DisplayName("Should create product with minimal fields")
        void shouldCreateProductWithMinimalFields() throws Exception {
            // Arrange
            CreateProductRequest request = new CreateProductRequest(
                    TEST_SKU,
                    TEST_NAME,
                    TEST_DESCRIPTION,
                    TEST_CATEGORY,
                    TEST_BRAND,
                    TEST_PRICE,
                    TEST_STOCK,
                    null,
                    null
            );

            when(productService.createProduct(any(CreateProductRequest.class)))
                    .thenReturn(testProduct);

            // Act & Assert
            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Search Products Tests")
    class SearchProductsTests {

        @Test
        @DisplayName("Should search products with keyword successfully")
        void shouldSearchProductsWithKeywordSuccessfully() throws Exception {
            // Arrange
            SearchResponse searchResponse = new SearchResponse(
                    "test",
                    "Elasticsearch (products_idx)",
                    false,
                    1,
                    List.of(testProduct)
            );
            when(productService.searchProducts("test", null, null, null))
                    .thenReturn(searchResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/search")
                            .param("q", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Elasticsearch query executed"))
                    .andExpect(jsonPath("$.data.query").value("test"))
                    .andExpect(jsonPath("$.data.totalHits").value(1))
                    .andExpect(jsonPath("$.data.results").isArray())
                    .andExpect(jsonPath("$.data.results[0].id").value(TEST_PRODUCT_ID));

            verify(productService).searchProducts("test", null, null, null);
        }

        @Test
        @DisplayName("Should search with category filter")
        void shouldSearchWithCategoryFilter() throws Exception {
            // Arrange
            SearchResponse searchResponse = new SearchResponse(
                    "",
                    "Elasticsearch (products_idx)",
                    false,
                    1,
                    List.of(testProduct)
            );
            when(productService.searchProducts(null, TEST_CATEGORY, null, null))
                    .thenReturn(searchResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/search")
                            .param("category", TEST_CATEGORY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalHits").value(1));

            verify(productService).searchProducts(null, TEST_CATEGORY, null, null);
        }

        @Test
        @DisplayName("Should search with price range filter")
        void shouldSearchWithPriceRangeFilter() throws Exception {
            // Arrange
            SearchResponse searchResponse = new SearchResponse(
                    "",
                    "Elasticsearch (products_idx)",
                    false,
                    1,
                    List.of(testProduct)
            );
            when(productService.searchProducts(null, null, 50.0, 150.0))
                    .thenReturn(searchResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/search")
                            .param("minPrice", "50.0")
                            .param("maxPrice", "150.0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalHits").value(1));

            verify(productService).searchProducts(null, null, 50.0, 150.0);
        }

        @Test
        @DisplayName("Should search with all filters combined")
        void shouldSearchWithAllFiltersCombined() throws Exception {
            // Arrange
            SearchResponse searchResponse = new SearchResponse(
                    "test",
                    "Elasticsearch (products_idx)",
                    false,
                    1,
                    List.of(testProduct)
            );
            when(productService.searchProducts("test", TEST_CATEGORY, 50.0, 150.0))
                    .thenReturn(searchResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/search")
                            .param("q", "test")
                            .param("category", TEST_CATEGORY)
                            .param("minPrice", "50.0")
                            .param("maxPrice", "150.0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.query").value("test"))
                    .andExpect(jsonPath("$.data.totalHits").value(1));

            verify(productService).searchProducts("test", TEST_CATEGORY, 50.0, 150.0);
        }

        @Test
        @DisplayName("Should return empty results when no matches found")
        void shouldReturnEmptyResultsWhenNoMatchesFound() throws Exception {
            // Arrange
            SearchResponse searchResponse = new SearchResponse(
                    "nonexistent",
                    "Elasticsearch (products_idx)",
                    false,
                    0,
                    Collections.emptyList()
            );
            when(productService.searchProducts("nonexistent", null, null, null))
                    .thenReturn(searchResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/search")
                            .param("q", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalHits").value(0))
                    .andExpect(jsonPath("$.data.results").isEmpty());
        }

        @Test
        @DisplayName("Should indicate fuzzy search was applied")
        void shouldIndicateFuzzySearchWasApplied() throws Exception {
            // Arrange
            SearchResponse searchResponse = new SearchResponse(
                    "tset",
                    "Elasticsearch (products_idx)",
                    true,
                    1,
                    List.of(testProduct)
            );
            when(productService.searchProducts("tset", null, null, null))
                    .thenReturn(searchResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/products/search")
                            .param("q", "tset"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.fuzzyTypoCorrectionApplied").value(true));
        }
    }
}
