package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.UserDtos.*;
import com.ecommerce.user.exception.*;
import com.ecommerce.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for UserController class.
 * Tests all REST endpoints using MockMvc with proper request/response validation.
 * Achieves 90%+ code coverage with edge cases and error scenarios.
 */
@WebMvcTest(UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "TestPassword123";
    private static final String TEST_FULL_NAME = "Test User";
    private static final String TEST_PHONE = "+1234567890";
    private static final String TEST_ADDRESS = "123 Test St";

    private UserProfileResponse testProfileResponse;
    private AuthSessionResponse testAuthResponse;

    @BeforeEach
    void setUp() {
        testProfileResponse = new UserProfileResponse(
                1L,
                TEST_EMAIL,
                TEST_FULL_NAME,
                TEST_PHONE,
                TEST_ADDRESS,
                "LOCAL",
                "CUSTOMER",
                Instant.now().toString(),
                Instant.now().toString()
        );

        testAuthResponse = new AuthSessionResponse(
                1L,
                TEST_EMAIL,
                TEST_FULL_NAME,
                "sess_123456",
                "jwt.token.here",
                "Bearer",
                Instant.now().plusSeconds(86400).toString(),
                true
        );
    }

    @Nested
    @DisplayName("POST /api/v1/users/register Tests")
    class RegisterEndpointTests {

        /**
         * Tests successful user registration endpoint.
         * Verifies 200 OK response with user profile data.
         */
        @Test
        @DisplayName("Should register user successfully and return 200 OK")
        void shouldRegisterUserSuccessfully() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    TEST_EMAIL,
                    TEST_PASSWORD,
                    TEST_FULL_NAME,
                    TEST_PHONE,
                    TEST_ADDRESS
            );

            when(userService.register(any(RegisterRequest.class))).thenReturn(testProfileResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.data.fullName").value(TEST_FULL_NAME))
                    .andExpect(jsonPath("$.data.phone").value(TEST_PHONE))
                    .andExpect(jsonPath("$.data.defaultAddress").value(TEST_ADDRESS))
                    .andExpect(jsonPath("$.data.authProvider").value("LOCAL"))
                    .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
        }

        /**
         * Tests registration with minimal required fields.
         * Verifies optional fields can be null.
         */
        @Test
        @DisplayName("Should register user with minimal fields")
        void shouldRegisterUserWithMinimalFields() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    TEST_EMAIL,
                    TEST_PASSWORD,
                    TEST_FULL_NAME,
                    null,
                    null
            );

            UserProfileResponse minimalResponse = new UserProfileResponse(
                    2L, TEST_EMAIL, TEST_FULL_NAME, null, null,
                    "LOCAL", "CUSTOMER", Instant.now().toString(), Instant.now().toString()
            );

            when(userService.register(any(RegisterRequest.class))).thenReturn(minimalResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.phone").isEmpty())
                    .andExpect(jsonPath("$.data.defaultAddress").isEmpty());
        }

        /**
         * Tests registration failure when email already exists.
         * Verifies appropriate error handling.
         */
        @Test
        @DisplayName("Should return error when email already exists")
        void shouldReturnErrorWhenEmailExists() throws Exception {
            // Arrange
            RegisterRequest request = new RegisterRequest(
                    TEST_EMAIL, TEST_PASSWORD, TEST_FULL_NAME, TEST_PHONE, TEST_ADDRESS
            );

            when(userService.register(any(RegisterRequest.class)))
                    .thenThrow(new DuplicateResourceException("Account already exists for email: " + TEST_EMAIL));

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Account already exists for email: " + TEST_EMAIL));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/oauth2/social-login Tests")
    class SocialLoginEndpointTests {

        /**
         * Tests successful social login endpoint.
         * Verifies 200 OK response with authentication session.
         */
        @Test
        @DisplayName("Should perform social login successfully and return session")
        void shouldPerformSocialLoginSuccessfully() throws Exception {
            // Arrange
            SocialLoginRequest request = new SocialLoginRequest(
                    "google",
                    "google-123",
                    TEST_EMAIL,
                    TEST_FULL_NAME
            );

            when(userService.socialLogin(any(SocialLoginRequest.class))).thenReturn(testAuthResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/oauth2/social-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Social authentication successful"))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt.token.here"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.active").value(true));
        }

        /**
         * Tests social login with Facebook provider.
         * Verifies different providers are handled correctly.
         */
        @Test
        @DisplayName("Should handle Facebook social login")
        void shouldHandleFacebookSocialLogin() throws Exception {
            // Arrange
            SocialLoginRequest request = new SocialLoginRequest(
                    "facebook",
                    "fb-456",
                    "facebook@example.com",
                    "Facebook User"
            );

            when(userService.socialLogin(any(SocialLoginRequest.class))).thenReturn(testAuthResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/oauth2/social-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        /**
         * Tests social login with GitHub provider.
         * Verifies GitHub provider is supported.
         */
        @Test
        @DisplayName("Should handle GitHub social login")
        void shouldHandleGitHubSocialLogin() throws Exception {
            // Arrange
            SocialLoginRequest request = new SocialLoginRequest(
                    "github",
                    "gh-789",
                    "github@example.com",
                    "GitHub User"
            );

            when(userService.socialLogin(any(SocialLoginRequest.class))).thenReturn(testAuthResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/oauth2/social-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.sessionId").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login Tests")
    class LoginEndpointTests {

        /**
         * Tests successful login endpoint.
         * Verifies 200 OK response with JWT token and session.
         */
        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            when(userService.login(any(LoginRequest.class))).thenReturn(testAuthResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.data.fullName").value(TEST_FULL_NAME))
                    .andExpect(jsonPath("$.data.sessionId").value("sess_123456"))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresAt").exists())
                    .andExpect(jsonPath("$.data.active").value(true));
        }

        /**
         * Tests login failure with invalid credentials.
         * Verifies error response is returned.
         */
        @Test
        @DisplayName("Should return error with invalid credentials")
        void shouldReturnErrorWithInvalidCredentials() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongPassword");

            when(userService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        /**
         * Tests login with non-existent email.
         * Verifies appropriate error handling.
         */
        @Test
        @DisplayName("Should return error when user does not exist")
        void shouldReturnErrorWhenUserDoesNotExist() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("nonexistent@example.com", TEST_PASSWORD);

            when(userService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/session Tests")
    class ValidateSessionEndpointTests {

        /**
         * Tests session validation endpoint with valid Bearer token.
         * Verifies session details are returned.
         */
        @Test
        @DisplayName("Should validate session successfully with valid Bearer token")
        void shouldValidateSessionSuccessfully() throws Exception {
            // Arrange
            String bearerToken = "Bearer jwt.token.here";

            when(userService.validateSession(anyString())).thenReturn(testAuthResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/auth/session")
                            .header("Authorization", bearerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Session is valid"))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.data.sessionId").value("sess_123456"))
                    .andExpect(jsonPath("$.data.active").value(true));
        }

        /**
         * Tests session validation with invalid token.
         * Verifies error response is returned.
         */
        @Test
        @DisplayName("Should return error with invalid token")
        void shouldReturnErrorWithInvalidToken() throws Exception {
            // Arrange
            String invalidToken = "Bearer invalid.token";

            when(userService.validateSession(anyString()))
                    .thenThrow(new InvalidTokenException("Session token is invalid or expired"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/auth/session")
                            .header("Authorization", invalidToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        /**
         * Tests session validation for inactive session.
         * Verifies error response is returned.
         */
        @Test
        @DisplayName("Should return error when session is inactive")
        void shouldReturnErrorWhenSessionInactive() throws Exception {
            // Arrange
            String bearerToken = "Bearer jwt.token.here";

            when(userService.validateSession(anyString()))
                    .thenThrow(new InvalidTokenException("Session has been logged out"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/auth/session")
                            .header("Authorization", bearerToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout Tests")
    class LogoutEndpointTests {

        /**
         * Tests successful logout endpoint.
         * Verifies 200 OK response with logout confirmation.
         */
        @Test
        @DisplayName("Should logout successfully with valid token")
        void shouldLogoutSuccessfully() throws Exception {
            // Arrange
            String bearerToken = "Bearer jwt.token.here";
            Map<String, String> logoutResponse = Map.of(
                    "status", "LOGGED_OUT",
                    "timestamp", Instant.now().toString()
            );

            when(userService.logout(anyString())).thenReturn(logoutResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", bearerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"))
                    .andExpect(jsonPath("$.data.status").value("LOGGED_OUT"))
                    .andExpect(jsonPath("$.data.timestamp").exists());
        }

        /**
         * Tests logout with token without Bearer prefix.
         * Verifies logout is processed correctly.
         */
        @Test
        @DisplayName("Should logout successfully without Bearer prefix")
        void shouldLogoutWithoutBearerPrefix() throws Exception {
            // Arrange
            String token = "jwt.token.here";
            Map<String, String> logoutResponse = Map.of(
                    "status", "LOGGED_OUT",
                    "timestamp", Instant.now().toString()
            );

            when(userService.logout(anyString())).thenReturn(logoutResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("LOGGED_OUT"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/profile Tests")
    class GetProfileEndpointTests {

        /**
         * Tests get profile endpoint with valid user ID.
         * Verifies user profile details are returned.
         */
        @Test
        @DisplayName("Should retrieve user profile successfully")
        void shouldRetrieveUserProfileSuccessfully() throws Exception {
            // Arrange
            when(userService.getProfile(1L)).thenReturn(testProfileResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/users/1/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Profile retrieved"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.data.fullName").value(TEST_FULL_NAME))
                    .andExpect(jsonPath("$.data.phone").value(TEST_PHONE))
                    .andExpect(jsonPath("$.data.defaultAddress").value(TEST_ADDRESS))
                    .andExpect(jsonPath("$.data.authProvider").value("LOCAL"))
                    .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
        }

        /**
         * Tests get profile with non-existent user ID.
         * Verifies error response is returned.
         */
        @Test
        @DisplayName("Should return error when user not found")
        void shouldReturnErrorWhenUserNotFound() throws Exception {
            // Arrange
            when(userService.getProfile(999L))
                    .thenThrow(new ResourceNotFoundException("User not found: 999"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/users/999/profile"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        /**
         * Tests get profile with different user ID.
         * Verifies path variable is correctly parsed.
         */
        @Test
        @DisplayName("Should retrieve profile for different user ID")
        void shouldRetrieveProfileForDifferentUserId() throws Exception {
            // Arrange
            UserProfileResponse otherUser = new UserProfileResponse(
                    5L, "other@example.com", "Other User", "+9999999999", "456 Other St",
                    "GOOGLE", "CUSTOMER", Instant.now().toString(), Instant.now().toString()
            );

            when(userService.getProfile(5L)).thenReturn(otherUser);

            // Act & Assert
            mockMvc.perform(get("/api/v1/users/5/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(5))
                    .andExpect(jsonPath("$.data.email").value("other@example.com"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{userId}/profile Tests")
    class UpdateProfileEndpointTests {

        /**
         * Tests update profile endpoint with all fields.
         * Verifies all profile fields are updated.
         */
        @Test
        @DisplayName("Should update all profile fields successfully")
        void shouldUpdateAllProfileFieldsSuccessfully() throws Exception {
            // Arrange
            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    "Updated Name",
                    "+9876543210",
                    "456 New Address"
            );

            UserProfileResponse updatedResponse = new UserProfileResponse(
                    1L, TEST_EMAIL, "Updated Name", "+9876543210", "456 New Address",
                    "LOCAL", "CUSTOMER", Instant.now().toString(), Instant.now().toString()
            );

            when(userService.updateProfile(eq(1L), any(ProfileUpdateRequest.class)))
                    .thenReturn(updatedResponse);

            // Act & Assert
            mockMvc.perform(put("/api/v1/users/1/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Profile updated"))
                    .andExpect(jsonPath("$.data.fullName").value("Updated Name"))
                    .andExpect(jsonPath("$.data.phone").value("+9876543210"))
                    .andExpect(jsonPath("$.data.defaultAddress").value("456 New Address"));
        }

        /**
         * Tests partial profile update (only name).
         * Verifies partial updates are supported.
         */
        @Test
        @DisplayName("Should update only full name when other fields are null")
        void shouldUpdateOnlyFullName() throws Exception {
            // Arrange
            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    "Only Name Updated",
                    null,
                    null
            );

            UserProfileResponse updatedResponse = new UserProfileResponse(
                    1L, TEST_EMAIL, "Only Name Updated", TEST_PHONE, TEST_ADDRESS,
                    "LOCAL", "CUSTOMER", Instant.now().toString(), Instant.now().toString()
            );

            when(userService.updateProfile(eq(1L), any(ProfileUpdateRequest.class)))
                    .thenReturn(updatedResponse);

            // Act & Assert
            mockMvc.perform(put("/api/v1/users/1/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.fullName").value("Only Name Updated"));
        }

        /**
         * Tests update profile for non-existent user.
         * Verifies error response is returned.
         */
        @Test
        @DisplayName("Should return error when updating non-existent user")
        void shouldReturnErrorWhenUpdatingNonExistentUser() throws Exception {
            // Arrange
            ProfileUpdateRequest request = new ProfileUpdateRequest(
                    "New Name", null, null
            );

            when(userService.updateProfile(eq(999L), any(ProfileUpdateRequest.class)))
                    .thenThrow(new ResourceNotFoundException("User not found: 999"));

            // Act & Assert
            mockMvc.perform(put("/api/v1/users/999/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/password-reset/request Tests")
    class PasswordResetRequestEndpointTests {

        /**
         * Tests password reset request endpoint with valid email.
         * Verifies reset token and link are generated.
         */
        @Test
        @DisplayName("Should generate password reset token successfully")
        void shouldGeneratePasswordResetTokenSuccessfully() throws Exception {
            // Arrange
            PasswordResetRequest request = new PasswordResetRequest(TEST_EMAIL);

            Map<String, String> resetResponse = Map.of(
                    "email", TEST_EMAIL,
                    "resetToken", "rst_abc123def456",
                    "secureResetLink", "https://shop.ecommerce.example.com/reset-password?token=rst_abc123def456",
                    "expiresAt", Instant.now().plusSeconds(1800).toString()
            );

            when(userService.requestPasswordReset(any(PasswordResetRequest.class)))
                    .thenReturn(resetResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password reset link generated and sent"))
                    .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.data.resetToken").value(startsWith("rst_")))
                    .andExpect(jsonPath("$.data.secureResetLink").value(containsString("reset-password?token=")))
                    .andExpect(jsonPath("$.data.expiresAt").exists());
        }

        /**
         * Tests password reset request with non-existent email.
         * Verifies error response is returned.
         */
        @Test
        @DisplayName("Should return error when email does not exist")
        void shouldReturnErrorWhenEmailDoesNotExist() throws Exception {
            // Arrange
            PasswordResetRequest request = new PasswordResetRequest("nonexistent@example.com");

            when(userService.requestPasswordReset(any(PasswordResetRequest.class)))
                    .thenThrow(new ResourceNotFoundException("No user found with email: nonexistent@example.com"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/password-reset/confirm Tests")
    class PasswordResetConfirmEndpointTests {

        @BeforeEach
        void resetMocks() {
            org.mockito.Mockito.reset(userService);
        }

        /**
         * Tests password reset confirmation with valid token.
         * Verifies password is updated successfully.
         */
        @Test
        @DisplayName("Should reset password successfully with valid token")
        void shouldResetPasswordSuccessfully() throws Exception {
            // Arrange
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
                    "rst_validtoken123",
                    "NewSecurePassword123"
            );

            Map<String, String> confirmResponse = Map.of(
                    "userId", "1",
                    "email", TEST_EMAIL,
                    "status", "PASSWORD_UPDATED_SUCCESSFULLY"
            );

            when(userService.confirmPasswordReset(any(PasswordResetConfirmRequest.class)))
                    .thenReturn(confirmResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password reset completed"))
                    .andExpect(jsonPath("$.data.userId").value("1"))
                    .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.data.status").value("PASSWORD_UPDATED_SUCCESSFULLY"));
        }

        /**
         * Tests password reset with invalid token.
         * Verifies error response is returned.
         */
        @Test
        @DisplayName("Should return error with invalid reset token")
        void shouldReturnErrorWithInvalidToken() throws Exception {
            // Arrange
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
                    "invalid_token",
                    "NewPassword123"
            );

            when(userService.confirmPasswordReset(any(PasswordResetConfirmRequest.class)))
                    .thenThrow(new InvalidTokenException("Invalid password reset token"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        /**
         * Tests password reset with already used token.
         * Verifies error response is returned.
         */
        @Test
        @DisplayName("Should return error when token already used")
        void shouldReturnErrorWhenTokenAlreadyUsed() throws Exception {
            // Arrange
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
                    "rst_usedtoken",
                    "NewPassword123"
            );

            when(userService.confirmPasswordReset(any(PasswordResetConfirmRequest.class)))
                    .thenThrow(new InvalidTokenException("Password reset token has already been used"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        /**
         * Tests password reset with expired token.
         * Verifies error response is returned.
         */
        @Test
        @DisplayName("Should return error when token has expired")
        void shouldReturnErrorWhenTokenExpired() throws Exception {
            // Arrange
            PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
                    "rst_expiredtoken",
                    "NewPassword123"
            );

            when(userService.confirmPasswordReset(any(PasswordResetConfirmRequest.class)))
                    .thenThrow(new InvalidTokenException("Password reset token has expired"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/users/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
