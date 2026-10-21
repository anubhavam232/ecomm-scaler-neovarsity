package com.ecommerce.product.exception;

/**
 * Exception thrown when a request contains invalid data or parameters.
 * Maps to HTTP 400 Bad Request status.
 */
public class InvalidRequestException extends ProductServiceException {
    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
