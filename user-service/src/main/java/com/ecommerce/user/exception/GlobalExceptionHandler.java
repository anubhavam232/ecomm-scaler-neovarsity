package com.ecommerce.user.exception;

import com.ecommerce.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for all controllers in the user-service.
 * Maps specific exceptions to appropriate HTTP status codes and consistent error responses.
 * Uses @RestControllerAdvice to intercept exceptions across all @RestController classes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ResourceNotFoundException (e.g., user not found, session not found).
     * Returns HTTP 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    /**
     * Handles InvalidCredentialsException (e.g., wrong password, invalid email).
     * Returns HTTP 401 UNAUTHORIZED.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentialsException(
            InvalidCredentialsException ex, WebRequest request) {
        logger.warn("Invalid credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    /**
     * Handles DuplicateResourceException (e.g., email already exists).
     * Returns HTTP 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        logger.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    /**
     * Handles InvalidTokenException (e.g., expired token, already used token, invalid token).
     * Returns HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidTokenException(
            InvalidTokenException ex, WebRequest request) {
        logger.warn("Invalid token: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    /**
     * Handles generic IllegalArgumentException for validation errors.
     * Returns HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    /**
     * Handles all other unexpected exceptions.
     * Returns HTTP 500 INTERNAL SERVER ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex, WebRequest request) {
        logger.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred. Please try again later.",
                        createErrorDetails(ex, request)
                ));
    }

    /**
     * Creates error details map with timestamp and path information.
     */
    private Map<String, Object> createErrorDetails(Exception ex, WebRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("timestamp", Instant.now().toString());
        details.put("path", request.getDescription(false).replace("uri=", ""));
        details.put("error", ex.getClass().getSimpleName());
        return details;
    }
}
