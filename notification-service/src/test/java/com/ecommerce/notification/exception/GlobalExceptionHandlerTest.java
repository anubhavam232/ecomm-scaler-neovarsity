package com.ecommerce.notification.exception;

import com.ecommerce.common.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/notifications");
    }

    // ========== NotificationSendException Tests ==========

    @Test
    void testHandleNotificationSendException() {
        // Arrange
        NotificationSendException exception = new NotificationSendException("Failed to send notification");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleNotificationSendException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ApiResponse<Object> body = response.getBody();
        assertFalse(body.isSuccess());
        assertTrue(body.getMessage().contains("Failed to send notification"));
        assertNotNull(body.getTimestamp());
        assertNull(body.getData());
    }

    @Test
    void testHandleNotificationSendException_NullMessage() {
        // Arrange
        NotificationSendException exception = new NotificationSendException(null);

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleNotificationSendException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void testHandleNotificationSendException_WithCause() {
        // Arrange
        RuntimeException cause = new RuntimeException("SES client error");
        NotificationSendException exception = new NotificationSendException("Failed to send", cause);

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleNotificationSendException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    // ========== InvalidNotificationException Tests ==========

    @Test
    void testHandleInvalidNotificationException() {
        // Arrange
        InvalidNotificationException exception = new InvalidNotificationException("Invalid email format");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleInvalidNotificationException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ApiResponse<Object> body = response.getBody();
        assertFalse(body.isSuccess());
        assertEquals("Invalid email format", body.getMessage());
        assertNotNull(body.getTimestamp());
        assertNull(body.getData());
    }

    @Test
    void testHandleInvalidNotificationException_EmptyMessage() {
        // Arrange
        InvalidNotificationException exception = new InvalidNotificationException("");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleInvalidNotificationException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("", response.getBody().getMessage());
    }

    @Test
    void testHandleInvalidNotificationException_UserIdValidation() {
        // Arrange
        InvalidNotificationException exception = new InvalidNotificationException(
                "User ID must be a positive number");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleInvalidNotificationException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User ID must be a positive number", response.getBody().getMessage());
    }

    // ========== NotificationNotFoundException Tests ==========

    @Test
    void testHandleNotificationNotFoundException() {
        // Arrange
        NotificationNotFoundException exception = new NotificationNotFoundException(
                "Notification not found with id: 123");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleNotificationNotFoundException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ApiResponse<Object> body = response.getBody();
        assertFalse(body.isSuccess());
        assertEquals("Notification not found with id: 123", body.getMessage());
        assertNotNull(body.getTimestamp());
        assertNull(body.getData());
    }

    @Test
    void testHandleNotificationNotFoundException_WithCause() {
        // Arrange
        IllegalStateException cause = new IllegalStateException("Database error");
        NotificationNotFoundException exception = new NotificationNotFoundException(
                "Notification not found", cause);

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleNotificationNotFoundException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    // ========== NotificationServiceException Tests ==========

    @Test
    void testHandleNotificationServiceException() {
        // Arrange
        NotificationServiceException exception = new NotificationServiceException(
                "General notification service error");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleNotificationServiceException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("General notification service error", response.getBody().getMessage());
    }

    // ========== MethodArgumentTypeMismatchException Tests ==========

    @Test
    void testHandleMethodArgumentTypeMismatch() {
        // Arrange
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception = 
                mock(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("userId");
        when(exception.getRequiredType()).thenReturn((Class) Long.class);
        when(exception.getMessage()).thenReturn("Type mismatch");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleMethodArgumentTypeMismatch(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("userId"));
        assertTrue(response.getBody().getMessage().contains("Long"));
    }

    // ========== IllegalArgumentException Tests ==========

    @Test
    void testHandleIllegalArgumentException() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleIllegalArgumentException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid argument provided", response.getBody().getMessage());
    }

    // ========== Generic Exception Tests ==========

    @Test
    void testHandleGlobalException() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error occurred");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleGlobalException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ApiResponse<Object> body = response.getBody();
        assertFalse(body.isSuccess());
        assertEquals("An unexpected error occurred. Please try again later.", body.getMessage());
        assertNotNull(body.getTimestamp());
        assertNull(body.getData());
    }

    @Test
    void testHandleGlobalException_NullPointerException() {
        // Arrange
        NullPointerException exception = new NullPointerException("Null value encountered");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleGlobalException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());
    }

    @Test
    void testHandleGlobalException_SQLException() {
        // Arrange
        Exception exception = new Exception("Database connection failed");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleGlobalException(
                exception, webRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }

    // ========== Response Structure Tests ==========

    @Test
    void testErrorResponseContainsAllRequiredFields() {
        // Arrange
        NotificationSendException exception = new NotificationSendException("Test error");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleNotificationSendException(
                exception, webRequest);

        // Assert
        ApiResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getMessage());
        assertNotNull(body.getTimestamp());
        assertFalse(body.isSuccess());
        assertNull(body.getData());
    }

    @Test
    void testTimestampIsNotNull() {
        // Arrange
        InvalidNotificationException exception = new InvalidNotificationException("Test error");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleInvalidNotificationException(
                exception, webRequest);

        // Assert
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testSuccessFieldIsFalseForAllErrors() {
        // Test multiple exception types
        NotificationSendException sendEx = new NotificationSendException("Send error");
        InvalidNotificationException invalidEx = new InvalidNotificationException("Invalid error");
        NotificationNotFoundException notFoundEx = new NotificationNotFoundException("Not found");
        Exception genericEx = new RuntimeException("Generic error");

        ResponseEntity<ApiResponse<Object>> r1 = globalExceptionHandler.handleNotificationSendException(sendEx, webRequest);
        ResponseEntity<ApiResponse<Object>> r2 = globalExceptionHandler.handleInvalidNotificationException(invalidEx, webRequest);
        ResponseEntity<ApiResponse<Object>> r3 = globalExceptionHandler.handleNotificationNotFoundException(notFoundEx, webRequest);
        ResponseEntity<ApiResponse<Object>> r4 = globalExceptionHandler.handleGlobalException(genericEx, webRequest);

        assertFalse(r1.getBody().isSuccess());
        assertFalse(r2.getBody().isSuccess());
        assertFalse(r3.getBody().isSuccess());
        assertFalse(r4.getBody().isSuccess());
    }

    @Test
    void testDataFieldIsNullForAllErrors() {
        // Arrange
        NotificationSendException exception = new NotificationSendException("Test error");

        // Act
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleNotificationSendException(
                exception, webRequest);

        // Assert
        assertNull(response.getBody().getData());
    }
}
