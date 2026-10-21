package com.ecommerce.payment.exception;

/**
 * Exception thrown when payment processing encounters an error.
 */
public class PaymentProcessingException extends RuntimeException {
    
    public PaymentProcessingException(String message) {
        super(message);
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
