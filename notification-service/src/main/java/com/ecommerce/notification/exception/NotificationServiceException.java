package com.ecommerce.notification.exception;

/**
 * Base exception for all notification service related errors.
 */
public class NotificationServiceException extends RuntimeException {
    public NotificationServiceException(String message) {
        super(message);
    }

    public NotificationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
