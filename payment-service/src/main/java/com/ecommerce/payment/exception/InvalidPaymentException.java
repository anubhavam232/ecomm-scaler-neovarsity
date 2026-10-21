package com.ecommerce.payment.exception;

/**
 * Exception thrown when payment data is invalid or payment processing fails.
 */
public class InvalidPaymentException extends RuntimeException {
    
    public InvalidPaymentException(String message) {
        super(message);
    }
    
    public InvalidPaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
