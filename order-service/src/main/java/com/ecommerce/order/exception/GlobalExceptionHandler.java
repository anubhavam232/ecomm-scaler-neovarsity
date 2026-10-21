package com.ecommerce.order.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/**
 * Global Exception Handler for Order Service
 * Handles all exceptions thrown by controllers and services
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle OrderNotFoundException - when order is not found in database
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(
            OrderNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                "ORDER_NOT_FOUND",
                request.getRequestURI()
        );
        error.addDetail("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle InvalidOrderException - when order data is invalid
     */
    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderException(
            InvalidOrderException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                "INVALID_ORDER",
                request.getRequestURI()
        );
        error.addDetail("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle IllegalArgumentException - for invalid arguments
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                "BAD_REQUEST",
                request.getRequestURI()
        );
        error.addDetail("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle MethodArgumentNotValidException - for validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Validation failed",
                "VALIDATION_ERROR",
                request.getRequestURI()
        );
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                error.addDetail(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle HttpMessageNotReadableException - for malformed JSON
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Malformed JSON request",
                "INVALID_REQUEST_BODY",
                request.getRequestURI()
        );
        error.addDetail("details", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle MethodArgumentTypeMismatchException - for type conversion errors
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName()),
                "TYPE_MISMATCH",
                request.getRequestURI()
        );
        error.addDetail("parameter", ex.getName());
        error.addDetail("providedValue", ex.getValue());
        error.addDetail("requiredType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle DataIntegrityViolationException - for database constraint violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Database constraint violation",
                "DATA_INTEGRITY_VIOLATION",
                request.getRequestURI()
        );
        error.addDetail("details", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handle NullPointerException - for null reference errors
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(
            NullPointerException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "A required value was null",
                "NULL_POINTER_ERROR",
                request.getRequestURI()
        );
        error.addDetail("details", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle all other exceptions - catch-all handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                "An unexpected error occurred",
                "INTERNAL_SERVER_ERROR",
                request.getRequestURI()
        );
        error.addDetail("errorType", ex.getClass().getSimpleName());
        error.addDetail("details", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
