package com.ecommerce.common.events;

public record OrderStatusUpdatedEvent(
        String eventId,
        Long orderId,
        String orderNumber,
        Long userId,
        String userEmail,
        String status,
        String trackingNumber,
        String description,
        String timestamp
) {}
