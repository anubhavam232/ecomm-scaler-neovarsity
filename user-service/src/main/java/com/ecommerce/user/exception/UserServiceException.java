package com.ecommerce.user.exception;

/**
 * Base exception for user service specific errors.
 */
public class UserServiceException extends RuntimeException {
    public UserServiceException(String message) {
        super(message);
    }

    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}