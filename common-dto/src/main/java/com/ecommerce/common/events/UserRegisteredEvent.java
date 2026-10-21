package com.ecommerce.common.events;

public record UserRegisteredEvent(
        String eventId,
        Long userId,
        String email,
        String fullName,
        String registrationMethod,
        String timestamp
) {}
