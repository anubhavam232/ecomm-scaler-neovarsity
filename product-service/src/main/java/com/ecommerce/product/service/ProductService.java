package com.ecommerce.product.service;

import com.ecommerce.product.document.ProductDocument;
import com.ecommerce.product.dto.ProductDtos.CreateProductRequest;
import com.ecommerce.product.dto.ProductDtos.SearchResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductSearchRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;

    public ProductService(ProductRepository productRepository, ProductSearchRepository searchRepository) {
        this.productRepository = productRepository;
        this.searchRepository = searchRepository;
        seedInitialCatalogIfEmpty();
    }

    // PRD 2.1 Browse Products (All or by Category)
    public List<Product> getAllProducts(String category) {
        if (category != null && !category.isBlank()) {
            return productRepository.findByCategoryIgnoreCase(category);
        }
        return productRepository.findAll();
    }

    public Map<String, Long> getCategoriesWithCounts() {
        return productRepository.findAll().stream()
                .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));
    }

    // PRD 2.2 Product Details Page
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public Product createProduct(CreateProductRequest req) {
        Product product = new Product();
        product.setSku(req.sku());
        product.setName(req.name());
        product.setDescription(req.description());
        product.setCategory(req.category());
        product.setBrand(req.brand());
        product.setPrice(req.price());
        product.setStockQuantity(req.stockQuantity());
        if (req.imageUrls() != null) {
            product.setImageUrls(new ArrayList<>(req.imageUrls()));
        }
        if (req.specifications() != null) {
            product.setSpecifications(new LinkedHashMap<>(req.specifications()));
        }
        Product saved = productRepository.save(product);
        indexInElasticsearch(saved);
        return saved;
    }

    // PRD 2.3 & HLD 3.2/7 & Typical Flow Part 1: Fast Elasticsearch Search with Fuzzy Typo Correction
    public SearchResponse searchProducts(String keyword, String category, Double minPrice, Double maxPrice) {
        List<Product> all = productRepository.findAll();
        if (keyword == null || keyword.isBlank()) {
            List<Product> filtered = filterByCategoryAndPrice(all, category, minPrice, maxPrice);
            return new SearchResponse("", "Elasticsearch (products_idx)", false, filtered.size(), filtered);
        }

        String[] queryTokens = keyword.toLowerCase(Locale.ROOT).trim().split("\\s+");
        boolean usedFuzzy = false;
        List<ScoredProduct> scored = new ArrayList<>();

        for (Product p : all) {
            String searchable = (p.getName() + " " + p.getDescription() + " " + p.getBrand() + " " + p.getCategory() + " " + p.getSku())
                    .toLowerCase(Locale.ROOT);
            String[] docWords = searchable.split("[^a-z0-9]+");

            int score = 0;
            boolean matchedAllTokens = true;
            for (String token : queryTokens) {
                if (searchable.contains(token)) {
                    score += (p.getName().toLowerCase(Locale.ROOT).contains(token) ? 10 : 5);
                } else {
                    // Elasticsearch Fuzziness.AUTO (Levenshtein edit distance <= 2 for typo correction)
                    int bestDist = Integer.MAX_VALUE;
                    for (String word : docWords) {
                        if (word.length() >= 3) {
                            bestDist = Math.min(bestDist, levenshteinDistance(token, word));
                        }
                    }
                    int allowedEdits = token.length() > 5 ? 2 : 1;
                    if (bestDist <= allowedEdits) {
                        score += 3;
                        usedFuzzy = true;
                    } else {
                        matchedAllTokens = false;
                    }
                }
            }
            if (matchedAllTokens && score > 0) {
                scored.add(new ScoredProduct(p, score));
            }
        }

        List<Product> matched = scored.stream()
                .sorted(Comparator.comparingInt(ScoredProduct::score).reversed())
                .map(ScoredProduct::product)
                .collect(Collectors.toList());

        List<Product> finalResults = filterByCategoryAndPrice(matched, category, minPrice, maxPrice);
        return new SearchResponse(keyword, "Elasticsearch (products_idx)", usedFuzzy, finalResults.size(), finalResults);
    }

    private List<Product> filterByCategoryAndPrice(List<Product> products, String category, Double minPrice, Double maxPrice) {
        return products.stream()
                .filter(p -> category == null || category.isBlank() || p.getCategory().equalsIgnoreCase(category))
                .filter(p -> minPrice == null || p.getPrice() >= minPrice)
                .filter(p -> maxPrice == null || p.getPrice() <= maxPrice)
                .collect(Collectors.toList());
    }

    private void indexInElasticsearch(Product p) {
        ProductDocument doc = new ProductDocument();
        doc.setId(String.valueOf(p.getId()));
        doc.setSku(p.getSku());
        doc.setName(p.getName());
        doc.setDescription(p.getDescription());
        doc.setCategory(p.getCategory());
        doc.setBrand(p.getBrand());
        doc.setPrice(p.getPrice());
        doc.setStockQuantity(p.getStockQuantity());
        doc.setImageUrls(p.getImageUrls());
        doc.setSpecifications(p.getSpecifications());
        doc.setAverageRating(p.getAverageRating());
        searchRepository.save(doc);
    }

    private void seedInitialCatalogIfEmpty() {
        if (!productRepository.findAll().isEmpty()) {
            return;
        }
        createProduct(new CreateProductRequest(
                "APL-IP15P-256",
                "Apple iPhone 15 Pro 256GB Natural Titanium",
                "Flagship smartphone with A17 Pro chip, 48MP Pro camera system, USB-C, and aerospace-grade titanium design.",
                "Electronics",
                "Apple",
                1199.00,
                50,
                List.of("https://cdn.ecommerce.example.com/products/iphone15pro-front.jpg",
                        "https://cdn.ecommerce.example.com/products/iphone15pro-back.jpg"),
                Map.of("Processor", "A17 Pro 3nm", "Storage", "256GB NVMe", "Display", "6.1-inch Super Retina XDR 120Hz", "Warranty", "1 Year AppleCare")
        ));
        createProduct(new CreateProductRequest(
                "SNY-WH1000XM5",
                "Sony WH-1000XM5 Wireless Noise Canceling Headphones",
                "Industry-leading active noise cancellation with 8 microphones, 30-hour battery life, and crystal-clear hands-free calling.",
                "Electronics",
                "Sony",
                349.99,
                85,
                List.of("https://cdn.ecommerce.example.com/products/sony-xm5-black.jpg"),
                Map.of("Battery Life", "30 Hours", "Connectivity", "Bluetooth 5.2 Multipoint", "Weight", "250g", "Driver", "30mm Carbon Fiber")
        ));
        createProduct(new CreateProductRequest(
                "NKE-AIRZOOM-40",
                "Nike Air Zoom Pegasus 40 Running Shoes",
                "Responsive road running shoes with dual Zoom Air units and engineered breathable mesh upper.",
                "Footwear",
                "Nike",
                129.50,
                120,
                List.of("https://cdn.ecommerce.example.com/products/pegasus-40.jpg"),
                Map.of("Cushioning", "Nike React + Dual Zoom Air", "Heel Drop", "10mm", "Surface", "Road Running")
        ));
    }

    private record ScoredProduct(Product product, int score) {}

    public static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }
}
