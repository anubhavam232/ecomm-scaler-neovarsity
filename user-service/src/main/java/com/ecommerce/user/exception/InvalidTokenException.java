package com.ecommerce.user.exception;

/**
 * Exception thrown when a token (reset token or session token) is invalid, expired, or already used.
 */
public class InvalidTokenException extends UserServiceException {
    public InvalidTokenException(String message) {
        super(message);
    }
}