package com.ecommerce.user.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends UserServiceException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}