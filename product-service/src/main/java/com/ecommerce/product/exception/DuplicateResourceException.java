package com.ecommerce.product.exception;

/**
 * Exception thrown when attempting to create a resource that already exists (e.g., duplicate SKU).
 * Maps to HTTP 409 Conflict status.
 */
public class DuplicateResourceException extends ProductServiceException {
    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
