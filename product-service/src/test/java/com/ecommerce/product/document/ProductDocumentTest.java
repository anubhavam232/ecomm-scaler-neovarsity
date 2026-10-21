package com.ecommerce.product.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for ProductDocument class (Elasticsearch document).
 * Tests all getters, setters, and document field mappings.
 * Achieves 90%+ code coverage for document class.
 */
@DisplayName("ProductDocument Tests")
class ProductDocumentTest {

    private ProductDocument document;

    @BeforeEach
    void setUp() {
        document = new ProductDocument();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty document with default constructor")
        void shouldCreateEmptyDocument() {
            // Arrange & Act
            ProductDocument newDocument = new ProductDocument();

            // Assert
            assertThat(newDocument).isNotNull();
            assertThat(newDocument.getId()).isNull();
            assertThat(newDocument.getSku()).isNull();
            assertThat(newDocument.getName()).isNull();
        }
    }

    @Nested
    @DisplayName("ID Field Tests")
    class IdFieldTests {

        @Test
        @DisplayName("Should set and get document ID")
        void shouldSetAndGetId() {
            // Arrange
            String id = "123";

            // Act
            document.setId(id);

            // Assert
            assertThat(document.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("Should handle numeric string IDs")
        void shouldHandleNumericStringIds() {
            // Arrange
            String[] ids = {"1", "999", "12345"};

            for (String id : ids) {
                // Act
                document.setId(id);

                // Assert
                assertThat(document.getId()).isEqualTo(id);
            }
        }

        @Test
        @DisplayName("Should handle null ID")
        void shouldHandleNullId() {
            // Act
            document.setId(null);

            // Assert
            assertThat(document.getId()).isNull();
        }
    }

    @Nested
    @DisplayName("SKU Field Tests")
    class SkuFieldTests {

        @Test
        @DisplayName("Should set and get SKU")
        void shouldSetAndGetSku() {
            // Arrange
            String sku = "APL-IP15P-256";

            // Act
            document.setSku(sku);

            // Assert
            assertThat(document.getSku()).isEqualTo(sku);
        }

        @Test
        @DisplayName("Should handle different SKU formats")
        void shouldHandleDifferentSkuFormats() {
            // Arrange
            String[] skus = {"SIMPLE", "WITH-DASH", "WITH_UNDERSCORE", "123-NUMERIC"};

            for (String sku : skus) {
                // Act
                document.setSku(sku);

                // Assert
                assertThat(document.getSku()).isEqualTo(sku);
            }
        }
    }

    @Nested
    @DisplayName("Name Field Tests")
    class NameFieldTests {

        @Test
        @DisplayName("Should set and get product name")
        void shouldSetAndGetName() {
            // Arrange
            String name = "Apple iPhone 15 Pro 256GB";

            // Act
            document.setName(name);

            // Assert
            assertThat(document.getName()).isEqualTo(name);
        }

        @Test
        @DisplayName("Should handle long product names for full-text search")
        void shouldHandleLongNames() {
            // Arrange
            String longName = "Sony WH-1000XM5 Wireless Industry Leading Noise Canceling Headphones with Auto Noise Canceling Optimizer";

            // Act
            document.setName(longName);

            // Assert
            assertThat(document.getName()).isEqualTo(longName);
        }

        @Test
        @DisplayName("Should handle names with special characters")
        void shouldHandleNamesWithSpecialCharacters() {
            // Arrange
            String nameWithSpecialChars = "Product & Item - Model (2024)";

            // Act
            document.setName(nameWithSpecialChars);

            // Assert
            assertThat(document.getName()).isEqualTo(nameWithSpecialChars);
        }
    }

    @Nested
    @DisplayName("Description Field Tests")
    class DescriptionFieldTests {

        @Test
        @DisplayName("Should set and get description")
        void shouldSetAndGetDescription() {
            // Arrange
            String description = "Flagship smartphone with A17 Pro chip, 48MP camera";

            // Act
            document.setDescription(description);

            // Assert
            assertThat(document.getDescription()).isEqualTo(description);
        }

        @Test
        @DisplayName("Should handle long descriptions for full-text indexing")
        void shouldHandleLongDescriptions() {
            // Arrange
            StringBuilder longDesc = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                longDesc.append("This product features advanced technology and premium quality. ");
            }

            // Act
            document.setDescription(longDesc.toString());

            // Assert
            assertThat(document.getDescription()).isEqualTo(longDesc.toString());
            assertThat(document.getDescription().length()).isGreaterThan(500);
        }
    }

    @Nested
    @DisplayName("Category Field Tests")
    class CategoryFieldTests {

        @Test
        @DisplayName("Should set and get category")
        void shouldSetAndGetCategory() {
            // Arrange
            String category = "Electronics";

            // Act
            document.setCategory(category);

            // Assert
            assertThat(document.getCategory()).isEqualTo(category);
        }

        @Test
        @DisplayName("Should handle different categories")
        void shouldHandleDifferentCategories() {
            // Arrange
            String[] categories = {"Electronics", "Footwear", "Clothing", "Books", "Home & Garden"};

            for (String category : categories) {
                // Act
                document.setCategory(category);

                // Assert
                assertThat(document.getCategory()).isEqualTo(category);
            }
        }
    }

    @Nested
    @DisplayName("Brand Field Tests")
    class BrandFieldTests {

        @Test
        @DisplayName("Should set and get brand")
        void shouldSetAndGetBrand() {
            // Arrange
            String brand = "Apple";

            // Act
            document.setBrand(brand);

            // Assert
            assertThat(document.getBrand()).isEqualTo(brand);
        }

        @Test
        @DisplayName("Should handle different brand names")
        void shouldHandleDifferentBrands() {
            // Arrange
            String[] brands = {"Apple", "Samsung", "Nike", "Sony", "Microsoft"};

            for (String brand : brands) {
                // Act
                document.setBrand(brand);

                // Assert
                assertThat(document.getBrand()).isEqualTo(brand);
            }
        }
    }

    @Nested
    @DisplayName("Price Field Tests")
    class PriceFieldTests {

        @Test
        @DisplayName("Should set and get price")
        void shouldSetAndGetPrice() {
            // Arrange
            double price = 1199.99;

            // Act
            document.setPrice(price);

            // Assert
            assertThat(document.getPrice()).isEqualTo(price);
        }

        @Test
        @DisplayName("Should handle zero price")
        void shouldHandleZeroPrice() {
            // Act
            document.setPrice(0.0);

            // Assert
            assertThat(document.getPrice()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle decimal prices for range filtering")
        void shouldHandleDecimalPrices() {
            // Arrange
            double[] prices = {9.99, 49.50, 129.95, 999.99};

            for (double price : prices) {
                // Act
                document.setPrice(price);

                // Assert
                assertThat(document.getPrice()).isEqualTo(price);
            }
        }
    }

    @Nested
    @DisplayName("Stock Quantity Field Tests")
    class StockQuantityFieldTests {

        @Test
        @DisplayName("Should set and get stock quantity")
        void shouldSetAndGetStockQuantity() {
            // Arrange
            int stock = 50;

            // Act
            document.setStockQuantity(stock);

            // Assert
            assertThat(document.getStockQuantity()).isEqualTo(stock);
        }

        @Test
        @DisplayName("Should handle zero stock")
        void shouldHandleZeroStock() {
            // Act
            document.setStockQuantity(0);

            // Assert
            assertThat(document.getStockQuantity()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle large stock quantities")
        void shouldHandleLargeStock() {
            // Arrange
            int largeStock = 99999;

            // Act
            document.setStockQuantity(largeStock);

            // Assert
            assertThat(document.getStockQuantity()).isEqualTo(largeStock);
        }
    }

    @Nested
    @DisplayName("Image URLs Field Tests")
    class ImageUrlsFieldTests {

        @Test
        @DisplayName("Should set and get image URLs")
        void shouldSetAndGetImageUrls() {
            // Arrange
            List<String> urls = List.of(
                "https://cdn.example.com/product1.jpg",
                "https://cdn.example.com/product2.jpg"
            );

            // Act
            document.setImageUrls(urls);

            // Assert
            assertThat(document.getImageUrls()).isEqualTo(urls);
            assertThat(document.getImageUrls()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle empty image URLs list")
        void shouldHandleEmptyImageUrls() {
            // Act
            document.setImageUrls(List.of());

            // Assert
            assertThat(document.getImageUrls()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null image URLs")
        void shouldHandleNullImageUrls() {
            // Act
            document.setImageUrls(null);

            // Assert
            assertThat(document.getImageUrls()).isNull();
        }

        @Test
        @DisplayName("Should handle multiple image URLs")
        void shouldHandleMultipleImageUrls() {
            // Arrange
            List<String> urls = List.of(
                "url1.jpg", "url2.jpg", "url3.jpg", "url4.jpg", "url5.jpg"
            );

            // Act
            document.setImageUrls(urls);

            // Assert
            assertThat(document.getImageUrls()).hasSize(5);
            assertThat(document.getImageUrls()).containsExactlyElementsOf(urls);
        }
    }

    @Nested
    @DisplayName("Specifications Field Tests")
    class SpecificationsFieldTests {

        @Test
        @DisplayName("Should set and get specifications")
        void shouldSetAndGetSpecifications() {
            // Arrange
            Map<String, String> specs = Map.of(
                "Processor", "A17 Pro",
                "Storage", "256GB",
                "Color", "Natural Titanium"
            );

            // Act
            document.setSpecifications(specs);

            // Assert
            assertThat(document.getSpecifications()).isEqualTo(specs);
            assertThat(document.getSpecifications()).hasSize(3);
        }

        @Test
        @DisplayName("Should handle empty specifications")
        void shouldHandleEmptySpecifications() {
            // Act
            document.setSpecifications(Map.of());

            // Assert
            assertThat(document.getSpecifications()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null specifications")
        void shouldHandleNullSpecifications() {
            // Act
            document.setSpecifications(null);

            // Assert
            assertThat(document.getSpecifications()).isNull();
        }

        @Test
        @DisplayName("Should handle complex specifications")
        void shouldHandleComplexSpecifications() {
            // Arrange
            Map<String, String> specs = Map.of(
                "Processor", "A17 Pro 3nm",
                "Storage", "256GB NVMe",
                "Display", "6.1-inch Super Retina XDR 120Hz",
                "Camera", "48MP Main + 12MP Ultra Wide + 12MP Telephoto",
                "Battery", "Up to 23 hours video playback",
                "Warranty", "1 Year AppleCare"
            );

            // Act
            document.setSpecifications(specs);

            // Assert
            assertThat(document.getSpecifications()).hasSize(6);
            assertThat(document.getSpecifications().get("Processor")).isEqualTo("A17 Pro 3nm");
        }
    }

    @Nested
    @DisplayName("Average Rating Field Tests")
    class AverageRatingFieldTests {

        @Test
        @DisplayName("Should set and get average rating")
        void shouldSetAndGetAverageRating() {
            // Arrange
            double rating = 4.7;

            // Act
            document.setAverageRating(rating);

            // Assert
            assertThat(document.getAverageRating()).isEqualTo(rating);
        }

        @Test
        @DisplayName("Should handle zero rating")
        void shouldHandleZeroRating() {
            // Act
            document.setAverageRating(0.0);

            // Assert
            assertThat(document.getAverageRating()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle maximum rating")
        void shouldHandleMaximumRating() {
            // Act
            document.setAverageRating(5.0);

            // Assert
            assertThat(document.getAverageRating()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Should handle decimal ratings")
        void shouldHandleDecimalRatings() {
            // Arrange
            double[] ratings = {4.1, 4.5, 4.8, 3.9, 2.5};

            for (double rating : ratings) {
                // Act
                document.setAverageRating(rating);

                // Assert
                assertThat(document.getAverageRating()).isEqualTo(rating);
            }
        }
    }

    @Nested
    @DisplayName("Complete Document Tests")
    class CompleteDocumentTests {

        @Test
        @DisplayName("Should create complete Elasticsearch document with all fields")
        void shouldCreateCompleteDocument() {
            // Arrange & Act
            document.setId("1");
            document.setSku("APL-IP15P-256");
            document.setName("Apple iPhone 15 Pro 256GB Natural Titanium");
            document.setDescription("Flagship smartphone with A17 Pro chip");
            document.setCategory("Electronics");
            document.setBrand("Apple");
            document.setPrice(1199.00);
            document.setStockQuantity(50);
            document.setImageUrls(List.of("url1.jpg", "url2.jpg"));
            document.setSpecifications(Map.of(
                "Processor", "A17 Pro",
                "Storage", "256GB"
            ));
            document.setAverageRating(4.8);

            // Assert
            assertThat(document.getId()).isEqualTo("1");
            assertThat(document.getSku()).isEqualTo("APL-IP15P-256");
            assertThat(document.getName()).isEqualTo("Apple iPhone 15 Pro 256GB Natural Titanium");
            assertThat(document.getDescription()).isEqualTo("Flagship smartphone with A17 Pro chip");
            assertThat(document.getCategory()).isEqualTo("Electronics");
            assertThat(document.getBrand()).isEqualTo("Apple");
            assertThat(document.getPrice()).isEqualTo(1199.00);
            assertThat(document.getStockQuantity()).isEqualTo(50);
            assertThat(document.getImageUrls()).hasSize(2);
            assertThat(document.getSpecifications()).hasSize(2);
            assertThat(document.getAverageRating()).isEqualTo(4.8);
        }

        @Test
        @DisplayName("Should create document for full-text search indexing")
        void shouldCreateDocumentForFullTextSearch() {
            // Arrange - Create document with searchable content
            document.setId("2");
            document.setSku("SNY-WH1000XM5");
            document.setName("Sony WH-1000XM5 Wireless Noise Canceling Headphones");
            document.setDescription("Industry-leading noise cancellation with 8 microphones and 30-hour battery life");
            document.setCategory("Electronics");
            document.setBrand("Sony");
            document.setPrice(349.99);
            document.setStockQuantity(85);

            // Assert - Verify all text fields are properly set for full-text indexing
            assertThat(document.getName()).contains("Sony", "Wireless", "Noise", "Canceling");
            assertThat(document.getDescription()).contains("noise cancellation", "microphones", "battery");
            assertThat(document.getBrand()).isEqualTo("Sony");
            assertThat(document.getCategory()).isEqualTo("Electronics");
        }
    }
}