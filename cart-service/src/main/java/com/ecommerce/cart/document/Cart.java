package com.ecommerce.cart.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * HLD Section 3.3: MongoDB document for flexible cart structures, cached in Redis.
 */
@Document(collection = "carts")
public class Cart implements Serializable {
    @Id
    private String id;
    private Long userId;
    private List<CartItem> items = new ArrayList<>();
    private int totalQuantity;
    private double subtotal;
    private double estimatedTax;
    private double grandTotal;
    private String cacheSource; // REDIS_CACHE_HIT or MONGODB_PRIMARY
    private String updatedAt;

    public Cart() {
        this.updatedAt = Instant.now().toString();
    }

    public Cart(Long userId) {
        this.id = "cart_user_" + userId;
        this.userId = userId;
        this.updatedAt = Instant.now().toString();
        recalculateTotals();
    }

    public void recalculateTotals() {
        this.totalQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
        this.subtotal = Math.round(items.stream().mapToDouble(CartItem::getLineTotal).sum() * 100.0) / 100.0;
        this.estimatedTax = Math.round(this.subtotal * 0.08 * 100.0) / 100.0;
        this.grandTotal = Math.round((this.subtotal + this.estimatedTax) * 100.0) / 100.0;
        this.updatedAt = Instant.now().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public double getEstimatedTax() { return estimatedTax; }
    public void setEstimatedTax(double estimatedTax) { this.estimatedTax = estimatedTax; }
    public double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }
    public String getCacheSource() { return cacheSource; }
    public void setCacheSource(String cacheSource) { this.cacheSource = cacheSource; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
