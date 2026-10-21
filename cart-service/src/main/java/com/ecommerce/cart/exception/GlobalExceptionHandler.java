package com.ecommerce.cart.exception;

import com.ecommerce.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for cart-service.
 * Provides centralized exception handling for all controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles CartNotFoundException - HTTP 404
     */
    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCartNotFound(CartNotFoundException ex) {
        logger.error("Cart not found: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handles CartItemNotFoundException - HTTP 404
     */
    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCartItemNotFound(CartItemNotFoundException ex) {
        logger.error("Cart item not found: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handles InvalidCartOperationException - HTTP 400
     */
    @ExceptionHandler(InvalidCartOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCartOperation(InvalidCartOperationException ex) {
        logger.error("Invalid cart operation: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handles IllegalArgumentException - HTTP 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.error("Invalid argument: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handles MethodArgumentTypeMismatchException - HTTP 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value '%s' for parameter '%s'", 
                ex.getValue(), ex.getName());
        logger.error("Type mismatch: {}", message);
        ApiResponse<Object> response = new ApiResponse<>(false, message, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handles all other CartServiceException - HTTP 500
     */
    @ExceptionHandler(CartServiceException.class)
    public ResponseEntity<ApiResponse<Object>> handleCartServiceException(CartServiceException ex) {
        logger.error("Cart service error: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = new ApiResponse<>(false, "Cart service error: " + ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles all other uncaught exceptions - HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = new ApiResponse<>(false, "An unexpected error occurred. Please try again later.", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Malformed JSON request: " + ex.getMostSpecificCause().getMessage(), null));
    }
}
