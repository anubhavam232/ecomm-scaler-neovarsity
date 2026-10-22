package com.ecommerce.notification.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionClassesTest {

    // ========== NotificationServiceException Tests ==========

    @Test
    void testNotificationServiceException_WithMessage() {
        // Arrange
        String errorMessage = "Notification service error occurred";

        // Act
        NotificationServiceException exception = new NotificationServiceException(errorMessage);

        // Assert
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testNotificationServiceException_WithMessageAndCause() {
        // Arrange
        String errorMessage = "Notification service error occurred";
        Throwable cause = new RuntimeException("Root cause");

        // Act
        NotificationServiceException exception = new NotificationServiceException(errorMessage, cause);

        // Assert
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("Root cause", exception.getCause().getMessage());
    }

    @Test
    void testNotificationServiceException_IsRuntimeException() {
        // Arrange & Act
        NotificationServiceException exception = new NotificationServiceException("Test");

        // Assert
        assertTrue(exception instanceof RuntimeException);
    }

    // ========== NotificationSendException Tests ==========

    @Test
    void testNotificationSendException_WithMessage() {
        // Arrange
        String errorMessage = "Failed to send notification";

        // Act
        NotificationSendException exception = new NotificationSendException(errorMessage);

        // Assert
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testNotificationSendException_WithMessageAndCause() {
        // Arrange
        String errorMessage = "Failed to send notification";
        Throwable cause = new IllegalStateException("SES client error");

        // Act
        NotificationSendException exception = new NotificationSendException(errorMessage, cause);

        // Assert
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("SES client error", exception.getCause().getMessage());
    }

    @Test
    void testNotificationSendException_ExtendsNotificationServiceException() {
        // Arrange & Act
        NotificationSendException exception = new NotificationSendException("Test");

        // Assert
        assertTrue(exception instanceof NotificationServiceException);
        assertTrue(exception instanceof RuntimeException);
    }

    // ========== InvalidNotificationException Tests ==========

    @Test
    void testInvalidNotificationException_WithMessage() {
        // Arrange
        String errorMessage = "Invalid email format";

        // Act
        InvalidNotificationException exception = new InvalidNotificationException(errorMessage);

        // Assert
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testInvalidNotificationException_WithMessageAndCause() {
        // Arrange
        String errorMessage = "Invalid notification data";
        Throwable cause = new IllegalArgumentException("Missing required field");

        // Act
        InvalidNotificationException exception = new InvalidNotificationException(errorMessage, cause);

        // Assert
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("Missing required field", exception.getCause().getMessage());
    }

    @Test
    void testInvalidNotificationException_ExtendsNotificationServiceException() {
        // Arrange & Act
        InvalidNotificationException exception = new InvalidNotificationException("Test");

        // Assert
        assertTrue(exception instanceof NotificationServiceException);
        assertTrue(exception instanceof RuntimeException);
    }

    // ========== NotificationNotFoundException Tests ==========

    @Test
    void testNotificationNotFoundException_WithMessage() {
        // Arrange
        String errorMessage = "Notification not found with id: 123";

        // Act
        NotificationNotFoundException exception = new NotificationNotFoundException(errorMessage);

        // Assert
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testNotificationNotFoundException_WithMessageAndCause() {
        // Arrange
        String errorMessage = "Notification not found";
        Throwable cause = new IllegalStateException("Database connection error");

        // Act
        NotificationNotFoundException exception = new NotificationNotFoundException(errorMessage, cause);

        // Assert
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("Database connection error", exception.getCause().getMessage());
    }

    @Test
    void testNotificationNotFoundException_ExtendsNotificationServiceException() {
        // Arrange & Act
        NotificationNotFoundException exception = new NotificationNotFoundException("Test");

        // Assert
        assertTrue(exception instanceof NotificationServiceException);
        assertTrue(exception instanceof RuntimeException);
    }

    // ========== Exception Hierarchy Tests ==========

    @Test
    void testExceptionHierarchy() {
        // Verify the exception hierarchy is correct
        NotificationSendException sendException = new NotificationSendException("Send error");
        InvalidNotificationException invalidException = new InvalidNotificationException("Invalid error");
        NotificationNotFoundException notFoundException = new NotificationNotFoundException("Not found error");

        // All should be instances of NotificationServiceException
        assertTrue(sendException instanceof NotificationServiceException);
        assertTrue(invalidException instanceof NotificationServiceException);
        assertTrue(notFoundException instanceof NotificationServiceException);

        // All should be instances of RuntimeException
        assertTrue(sendException instanceof RuntimeException);
        assertTrue(invalidException instanceof RuntimeException);
        assertTrue(notFoundException instanceof RuntimeException);
    }

    @Test
    void testExceptionMessages_NotNull() {
        // Ensure exception messages are properly set
        NotificationSendException ex1 = new NotificationSendException("Message 1");
        InvalidNotificationException ex2 = new InvalidNotificationException("Message 2");
        NotificationNotFoundException ex3 = new NotificationNotFoundException("Message 3");

        assertNotNull(ex1.getMessage());
        assertNotNull(ex2.getMessage());
        assertNotNull(ex3.getMessage());
    }

    @Test
    void testExceptionCauseChaining() {
        // Test exception cause chaining works correctly
        RuntimeException rootCause = new RuntimeException("Root cause");
        IllegalArgumentException intermediateCause = new IllegalArgumentException("Intermediate", rootCause);
        NotificationSendException finalException = new NotificationSendException("Final error", intermediateCause);

        assertEquals("Final error", finalException.getMessage());
        assertEquals(intermediateCause, finalException.getCause());
        assertEquals(rootCause, finalException.getCause().getCause());
        assertEquals("Root cause", finalException.getCause().getCause().getMessage());
    }
}
