package com.ecommerce.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for User entity class.
 * Tests constructors, getters, setters, and default field values.
 * Ensures proper entity behavior and data integrity.
 */
@DisplayName("User Entity Tests")
class UserEntityTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        /**
         * Tests default constructor.
         * Verifies default values are set correctly for authProvider, role, active, and timestamps.
         */
        @Test
        @DisplayName("Should set default values when created with default constructor")
        void shouldSetDefaultValuesOnConstruction() {
            // Act
            User user = new User();

            // Assert
            assertThat(user.getAuthProvider()).isEqualTo("LOCAL");
            assertThat(user.getRole()).isEqualTo("CUSTOMER");
            assertThat(user.isActive()).isTrue();
            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isNotNull();
            assertThat(user.getCreatedAt()).isEqualTo(user.getUpdatedAt());
        }

        /**
         * Tests that created and updated timestamps are valid ISO-8601 strings.
         * Verifies timestamps can be parsed correctly.
         */
        @Test
        @DisplayName("Should create valid timestamps on construction")
        void shouldCreateValidTimestampsOnConstruction() {
            // Act
            User user = new User();

            // Assert
            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isNotNull();
            
            // Verify timestamps are parseable
            Instant createdInstant = Instant.parse(user.getCreatedAt());
            Instant updatedInstant = Instant.parse(user.getUpdatedAt());
            
            assertThat(createdInstant).isNotNull();
            assertThat(updatedInstant).isNotNull();
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        /**
         * Tests id field getter and setter.
         * Verifies id can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set id correctly")
        void shouldGetAndSetId() {
            // Arrange
            User user = new User();
            Long expectedId = 123L;

            // Act
            user.setId(expectedId);

            // Assert
            assertThat(user.getId()).isEqualTo(expectedId);
        }

        /**
         * Tests email field getter and setter.
         * Verifies email can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set email correctly")
        void shouldGetAndSetEmail() {
            // Arrange
            User user = new User();
            String expectedEmail = "test@example.com";

            // Act
            user.setEmail(expectedEmail);

            // Assert
            assertThat(user.getEmail()).isEqualTo(expectedEmail);
        }

        /**
         * Tests passwordHash field getter and setter.
         * Verifies password hash can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set passwordHash correctly")
        void shouldGetAndSetPasswordHash() {
            // Arrange
            User user = new User();
            String expectedHash = "hashedPassword123";

            // Act
            user.setPasswordHash(expectedHash);

            // Assert
            assertThat(user.getPasswordHash()).isEqualTo(expectedHash);
        }

        /**
         * Tests passwordSalt field getter and setter.
         * Verifies password salt can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set passwordSalt correctly")
        void shouldGetAndSetPasswordSalt() {
            // Arrange
            User user = new User();
            String expectedSalt = "randomSalt456";

            // Act
            user.setPasswordSalt(expectedSalt);

            // Assert
            assertThat(user.getPasswordSalt()).isEqualTo(expectedSalt);
        }

        /**
         * Tests fullName field getter and setter.
         * Verifies full name can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set fullName correctly")
        void shouldGetAndSetFullName() {
            // Arrange
            User user = new User();
            String expectedName = "John Doe";

            // Act
            user.setFullName(expectedName);

            // Assert
            assertThat(user.getFullName()).isEqualTo(expectedName);
        }

        /**
         * Tests phone field getter and setter.
         * Verifies phone number can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set phone correctly")
        void shouldGetAndSetPhone() {
            // Arrange
            User user = new User();
            String expectedPhone = "+1234567890";

            // Act
            user.setPhone(expectedPhone);

            // Assert
            assertThat(user.getPhone()).isEqualTo(expectedPhone);
        }

        /**
         * Tests defaultAddress field getter and setter.
         * Verifies address can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set defaultAddress correctly")
        void shouldGetAndSetDefaultAddress() {
            // Arrange
            User user = new User();
            String expectedAddress = "123 Main St, City, State 12345";

            // Act
            user.setDefaultAddress(expectedAddress);

            // Assert
            assertThat(user.getDefaultAddress()).isEqualTo(expectedAddress);
        }

        /**
         * Tests authProvider field getter and setter.
         * Verifies auth provider can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set authProvider correctly")
        void shouldGetAndSetAuthProvider() {
            // Arrange
            User user = new User();
            String expectedProvider = "GOOGLE";

            // Act
            user.setAuthProvider(expectedProvider);

            // Assert
            assertThat(user.getAuthProvider()).isEqualTo(expectedProvider);
        }

        /**
         * Tests socialProviderId field getter and setter.
         * Verifies social provider ID can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set socialProviderId correctly")
        void shouldGetAndSetSocialProviderId() {
            // Arrange
            User user = new User();
            String expectedProviderId = "google-12345";

            // Act
            user.setSocialProviderId(expectedProviderId);

            // Assert
            assertThat(user.getSocialProviderId()).isEqualTo(expectedProviderId);
        }

        /**
         * Tests role field getter and setter.
         * Verifies user role can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set role correctly")
        void shouldGetAndSetRole() {
            // Arrange
            User user = new User();
            String expectedRole = "ADMIN";

            // Act
            user.setRole(expectedRole);

            // Assert
            assertThat(user.getRole()).isEqualTo(expectedRole);
        }

        /**
         * Tests active field getter and setter.
         * Verifies active status can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set active status correctly")
        void shouldGetAndSetActive() {
            // Arrange
            User user = new User();

            // Act
            user.setActive(false);

            // Assert
            assertThat(user.isActive()).isFalse();

            // Act - set back to true
            user.setActive(true);

            // Assert
            assertThat(user.isActive()).isTrue();
        }

        /**
         * Tests createdAt field getter and setter.
         * Verifies created timestamp can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set createdAt correctly")
        void shouldGetAndSetCreatedAt() {
            // Arrange
            User user = new User();
            String expectedTimestamp = "2023-01-15T10:30:00Z";

            // Act
            user.setCreatedAt(expectedTimestamp);

            // Assert
            assertThat(user.getCreatedAt()).isEqualTo(expectedTimestamp);
        }

        /**
         * Tests updatedAt field getter and setter.
         * Verifies updated timestamp can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should get and set updatedAt correctly")
        void shouldGetAndSetUpdatedAt() {
            // Arrange
            User user = new User();
            String expectedTimestamp = "2023-01-20T15:45:00Z";

            // Act
            user.setUpdatedAt(expectedTimestamp);

            // Assert
            assertThat(user.getUpdatedAt()).isEqualTo(expectedTimestamp);
        }
    }

    @Nested
    @DisplayName("Complete User Entity Tests")
    class CompleteUserTests {

        /**
         * Tests creating a complete user with all fields populated.
         * Verifies all fields can be set and retrieved correctly.
         */
        @Test
        @DisplayName("Should create complete user with all fields")
        void shouldCreateCompleteUser() {
            // Arrange & Act
            User user = new User();
            user.setId(1L);
            user.setEmail("john.doe@example.com");
            user.setPasswordHash("hashedPassword");
            user.setPasswordSalt("randomSalt");
            user.setFullName("John Doe");
            user.setPhone("+1234567890");
            user.setDefaultAddress("123 Main St");
            user.setAuthProvider("LOCAL");
            user.setSocialProviderId(null);
            user.setRole("CUSTOMER");
            user.setActive(true);
            user.setCreatedAt("2023-01-01T00:00:00Z");
            user.setUpdatedAt("2023-01-02T00:00:00Z");

            // Assert
            assertThat(user.getId()).isEqualTo(1L);
            assertThat(user.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(user.getPasswordHash()).isEqualTo("hashedPassword");
            assertThat(user.getPasswordSalt()).isEqualTo("randomSalt");
            assertThat(user.getFullName()).isEqualTo("John Doe");
            assertThat(user.getPhone()).isEqualTo("+1234567890");
            assertThat(user.getDefaultAddress()).isEqualTo("123 Main St");
            assertThat(user.getAuthProvider()).isEqualTo("LOCAL");
            assertThat(user.getSocialProviderId()).isNull();
            assertThat(user.getRole()).isEqualTo("CUSTOMER");
            assertThat(user.isActive()).isTrue();
            assertThat(user.getCreatedAt()).isEqualTo("2023-01-01T00:00:00Z");
            assertThat(user.getUpdatedAt()).isEqualTo("2023-01-02T00:00:00Z");
        }

        /**
         * Tests creating a social login user with provider ID.
         * Verifies social authentication fields are handled correctly.
         */
        @Test
        @DisplayName("Should create social login user with provider details")
        void shouldCreateSocialLoginUser() {
            // Arrange & Act
            User user = new User();
            user.setEmail("google.user@gmail.com");
            user.setFullName("Google User");
            user.setAuthProvider("GOOGLE");
            user.setSocialProviderId("google-98765");

            // Assert
            assertThat(user.getAuthProvider()).isEqualTo("GOOGLE");
            assertThat(user.getSocialProviderId()).isEqualTo("google-98765");
        }

        /**
         * Tests creating user with null optional fields.
         * Verifies null values are handled correctly.
         */
        @Test
        @DisplayName("Should handle null optional fields correctly")
        void shouldHandleNullOptionalFields() {
            // Arrange & Act
            User user = new User();
            user.setEmail("minimal@example.com");
            user.setFullName("Minimal User");
            user.setPhone(null);
            user.setDefaultAddress(null);
            user.setSocialProviderId(null);

            // Assert
            assertThat(user.getPhone()).isNull();
            assertThat(user.getDefaultAddress()).isNull();
            assertThat(user.getSocialProviderId()).isNull();
        }
    }
}
