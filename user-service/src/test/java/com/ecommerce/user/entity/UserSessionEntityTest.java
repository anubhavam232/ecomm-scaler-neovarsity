package com.ecommerce.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for UserSession entity class.
 * Tests constructors, getters, setters, and session management fields.
 * Ensures proper session entity behavior and data integrity.
 */
@DisplayName("UserSession Entity Tests")
class UserSessionEntityTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        /**
         * Tests default constructor.
         * Verifies UserSession can be created with no-args constructor.
         */
        @Test
        @DisplayName("Should create UserSession with default constructor")
        void shouldCreateUserSessionWithDefaultConstructor() {
            // Act
            UserSession session = new UserSession();

            // Assert
            assertThat(session).isNotNull();
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        /**
         * Tests sessionId field getter and setter.
         * Verifies session ID can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set sessionId correctly")
        void shouldGetAndSetSessionId() {
            // Arrange
            UserSession session = new UserSession();
            String expectedSessionId = "sess_abc123";

            // Act
            session.setSessionId(expectedSessionId);

            // Assert
            assertThat(session.getSessionId()).isEqualTo(expectedSessionId);
        }

        /**
         * Tests userId field getter and setter.
         * Verifies user ID can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set userId correctly")
        void shouldGetAndSetUserId() {
            // Arrange
            UserSession session = new UserSession();
            Long expectedUserId = 42L;

            // Act
            session.setUserId(expectedUserId);

            // Assert
            assertThat(session.getUserId()).isEqualTo(expectedUserId);
        }

        /**
         * Tests token field getter and setter.
         * Verifies JWT token can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set token correctly")
        void shouldGetAndSetToken() {
            // Arrange
            UserSession session = new UserSession();
            String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.signature";

            // Act
            session.setToken(expectedToken);

            // Assert
            assertThat(session.getToken()).isEqualTo(expectedToken);
        }

        /**
         * Tests active field getter and setter.
         * Verifies session active status can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set active status correctly")
        void shouldGetAndSetActive() {
            // Arrange
            UserSession session = new UserSession();

            // Act - set to true
            session.setActive(true);

            // Assert
            assertThat(session.isActive()).isTrue();

            // Act - set to false
            session.setActive(false);

            // Assert
            assertThat(session.isActive()).isFalse();
        }

        /**
         * Tests createdAt field getter and setter.
         * Verifies session creation timestamp can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set createdAt correctly")
        void shouldGetAndSetCreatedAt() {
            // Arrange
            UserSession session = new UserSession();
            String expectedTimestamp = "2023-06-15T10:30:00Z";

            // Act
            session.setCreatedAt(expectedTimestamp);

            // Assert
            assertThat(session.getCreatedAt()).isEqualTo(expectedTimestamp);
        }

        /**
         * Tests expiresAt field getter and setter.
         * Verifies session expiration timestamp can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set expiresAt correctly")
        void shouldGetAndSetExpiresAt() {
            // Arrange
            UserSession session = new UserSession();
            String expectedExpiry = "2023-06-16T10:30:00Z";

            // Act
            session.setExpiresAt(expectedExpiry);

            // Assert
            assertThat(session.getExpiresAt()).isEqualTo(expectedExpiry);
        }

        /**
         * Tests loggedOutAt field getter and setter.
         * Verifies logout timestamp can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set loggedOutAt correctly")
        void shouldGetAndSetLoggedOutAt() {
            // Arrange
            UserSession session = new UserSession();
            String expectedLogoutTime = "2023-06-15T18:45:00Z";

            // Act
            session.setLoggedOutAt(expectedLogoutTime);

            // Assert
            assertThat(session.getLoggedOutAt()).isEqualTo(expectedLogoutTime);
        }
    }

    @Nested
    @DisplayName("Complete Session Tests")
    class CompleteSessionTests {

        /**
         * Tests creating a complete active session with all fields.
         * Verifies all session fields can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should create complete active session")
        void shouldCreateCompleteActiveSession() {
            // Arrange
            String now = Instant.now().toString();
            String expiresAt = Instant.now().plusSeconds(86400).toString();

            // Act
            UserSession session = new UserSession();
            session.setSessionId("sess_xyz789");
            session.setUserId(100L);
            session.setToken("jwt.token.here");
            session.setActive(true);
            session.setCreatedAt(now);
            session.setExpiresAt(expiresAt);
            session.setLoggedOutAt(null);

            // Assert
            assertThat(session.getSessionId()).isEqualTo("sess_xyz789");
            assertThat(session.getUserId()).isEqualTo(100L);
            assertThat(session.getToken()).isEqualTo("jwt.token.here");
            assertThat(session.isActive()).isTrue();
            assertThat(session.getCreatedAt()).isEqualTo(now);
            assertThat(session.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(session.getLoggedOutAt()).isNull();
        }

        /**
         * Tests creating an inactive (logged out) session.
         * Verifies logged out session has loggedOutAt timestamp and active=false.
         */
        @Test
        @DisplayName("Should create inactive logged out session")
        void shouldCreateInactiveLoggedOutSession() {
            // Arrange
            String loggedOutTime = Instant.now().toString();

            // Act
            UserSession session = new UserSession();
            session.setSessionId("sess_old123");
            session.setUserId(50L);
            session.setToken("old.jwt.token");
            session.setActive(false);
            session.setLoggedOutAt(loggedOutTime);

            // Assert
            assertThat(session.isActive()).isFalse();
            assertThat(session.getLoggedOutAt()).isEqualTo(loggedOutTime);
        }

        /**
         * Tests session with long JWT token (1024 characters).
         * Verifies token field can handle large JWT tokens.
         */
        @Test
        @DisplayName("Should handle long JWT token")
        void shouldHandleLongJwtToken() {
            // Arrange
            String longToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                    + "a".repeat(800)
                    + ".signature";

            // Act
            UserSession session = new UserSession();
            session.setToken(longToken);

            // Assert
            assertThat(session.getToken()).isEqualTo(longToken);
            assertThat(session.getToken().length()).isGreaterThan(800);
        }

        /**
         * Tests session timestamps are parseable as ISO-8601 format.
         * Verifies timestamp strings can be converted to Instant objects.
         */
        @Test
        @DisplayName("Should have parseable timestamp strings")
        void shouldHaveParseableTimestamps() {
            // Arrange
            String createdAt = Instant.now().toString();
            String expiresAt = Instant.now().plusSeconds(3600).toString();

            // Act
            UserSession session = new UserSession();
            session.setCreatedAt(createdAt);
            session.setExpiresAt(expiresAt);

            // Assert
            Instant parsedCreated = Instant.parse(session.getCreatedAt());
            Instant parsedExpiry = Instant.parse(session.getExpiresAt());

            assertThat(parsedCreated).isNotNull();
            assertThat(parsedExpiry).isNotNull();
            assertThat(parsedExpiry).isAfter(parsedCreated);
        }
    }
}
