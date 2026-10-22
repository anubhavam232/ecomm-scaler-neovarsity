package com.ecommerce.notification.model;

public record NotificationRecord(
        String notificationId,
        Long userId,
        String recipientEmail,
        String channel,
        String provider,
        String sesMessageId,
        String eventType,
        String subject,
        String body,
        String sentAt
) {}
