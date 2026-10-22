package com.ecommerce.notification.exception;

import com.ecommerce.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for notification service.
 * Provides centralized exception handling across all @RequestMapping methods.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotificationNotFoundException(
            NotificationNotFoundException ex,
            WebRequest request) {
        logger.error("Notification not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    @ExceptionHandler(InvalidNotificationException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidNotificationException(
            InvalidNotificationException ex,
            WebRequest request) {
        logger.error("Invalid notification data: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    @ExceptionHandler(NotificationSendException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotificationSendException(
            NotificationSendException ex,
            WebRequest request) {
        logger.error("Failed to send notification: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to send notification: " + ex.getMessage(), 
                        createErrorDetails(ex, request)));
    }

    @ExceptionHandler(NotificationServiceException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotificationServiceException(
            NotificationServiceException ex,
            WebRequest request) {
        logger.error("Notification service error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {
        logger.error("Method argument type mismatch: {}", ex.getMessage());
        String error = String.format("Parameter '%s' should be of type %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error, createErrorDetails(ex, request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        logger.error("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex,
            WebRequest request) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later.", 
                        createErrorDetails(ex, request)));
    }

    private Map<String, Object> createErrorDetails(Exception ex, WebRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", Instant.now().toString());
        details.put("path", request.getDescription(false).replace("uri=", ""));
        details.put("error", ex.getClass().getSimpleName());
        return details;
    }
}
