package com.ecommerce.notification.exception;

/**
 * Exception thrown when a notification is not found.
 */
public class NotificationNotFoundException extends NotificationServiceException {
    public NotificationNotFoundException(String message) {
        super(message);
    }

    public NotificationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
