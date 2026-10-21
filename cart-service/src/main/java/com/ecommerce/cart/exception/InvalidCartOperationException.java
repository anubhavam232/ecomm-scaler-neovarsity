package com.ecommerce.cart.exception;

/**
 * Exception thrown when an invalid cart operation is attempted.
 * Results in HTTP 400 response.
 */
public class InvalidCartOperationException extends CartServiceException {
    
    public InvalidCartOperationException(String message) {
        super(message);
    }
    
    public InvalidCartOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
