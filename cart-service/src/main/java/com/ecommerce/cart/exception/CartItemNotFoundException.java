package com.ecommerce.cart.exception;

/**
 * Exception thrown when a product/item is not found in the cart.
 * Results in HTTP 404 response.
 */
public class CartItemNotFoundException extends CartServiceException {
    
    public CartItemNotFoundException(String message) {
        super(message);
    }
    
    public CartItemNotFoundException(Long productId) {
        super("Product not in cart: " + productId);
    }
}
