package com.ecommerce.payment.dto;

public class PaymentDtos {
    public record ManualPaymentRequest(
            Long orderId,
            String orderNumber,
            Long userId,
            String userEmail,
            double amount,
            String paymentMethod,
            String instrumentDetails
    ) {}

    public record PaymentReceiptResponse(
            String receiptNumber,
            String transactionId,
            Long orderId,
            String orderNumber,
            Long userId,
            String userEmail,
            double amountPaid,
            String currency,
            String paymentMethod,
            String maskedInstrument,
            String gatewayProvider,
            String status,
            String cryptographicSignature,
            String issuedAt
    ) {}
}
