package com.ecommerce.notification.service;

import com.ecommerce.common.events.*;
import com.ecommerce.notification.exception.InvalidNotificationException;
import com.ecommerce.notification.exception.NotificationSendException;
import com.ecommerce.notification.model.NotificationRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
    }

    // ========== UserRegisteredEvent Tests ==========

    @Test
    void testOnUserRegistered_Success() {
        // Arrange
        UserRegisteredEvent event = new UserRegisteredEvent(
                "evt-001",
                1L,
                "test@example.com",
                "John Doe",
                "EMAIL",
                Instant.now().toString()
        );

        // Act & Assert
        assertDoesNotThrow(() -> notificationService.onUserRegistered(event));
        
        // Verify notification was created
        List<NotificationRecord> notifications = notificationService.getNotificationsForUser(1L);
        assertEquals(1, notifications.size());
        assertEquals("test@example.com", notifications.get(0).recipientEmail());
    }

    @Test
    void testOnUserRegistered_NullEvent() {
        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onUserRegistered(null));
    }

    @Test
    void testOnUserRegistered_NullUserId() {
        // Arrange
        UserRegisteredEvent event = new UserRegisteredEvent(
                "evt-001",
                null,
                "test@example.com",
                "John Doe",
                "EMAIL",
                Instant.now().toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onUserRegistered(event));
    }

    @Test
    void testOnUserRegistered_NullEmail() {
        // Arrange
        UserRegisteredEvent event = new UserRegisteredEvent(
                "evt-001",
                1L,
                null,
                "John Doe",
                "EMAIL",
                Instant.now().toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onUserRegistered(event));
    }

    @Test
    void testOnUserRegistered_EmptyEmail() {
        // Arrange
        UserRegisteredEvent event = new UserRegisteredEvent(
                "evt-001",
                1L,
                "",
                "John Doe",
                "EMAIL",
                Instant.now().toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onUserRegistered(event));
    }

    @Test
    void testOnUserRegistered_InvalidEmailFormat() {
        // Arrange
        UserRegisteredEvent event = new UserRegisteredEvent(
                "evt-001",
                1L,
                "invalid-email",
                "John Doe",
                "EMAIL",
                Instant.now().toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onUserRegistered(event));
    }

    // ========== PasswordResetRequestedEvent Tests ==========

    @Test
    void testOnPasswordResetRequested_Success() {
        // Arrange
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                "evt-002",
                2L,
                "test@example.com",
                "reset-token-123",
                "https://example.com/reset?token=reset-token-123",
                Instant.now().plusSeconds(3600).toString()
        );

        // Act & Assert
        assertDoesNotThrow(() -> notificationService.onPasswordResetRequested(event));
        
        // Verify notification was created
        List<NotificationRecord> notifications = notificationService.getNotificationsForUser(2L);
        assertEquals(1, notifications.size());
        assertEquals("PASSWORD_RESET_SECURE_LINK", notifications.get(0).eventType());
    }

    @Test
    void testOnPasswordResetRequested_NullEvent() {
        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onPasswordResetRequested(null));
    }

    @Test
    void testOnPasswordResetRequested_NullEmail() {
        // Arrange
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                "evt-002",
                2L,
                null,
                "reset-token-123",
                "https://example.com/reset?token=reset-token-123",
                Instant.now().plusSeconds(3600).toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onPasswordResetRequested(event));
    }

    @Test
    void testOnPasswordResetRequested_NullResetLink() {
        // Arrange
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                "evt-002",
                2L,
                "test@example.com",
                "reset-token-123",
                null,
                Instant.now().plusSeconds(3600).toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onPasswordResetRequested(event));
    }

    @Test
    void testOnPasswordResetRequested_InvalidEmail() {
        // Arrange
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                "evt-002",
                2L,
                "invalid-email",
                "reset-token-123",
                "https://example.com/reset?token=reset-token-123",
                Instant.now().plusSeconds(3600).toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onPasswordResetRequested(event));
    }

    // ========== PaymentCompletedEvent Tests ==========

    @Test
    void testOnPaymentCompleted_Success() {
        // Arrange
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "evt-003",
                100L,
                "ORD-12345",
                3L,
                "test@example.com",
                "TXN-67890",
                "RCP-001",
                1499.99,
                "CREDIT_CARD",
                "SUCCESS",
                "hash123",
                Instant.now().toString()
        );

        // Act & Assert
        assertDoesNotThrow(() -> notificationService.onPaymentCompleted(event));
        
        // Verify notification was created
        List<NotificationRecord> notifications = notificationService.getNotificationsForUser(3L);
        assertEquals(1, notifications.size());
        assertEquals("PAYMENT_RECEIPT", notifications.get(0).eventType());
    }

    @Test
    void testOnPaymentCompleted_NullEvent() {
        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onPaymentCompleted(null));
    }

    @Test
    void testOnPaymentCompleted_NullEmail() {
        // Arrange
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "evt-003",
                100L,
                "ORD-12345",
                3L,
                null,
                "TXN-67890",
                "RCP-001",
                1499.99,
                "CREDIT_CARD",
                "SUCCESS",
                "hash123",
                Instant.now().toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onPaymentCompleted(event));
    }

    @Test
    void testOnPaymentCompleted_ZeroAmount() {
        // Arrange
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "evt-003",
                100L,
                "ORD-12345",
                3L,
                "test@example.com",
                "TXN-67890",
                "RCP-001",
                0.0,
                "CREDIT_CARD",
                "SUCCESS",
                "hash123",
                Instant.now().toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onPaymentCompleted(event));
    }

    @Test
    void testOnPaymentCompleted_NegativeAmount() {
        // Arrange
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "evt-003",
                100L,
                "ORD-12345",
                3L,
                "test@example.com",
                "TXN-67890",
                "RCP-001",
                -100.0,
                "CREDIT_CARD",
                "SUCCESS",
                "hash123",
                Instant.now().toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onPaymentCompleted(event));
    }

    // ========== OrderStatusUpdatedEvent Tests ==========

    @Test
    void testOnOrderStatusUpdated_Success() {
        // Arrange
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                "evt-004",
                100L,
                "ORD-12345",
                4L,
                "test@example.com",
                "SHIPPED",
                "TRK-999",
                "Your order has been shipped",
                Instant.now().toString()
        );

        // Act & Assert
        assertDoesNotThrow(() -> notificationService.onOrderStatusUpdated(event));
        
        // Verify notification was created
        List<NotificationRecord> notifications = notificationService.getNotificationsForUser(4L);
        assertEquals(1, notifications.size());
        assertEquals("ORDER_STATUS_UPDATE", notifications.get(0).eventType());
    }

    @Test
    void testOnOrderStatusUpdated_NullEvent() {
        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onOrderStatusUpdated(null));
    }

    @Test
    void testOnOrderStatusUpdated_NullOrderNumber() {
        // Arrange
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                "evt-004",
                100L,
                null,
                4L,
                "test@example.com",
                "SHIPPED",
                "TRK-999",
                "Your order has been shipped",
                Instant.now().toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onOrderStatusUpdated(event));
    }

    @Test
    void testOnOrderStatusUpdated_NullStatus() {
        // Arrange
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                "evt-004",
                100L,
                "ORD-12345",
                4L,
                "test@example.com",
                null,
                "TRK-999",
                "Your order has been shipped",
                Instant.now().toString()
        );

        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.onOrderStatusUpdated(event));
    }

    // ========== sendAmazonSesEmail Tests ==========

    @Test
    void testSendAmazonSesEmail_Success() {
        // Act
        NotificationRecord record = notificationService.sendAmazonSesEmail(
                5L,
                "test@example.com",
                "TEST_EVENT",
                "Test Subject",
                "Test Body"
        );

        // Assert
        assertNotNull(record);
        assertEquals(5L, record.userId());
        assertEquals("test@example.com", record.recipientEmail());
        assertEquals("TEST_EVENT", record.eventType());
        assertEquals("Test Subject", record.subject());
        assertEquals("Test Body", record.body());
        assertEquals("EMAIL", record.channel());
        assertEquals("Amazon SES (us-east-1)", record.provider());
    }

    @Test
    void testSendAmazonSesEmail_NullSubject() {
        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.sendAmazonSesEmail(
                        5L,
                        "test@example.com",
                        "TEST_EVENT",
                        null,
                        "Test Body"
                ));
    }

    @Test
    void testSendAmazonSesEmail_NullBody() {
        // Act & Assert
        assertThrows(InvalidNotificationException.class,
                () -> notificationService.sendAmazonSesEmail(
                        5L,
                        "test@example.com",
                        "TEST_EVENT",
                        "Test Subject",
                        null
                ));
    }

    // ========== Email Validation Tests ==========

    @Test
    void testValidEmailFormats() {
        // Valid email addresses should pass validation
        String[] validEmails = {
                "user@example.com",
                "user.name@example.co.uk",
                "user+tag@example.com",
                "user123@test-domain.com",
                "test.user+filter@my-domain.org"
        };

        for (int i = 0; i < validEmails.length; i++) {
            final String email = validEmails[i];
            final Long userId = (long) (i + 100);
            
            assertDoesNotThrow(() -> {
                UserRegisteredEvent event = new UserRegisteredEvent(
                        "evt-" + userId,
                        userId,
                        email,
                        "Test User",
                        "EMAIL",
                        Instant.now().toString()
                );
                notificationService.onUserRegistered(event);
            }, "Email " + email + " should be valid");
        }
    }

    @Test
    void testInvalidEmailFormats() {
        // Invalid email addresses should throw exception
        String[] invalidEmails = {
                "invalid",
                "@example.com",
                "user@",
                "user @example.com",
                "user@.com",
                "user..name@example."
        };

        for (int i = 0; i < invalidEmails.length; i++) {
            final String email = invalidEmails[i];
            final Long userId = (long) (i + 200);

            assertThrows(InvalidNotificationException.class, () -> {
                UserRegisteredEvent event = new UserRegisteredEvent(
                        "evt-" + userId,
                        userId,
                        email,
                        "Test User",
                        "EMAIL",
                        Instant.now().toString()
                );
                notificationService.onUserRegistered(event);
            }, "Email " + email + " should be invalid");
        }
    }

    // ========== Notification Retrieval Tests ==========

    @Test
    void testGetNotificationsForUser() {
        // Arrange - Send multiple notifications for the same user
        notificationService.sendAmazonSesEmail(10L, "user10@example.com", "EVENT1", "Subject1", "Body1");
        notificationService.sendAmazonSesEmail(10L, "user10@example.com", "EVENT2", "Subject2", "Body2");
        notificationService.sendAmazonSesEmail(11L, "user11@example.com", "EVENT3", "Subject3", "Body3");

        // Act
        List<NotificationRecord> user10Notifications = notificationService.getNotificationsForUser(10L);
        List<NotificationRecord> user11Notifications = notificationService.getNotificationsForUser(11L);

        // Assert
        assertEquals(2, user10Notifications.size());
        assertEquals(1, user11Notifications.size());
        assertTrue(user10Notifications.stream().allMatch(n -> n.userId().equals(10L)));
        assertTrue(user11Notifications.stream().allMatch(n -> n.userId().equals(11L)));
    }

    @Test
    void testGetAllNotifications() {
        // Arrange - Clear any existing notifications and add new ones
        NotificationService freshService = new NotificationService();
        freshService.sendAmazonSesEmail(20L, "user20@example.com", "EVENT1", "Subject1", "Body1");
        freshService.sendAmazonSesEmail(21L, "user21@example.com", "EVENT2", "Subject2", "Body2");
        freshService.sendAmazonSesEmail(22L, "user22@example.com", "EVENT3", "Subject3", "Body3");

        // Act
        List<NotificationRecord> allNotifications = freshService.getAllNotifications();

        // Assert
        assertEquals(3, allNotifications.size());
    }

    @Test
    void testGetNotificationsForUser_NoNotifications() {
        // Act
        List<NotificationRecord> notifications = notificationService.getNotificationsForUser(999L);

        // Assert
        assertNotNull(notifications);
        assertTrue(notifications.isEmpty());
    }
}
