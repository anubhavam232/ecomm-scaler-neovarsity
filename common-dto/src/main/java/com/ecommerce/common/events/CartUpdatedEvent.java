package com.ecommerce.common.events;

public record CartUpdatedEvent(
        String eventId,
        Long userId,
        String action,
        Long productId,
        String productName,
        int quantity,
        double unitPrice,
        double cartTotal,
        String timestamp
) {}
