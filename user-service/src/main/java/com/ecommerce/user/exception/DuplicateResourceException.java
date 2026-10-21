package com.ecommerce.user.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends UserServiceException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}