package com.ecommerce.product.exception;

/**
 * Exception thrown when a requested resource (product) is not found.
 * Maps to HTTP 404 Not Found status.
 */
public class ResourceNotFoundException extends ProductServiceException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
