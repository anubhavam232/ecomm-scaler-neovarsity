package com.ecommerce.cart.exception;

/**
 * Base exception class for all cart-service specific exceptions.
 * Extends RuntimeException to avoid forced exception handling.
 */
public class CartServiceException extends RuntimeException {
    
    public CartServiceException(String message) {
        super(message);
    }
    
    public CartServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
