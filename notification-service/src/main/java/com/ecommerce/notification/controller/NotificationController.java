package com.ecommerce.notification.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.notification.exception.InvalidNotificationException;
import com.ecommerce.notification.model.NotificationRecord;
import com.ecommerce.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationRecord>>> getAllNotifications() {
        try {
            logger.info("Fetching all notifications");
            List<NotificationRecord> notifications = notificationService.getAllNotifications();
            logger.info("Successfully retrieved {} notifications", notifications.size());
            return ResponseEntity.ok(ApiResponse.ok("All SES notifications retrieved", notifications));
        } catch (Exception e) {
            logger.error("Error retrieving all notifications: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationRecord>>> getUserNotifications(@PathVariable("userId") Long userId) {
        try {
            logger.info("Fetching notifications for userId: {}", userId);

            // Validate userId
            if (userId == null || userId <= 0) {
                logger.error("Invalid userId: {}", userId);
                throw new InvalidNotificationException("User ID must be a positive number");
            }

            List<NotificationRecord> notifications = notificationService.getNotificationsForUser(userId);
            logger.info("Successfully retrieved {} notifications for userId: {}", notifications.size(), userId);
            return ResponseEntity.ok(ApiResponse.ok("User notifications retrieved", notifications));
        } catch (InvalidNotificationException e) {
            logger.error("Validation error for userId {}: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving notifications for userId {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}
