package com.ecommerce.payment.exception;

/**
 * Exception thrown when payment gateway communication fails.
 */
public class PaymentGatewayException extends RuntimeException {
    
    private final String gatewayName;
    
    public PaymentGatewayException(String gatewayName, String message) {
        super("Payment gateway [" + gatewayName + "] error: " + message);
        this.gatewayName = gatewayName;
    }
    
    public PaymentGatewayException(String gatewayName, String message, Throwable cause) {
        super("Payment gateway [" + gatewayName + "] error: " + message, cause);
        this.gatewayName = gatewayName;
    }
    
    public String getGatewayName() {
        return gatewayName;
    }
}
