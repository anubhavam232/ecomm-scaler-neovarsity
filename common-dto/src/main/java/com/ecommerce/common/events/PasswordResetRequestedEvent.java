package com.ecommerce.common.events;

public record PasswordResetRequestedEvent(
        String eventId,
        Long userId,
        String email,
        String resetToken,
        String secureResetLink,
        String expiresAt
) {}
