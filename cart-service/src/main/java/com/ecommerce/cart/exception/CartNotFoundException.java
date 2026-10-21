package com.ecommerce.cart.exception;

/**
 * Exception thrown when a cart is not found for a given user.
 * Results in HTTP 404 response.
 */
public class CartNotFoundException extends CartServiceException {
    
    public CartNotFoundException(String message) {
        super(message);
    }
    
    public CartNotFoundException(Long userId) {
        super("Cart not found for user: " + userId);
    }
}
