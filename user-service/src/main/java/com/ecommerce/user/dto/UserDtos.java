package com.ecommerce.user.dto;

public class UserDtos {
    public record RegisterRequest(
            String email,
            String password,
            String fullName,
            String phone,
            String defaultAddress
    ) {}

    public record SocialLoginRequest(
            String provider,
            String providerId,
            String email,
            String fullName
    ) {}

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record ProfileUpdateRequest(
            String fullName,
            String phone,
            String defaultAddress
    ) {}

    public record PasswordResetRequest(
            String email
    ) {}

    public record PasswordResetConfirmRequest(
            String token,
            String newPassword
    ) {}

    public record UserProfileResponse(
            Long id,
            String email,
            String fullName,
            String phone,
            String defaultAddress,
            String authProvider,
            String role,
            String createdAt,
            String updatedAt
    ) {}

    public record AuthSessionResponse(
            Long userId,
            String email,
            String fullName,
            String sessionId,
            String accessToken,
            String tokenType,
            String expiresAt,
            boolean active
    ) {}
}
