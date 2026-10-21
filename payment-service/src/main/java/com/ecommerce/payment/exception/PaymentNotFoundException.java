package com.ecommerce.payment.exception;

/**
 * Exception thrown when a payment transaction is not found.
 */
public class PaymentNotFoundException extends RuntimeException {
    
    public PaymentNotFoundException(Long orderId) {
        super("Payment not found for order ID: " + orderId);
    }
    
    public PaymentNotFoundException(String identifier) {
        super("Payment not found: " + identifier);
    }
    
    public PaymentNotFoundException(String identifier, String type) {
        super("Payment not found with " + type + ": " + identifier);
    }
}
