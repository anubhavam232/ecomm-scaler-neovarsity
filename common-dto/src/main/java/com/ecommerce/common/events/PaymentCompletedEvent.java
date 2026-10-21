package com.ecommerce.common.events;

public record PaymentCompletedEvent(
        String eventId,
        Long orderId,
        String orderNumber,
        Long userId,
        String userEmail,
        String transactionId,
        String receiptNumber,
        double amount,
        String paymentMethod,
        String status,
        String signatureHash,
        String timestamp
) {}
