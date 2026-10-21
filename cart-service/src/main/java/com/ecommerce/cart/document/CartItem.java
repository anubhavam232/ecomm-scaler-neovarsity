package com.ecommerce.cart.document;

import java.io.Serializable;

public class CartItem implements Serializable {
    private Long productId;
    private String sku;
    private String productName;
    private double unitPrice;
    private int quantity;
    private double lineTotal;

    public CartItem() {}

    public CartItem(Long productId, String sku, String productName, double unitPrice, int quantity) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = Math.round(unitPrice * quantity * 100.0) / 100.0;
    }

    public void recalculate() {
        this.lineTotal = Math.round(this.unitPrice * this.quantity * 100.0) / 100.0;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; this.recalculate(); }
    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}
