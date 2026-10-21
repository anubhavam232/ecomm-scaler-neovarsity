package com.ecommerce.cart.dto;

public class CartDtos {
    public record AddCartItemRequest(
            Long productId,
            String sku,
            String productName,
            double unitPrice,
            int quantity
    ) {}

    public record UpdateQuantityRequest(
            int quantity
    ) {}
}
