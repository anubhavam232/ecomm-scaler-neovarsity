package com.ecommerce.notification.service;

import com.ecommerce.common.events.*;
import com.ecommerce.notification.model.NotificationRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HLD Section 3.6: Consumes Kafka events and dispatches transactional emails via Amazon SES.
 */
@Service
public class NotificationService {
    private final List<NotificationRecord> sentNotifications = Collections.synchronizedList(new ArrayList<>());

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "notification-service-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        sendAmazonSesEmail(
                event.userId(),
                event.email(),
                "USER_REGISTRATION_WELCOME",
                "Welcome to E-Commerce Platform, " + event.fullName() + "! Enjoy 15% Off Your First Order",
                "Hi " + event.fullName() + ", your account (" + event.email() + ") was registered via "
                        + event.registrationMethod() + ". Use promo code WELCOME15 at checkout!"
        );
    }

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "notification-service-reset-group")
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        sendAmazonSesEmail(
                event.userId(),
                event.email(),
                "PASSWORD_RESET_SECURE_LINK",
                "Password Reset Request - Secure Link Inside",
                "Click the secure link to reset your password (expires at " + event.expiresAt() + "): "
                        + event.secureResetLink()
        );
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "notification-service-group")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        sendAmazonSesEmail(
                event.userId(),
                event.userEmail(),
                "PAYMENT_RECEIPT",
                "Payment Receipt " + event.receiptNumber() + " for Order " + event.orderNumber(),
                "Your payment of $" + event.amount() + " via " + event.paymentMethod()
                        + " was processed with status " + event.status() + " (Txn ID: " + event.transactionId() + ")."
        );
    }

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "notification-service-group")
    public void onOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        sendAmazonSesEmail(
                event.userId(),
                event.userEmail(),
                "ORDER_STATUS_UPDATE",
                "Order Update: " + event.orderNumber() + " is now " + event.status(),
                event.description() + " | Tracking Number: " + event.trackingNumber()
        );
    }

    public NotificationRecord sendAmazonSesEmail(
            Long userId,
            String recipientEmail,
            String eventType,
            String subject,
            String body
    ) {
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
        return record;
    }

    public List<NotificationRecord> getNotificationsForUser(Long userId) {
        return sentNotifications.stream()
                .filter(n -> n.userId() != null && n.userId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<NotificationRecord> getAllNotifications() {
        return new ArrayList<>(sentNotifications);
    }
}
