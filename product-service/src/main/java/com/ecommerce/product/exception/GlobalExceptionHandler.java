package com.ecommerce.product.exception;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for Product Service.
 * Intercepts exceptions thrown by controllers and maps them to appropriate HTTP responses.
 * Uses @RestControllerAdvice to centralize exception handling logic.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException (product not found scenarios).
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    /**
     * Handles DuplicateResourceException (duplicate SKU scenarios).
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(
            DuplicateResourceException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    /**
     * Handles InvalidRequestException (invalid data or parameters).
     * Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(
            InvalidRequestException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), createErrorDetails(ex, request)));
    }

    /**
     * Handles base ProductServiceException.
     * Returns HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(ProductServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductServiceException(
            ProductServiceException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Service error: " + ex.getMessage(), createErrorDetails(ex, request)));
    }

    /**
     * Handles all other unexpected exceptions.
     * Returns HTTP 500 Internal Server Error with generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage(), createErrorDetails(ex, request)));
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
