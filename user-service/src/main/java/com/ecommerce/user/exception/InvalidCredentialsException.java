package com.ecommerce.user.exception;

/**
 * Exception thrown when user credentials are invalid.
 */
public class InvalidCredentialsException extends UserServiceException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}