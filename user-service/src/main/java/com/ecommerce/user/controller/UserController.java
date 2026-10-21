package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.UserDtos.*;
import com.ecommerce.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users/register")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("User registered successfully", userService.register(request)));
    }

    @PostMapping("/users/oauth2/social-login")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> socialLogin(@RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Social authentication successful", userService.socialLogin(request)));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", userService.login(request)));
    }

    @GetMapping("/auth/session")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> validateSession(
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(ApiResponse.ok("Session is valid", userService.validateSession(authorizationHeader)));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", userService.logout(authorizationHeader)));
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", userService.getProfile(userId)));
    }

    @PutMapping("/users/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @PathVariable("userId") Long userId,
            @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", userService.updateProfile(userId, request)));
    }

    @PostMapping("/users/password-reset/request")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestPasswordReset(
            @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Password reset link generated and sent", userService.requestPasswordReset(request)));
    }

    @PostMapping("/users/password-reset/confirm")
    public ResponseEntity<ApiResponse<Map<String, String>>> confirmPasswordReset(
            @RequestBody PasswordResetConfirmRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Password reset completed", userService.confirmPasswordReset(request)));
    }
}
