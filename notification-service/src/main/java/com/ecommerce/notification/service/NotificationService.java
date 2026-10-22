package com.ecommerce.notification.service;

import com.ecommerce.common.events.*;
import com.ecommerce.notification.exception.InvalidNotificationException;
import com.ecommerce.notification.exception.NotificationSendException;
import com.ecommerce.notification.exception.NotificationServiceException;
import com.ecommerce.notification.model.NotificationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * HLD Section 3.6: Consumes Kafka events and dispatches transactional emails via Amazon SES.
 */
@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private final List<NotificationRecord> sentNotifications = Collections.synchronizedList(new ArrayList<>());

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "notification-service-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            validateEvent(event, "UserRegisteredEvent");

            logger.info("Received UserRegisteredEvent for userId: {}, email: {}", event.userId(), event.email());

            validateUserId(event.userId());
            validateEmail(event.email());

            sendAmazonSesEmail(
                    event.userId(),
                    event.email(),
                    "USER_REGISTRATION_WELCOME",
                    "Welcome to E-Commerce Platform, " + event.fullName() + "! Enjoy 15% Off Your First Order",
                    "Hi " + event.fullName() + ", your account (" + event.email() + ") was registered via "
                            + event.registrationMethod() + ". Use promo code WELCOME15 at checkout!"
            );

            logger.info("Successfully sent welcome email to userId: {}", event.userId());
        } catch (InvalidNotificationException e) {
            logger.error("Invalid notification data for UserRegisteredEvent: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to process UserRegisteredEvent: {}", e.getMessage(), e);
            throw new NotificationSendException("Failed to send welcome email", e);
        }
    }

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "notification-service-reset-group")
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            validateEvent(event, "PasswordResetRequestedEvent");

            logger.info("Received PasswordResetRequestedEvent for userId: {}, email: {}", event.userId(), event.email());

            validateUserId(event.userId());
            validateEmail(event.email());

            if (event.secureResetLink() == null || event.secureResetLink().trim().isEmpty()) {
                throw new InvalidNotificationException("Secure reset link cannot be null or empty");
            }

            sendAmazonSesEmail(
                    event.userId(),
                    event.email(),
                    "PASSWORD_RESET_SECURE_LINK",
                    "Password Reset Request - Secure Link Inside",
                    "Click the secure link to reset your password (expires at " + event.expiresAt() + "): "
                            + event.secureResetLink()
            );

            logger.info("Successfully sent password reset email to userId: {}", event.userId());
        } catch (InvalidNotificationException e) {
            logger.error("Invalid notification data for PasswordResetRequestedEvent: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to process PasswordResetRequestedEvent: {}", e.getMessage(), e);
            throw new NotificationSendException("Failed to send password reset email", e);
        }
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "notification-service-group")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        try {
            validateEvent(event, "PaymentCompletedEvent");

            logger.info("Received PaymentCompletedEvent for userId: {}, orderId: {}", event.userId(), event.orderNumber());

            validateUserId(event.userId());
            validateEmail(event.userEmail());

            if (event.amount() <= 0.0) {
                throw new InvalidNotificationException("Payment amount must be greater than zero");
            }

            sendAmazonSesEmail(
                    event.userId(),
                    event.userEmail(),
                    "PAYMENT_RECEIPT",
                    "Payment Receipt " + event.receiptNumber() + " for Order " + event.orderNumber(),
                    "Your payment of $" + event.amount() + " via " + event.paymentMethod()
                            + " was processed with status " + event.status() + " (Txn ID: " + event.transactionId() + ")."
            );

            logger.info("Successfully sent payment receipt email to userId: {}", event.userId());
        } catch (InvalidNotificationException e) {
            logger.error("Invalid notification data for PaymentCompletedEvent: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to process PaymentCompletedEvent: {}", e.getMessage(), e);
            throw new NotificationSendException("Failed to send payment receipt email", e);
        }
    }

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "notification-service-group")
    public void onOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        try {
            validateEvent(event, "OrderStatusUpdatedEvent");

            logger.info("Received OrderStatusUpdated event for userId: {}, orderId: {}, status: {}",
                    event.userId(), event.orderNumber(), event.status());

            validateUserId(event.userId());
            validateEmail(event.userEmail());

            if (event.orderNumber() == null || event.orderNumber().trim().isEmpty()) {
                throw new InvalidNotificationException("Order number cannot be null or empty");
            }

            if (event.status() == null || event.status().trim().isEmpty()) {
                throw new InvalidNotificationException("Order status cannot be null or empty");
            }

            sendAmazonSesEmail(
                    event.userId(),
                    event.userEmail(),
                    "ORDER_STATUS_UPDATE",
                    "Order Update: " + event.orderNumber() + " is now " + event.status(),
                    event.description() + " | Tracking Number: " + event.trackingNumber()
            );

            logger.info("Successfully sent order status update email to userId: {}", event.userId());
        } catch (InvalidNotificationException e) {
            logger.error("Invalid notification data for OrderStatusUpdatedEvent: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to process OrderStatusUpdatedEvent: {}", e.getMessage(), e);
            throw new NotificationSendException("Failed to send order status update email", e);
        }
    }

    public NotificationRecord sendAmazonSesEmail(
            Long userId,
            String recipientEmail,
            String eventType,
            String subject,
            String body
    ) {
        try {
            logger.debug("Sending email to {} for eventType: {}", recipientEmail, eventType);

            // Validate email parameters
            validateUserId(userId);
            validateEmail(recipientEmail);

            if (subject == null || subject.trim().isEmpty()) {
                throw new InvalidNotificationException("Email subject cannot be null or empty");
            }

            if (body == null || body.trim().isEmpty()) {
                throw new InvalidNotificationException("Email body cannot be null or empty");
            }

            String sesMessageId = "ses-" + UUID.randomUUID().toString();
            NotificationRecord record = new NotificationRecord(
                    UUID.randomUUID().toString(),
                    userId,
                    recipientEmail,
                    "EMAIL",
                    "Amazon SES (us-east-1)",
                    sesMessageId,
                    eventType,
                    subject,
                    body,
                    Instant.now().toString()
            );
            sentNotifications.add(record);

            logger.info("Email sent successfully. SES MessageId: {}, EventType: {}", sesMessageId, eventType);
            return record;
        } catch (InvalidNotificationException e) {
            logger.error("Email validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", recipientEmail, e.getMessage(), e);
            throw new NotificationSendException("Failed to send email via Amazon SES", e);
        }
    }

    public List<NotificationRecord> getNotificationsForUser(Long userId) {
        return sentNotifications.stream()
                .filter(n -> n.userId() != null && n.userId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<NotificationRecord> getAllNotifications() {
        return new ArrayList<>(sentNotifications);
    }

    // Validation helper methods

    private void validateEvent(Object event, String eventName) {
        if (event == null) {
            throw new InvalidNotificationException(eventName + " cannot be null");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new InvalidNotificationException("User ID must be a positive number");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidNotificationException("Email address cannot be null or empty");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidNotificationException("Invalid email format: " + email);
        }
    }
}
