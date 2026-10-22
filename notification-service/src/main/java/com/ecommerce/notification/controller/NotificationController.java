package com.ecommerce.notification.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.notification.model.NotificationRecord;
import com.ecommerce.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationRecord>>> getAllNotifications() {
        return ResponseEntity.ok(ApiResponse.ok("All SES notifications retrieved", notificationService.getAllNotifications()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationRecord>>> getUserNotifications(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("User notifications retrieved", notificationService.getNotificationsForUser(userId)));
    }
}
