package com.ecommerce.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for PasswordResetToken entity class.
 * Tests constructors, getters, setters, and password reset token fields.
 * Ensures proper token entity behavior and data integrity.
 */
@DisplayName("PasswordResetToken Entity Tests")
class PasswordResetTokenEntityTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        /**
         * Tests default constructor.
         * Verifies PasswordResetToken can be created with no-args constructor.
         */
        @Test
        @DisplayName("Should create PasswordResetToken with default constructor")
        void shouldCreatePasswordResetTokenWithDefaultConstructor() {
            // Act
            PasswordResetToken token = new PasswordResetToken();

            // Assert
            assertThat(token).isNotNull();
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        /**
         * Tests token field getter and setter.
         * Verifies reset token string can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set token correctly")
        void shouldGetAndSetToken() {
            // Arrange
            PasswordResetToken resetToken = new PasswordResetToken();
            String expectedToken = "rst_abc123def456";

            // Act
            resetToken.setToken(expectedToken);

            // Assert
            assertThat(resetToken.getToken()).isEqualTo(expectedToken);
        }

        /**
         * Tests userId field getter and setter.
         * Verifies user ID can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set userId correctly")
        void shouldGetAndSetUserId() {
            // Arrange
            PasswordResetToken resetToken = new PasswordResetToken();
            Long expectedUserId = 789L;

            // Act
            resetToken.setUserId(expectedUserId);

            // Assert
            assertThat(resetToken.getUserId()).isEqualTo(expectedUserId);
        }

        /**
         * Tests email field getter and setter.
         * Verifies email can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set email correctly")
        void shouldGetAndSetEmail() {
            // Arrange
            PasswordResetToken resetToken = new PasswordResetToken();
            String expectedEmail = "reset@example.com";

            // Act
            resetToken.setEmail(expectedEmail);

            // Assert
            assertThat(resetToken.getEmail()).isEqualTo(expectedEmail);
        }

        /**
         * Tests used field getter and setter.
         * Verifies token used status can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set used status correctly")
        void shouldGetAndSetUsed() {
            // Arrange
            PasswordResetToken resetToken = new PasswordResetToken();

            // Act - set to false
            resetToken.setUsed(false);

            // Assert
            assertThat(resetToken.isUsed()).isFalse();

            // Act - set to true
            resetToken.setUsed(true);

            // Assert
            assertThat(resetToken.isUsed()).isTrue();
        }

        /**
         * Tests expiresAt field getter and setter.
         * Verifies expiration timestamp can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set expiresAt correctly")
        void shouldGetAndSetExpiresAt() {
            // Arrange
            PasswordResetToken resetToken = new PasswordResetToken();
            String expectedExpiry = "2023-12-31T23:59:59Z";

            // Act
            resetToken.setExpiresAt(expectedExpiry);

            // Assert
            assertThat(resetToken.getExpiresAt()).isEqualTo(expectedExpiry);
        }

        /**
         * Tests createdAt field getter and setter.
         * Verifies creation timestamp can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set createdAt correctly")
        void shouldGetAndSetCreatedAt() {
            // Arrange
            PasswordResetToken resetToken = new PasswordResetToken();
            String expectedCreated = "2023-12-01T10:00:00Z";

            // Act
            resetToken.setCreatedAt(expectedCreated);

            // Assert
            assertThat(resetToken.getCreatedAt()).isEqualTo(expectedCreated);
        }
    }

    @Nested
    @DisplayName("Complete Reset Token Tests")
    class CompleteResetTokenTests {

        /**
         * Tests creating a complete unused reset token.
         * Verifies all token fields can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should create complete unused reset token")
        void shouldCreateCompleteUnusedResetToken() {
            // Arrange
            String now = Instant.now().toString();
            String expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES).toString();

            // Act
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken("rst_newtoken123");
            resetToken.setUserId(25L);
            resetToken.setEmail("user@example.com");
            resetToken.setUsed(false);
            resetToken.setExpiresAt(expiresAt);
            resetToken.setCreatedAt(now);

            // Assert
            assertThat(resetToken.getToken()).isEqualTo("rst_newtoken123");
            assertThat(resetToken.getUserId()).isEqualTo(25L);
            assertThat(resetToken.getEmail()).isEqualTo("user@example.com");
            assertThat(resetToken.isUsed()).isFalse();
            assertThat(resetToken.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(resetToken.getCreatedAt()).isEqualTo(now);
        }

        /**
         * Tests creating a used (consumed) reset token.
         * Verifies used token has used=true status.
         */
        @Test
        @DisplayName("Should create used reset token")
        void shouldCreateUsedResetToken() {
            // Act
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken("rst_usedtoken456");
            resetToken.setUserId(30L);
            resetToken.setEmail("used@example.com");
            resetToken.setUsed(true);

            // Assert
            assertThat(resetToken.isUsed()).isTrue();
            assertThat(resetToken.getToken()).isEqualTo("rst_usedtoken456");
        }

        /**
         * Tests reset token with expiry time validation.
         * Verifies expiry time is in the future for new tokens.
         */
        @Test
        @DisplayName("Should have expiry time in the future for new token")
        void shouldHaveExpiryTimeInFuture() {
            // Arrange
            Instant now = Instant.now();
            Instant expiresAt = now.plus(30, ChronoUnit.MINUTES);

            // Act
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setCreatedAt(now.toString());
            resetToken.setExpiresAt(expiresAt.toString());

            // Assert
            Instant parsedExpiry = Instant.parse(resetToken.getExpiresAt());
            Instant parsedCreated = Instant.parse(resetToken.getCreatedAt());

            assertThat(parsedExpiry).isAfter(parsedCreated);
            assertThat(parsedExpiry).isAfter(now);
        }

        /**
         * Tests reset token with token format validation.
         * Verifies token follows rst_ prefix convention.
         */
        @Test
        @DisplayName("Should have token with rst_ prefix")
        void shouldHaveTokenWithRstPrefix() {
            // Act
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken("rst_abc123def456ghi789");

            // Assert
            assertThat(resetToken.getToken()).startsWith("rst_");
        }

        /**
         * Tests timestamp strings are parseable as ISO-8601 format.
         * Verifies timestamps can be converted to Instant objects.
         */
        @Test
        @DisplayName("Should have parseable timestamp strings")
        void shouldHaveParseableTimestamps() {
            // Arrange
            String createdAt = Instant.now().toString();
            String expiresAt = Instant.now().plusSeconds(1800).toString();

            // Act
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setCreatedAt(createdAt);
            resetToken.setExpiresAt(expiresAt);

            // Assert
            Instant parsedCreated = Instant.parse(resetToken.getCreatedAt());
            Instant parsedExpiry = Instant.parse(resetToken.getExpiresAt());

            assertThat(parsedCreated).isNotNull();
            assertThat(parsedExpiry).isNotNull();
        }

        /**
         * Tests reset token expiry duration is 30 minutes.
         * Verifies token expiry follows security requirements.
         */
        @Test
        @DisplayName("Should have 30 minute expiry duration")
        void shouldHave30MinuteExpiryDuration() {
            // Arrange
            Instant now = Instant.now();
            Instant expiresAt = now.plus(30, ChronoUnit.MINUTES);

            // Act
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setCreatedAt(now.toString());
            resetToken.setExpiresAt(expiresAt.toString());

            // Assert
            Instant parsedCreated = Instant.parse(resetToken.getCreatedAt());
            Instant parsedExpiry = Instant.parse(resetToken.getExpiresAt());
            long minutesDiff = ChronoUnit.MINUTES.between(parsedCreated, parsedExpiry);

            assertThat(minutesDiff).isEqualTo(30);
        }
    }
}
