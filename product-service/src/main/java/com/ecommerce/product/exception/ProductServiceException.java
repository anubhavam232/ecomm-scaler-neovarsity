package com.ecommerce.product.exception;

/**
 * Base exception class for Product Service.
 * All custom exceptions in the product service should extend this class.
 */
public class ProductServiceException extends RuntimeException {
    public ProductServiceException(String message) {
        super(message);
    }

    public ProductServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
