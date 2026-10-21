package com.ecommerce.common.events;

public record OrderPlacedEvent(
        String eventId,
        Long orderId,
        String orderNumber,
        Long userId,
        String userEmail,
        double totalAmount,
        String paymentMethod,
        String paymentInstrumentDetails,
        String deliveryAddress,
        String timestamp
) {}
