package com.ecommerce.notification.exception;

/**
 * Exception thrown when notification data is invalid.
 */
public class InvalidNotificationException extends NotificationServiceException {
    public InvalidNotificationException(String message) {
        super(message);
    }

    public InvalidNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
