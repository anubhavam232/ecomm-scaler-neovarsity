package com.ecommerce.notification.controller;

import com.ecommerce.notification.exception.InvalidNotificationException;
import com.ecommerce.notification.model.NotificationRecord;
import com.ecommerce.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private List<NotificationRecord> sampleNotifications;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(notificationService);

        // Create sample notifications
        sampleNotifications = Arrays.asList(
                new NotificationRecord(
                        "notif-001",
                        1L,
                        "user1@example.com",
                        "EMAIL",
                        "Amazon SES (us-east-1)",
                        "ses-msg-001",
                        "USER_REGISTRATION_WELCOME",
                        "Welcome to E-Commerce",
                        "Welcome message body",
                        Instant.now().toString()
                ),
                new NotificationRecord(
                        "notif-002",
                        1L,
                        "user1@example.com",
                        "EMAIL",
                        "Amazon SES (us-east-1)",
                        "ses-msg-002",
                        "ORDER_STATUS_UPDATE",
                        "Order Update",
                        "Your order has been shipped",
                        Instant.now().toString()
                )
        );
    }

    // ========== Get All Notifications Tests ==========

    @Test
    void testGetAllNotifications_Success() throws Exception {
        // Arrange
        when(notificationService.getAllNotifications()).thenReturn(sampleNotifications);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All SES notifications retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].notificationId").value("notif-001"))
                .andExpect(jsonPath("$.data[0].userId").value(1))
                .andExpect(jsonPath("$.data[0].recipientEmail").value("user1@example.com"))
                .andExpect(jsonPath("$.data[0].eventType").value("USER_REGISTRATION_WELCOME"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(notificationService, times(1)).getAllNotifications();
    }

    @Test
    void testGetAllNotifications_EmptyList() throws Exception {
        // Arrange
        when(notificationService.getAllNotifications()).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All SES notifications retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(notificationService, times(1)).getAllNotifications();
    }

    @Test
    void testGetAllNotifications_ServiceException() throws Exception {
        // Arrange
        when(notificationService.getAllNotifications())
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(notificationService, times(1)).getAllNotifications();
    }

    // ========== Get User Notifications Tests ==========

    @Test
    void testGetUserNotifications_Success() throws Exception {
        // Arrange
        when(notificationService.getNotificationsForUser(1L)).thenReturn(sampleNotifications);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User notifications retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].userId").value(1))
                .andExpect(jsonPath("$.data[1].userId").value(1))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(notificationService, times(1)).getNotificationsForUser(1L);
    }

    @Test
    void testGetUserNotifications_EmptyList() throws Exception {
        // Arrange
        when(notificationService.getNotificationsForUser(999L)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User notifications retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(notificationService, times(1)).getNotificationsForUser(999L);
    }

    @Test
    void testGetUserNotifications_InvalidUserId_Zero() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/user/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User ID must be a positive number"));

        verify(notificationService, never()).getNotificationsForUser(anyLong());
    }

    @Test
    void testGetUserNotifications_InvalidUserId_Negative() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/user/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User ID must be a positive number"));

        verify(notificationService, never()).getNotificationsForUser(anyLong());
    }

    @Test
    void testGetUserNotifications_InvalidUserIdFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/user/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(notificationService, never()).getNotificationsForUser(anyLong());
    }

    @Test
    void testGetUserNotifications_ServiceException() throws Exception {
        // Arrange
        when(notificationService.getNotificationsForUser(1L))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/user/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(notificationService, times(1)).getNotificationsForUser(1L);
    }

    // ========== Response Structure Tests ==========

    @Test
    void testResponseStructure_ContainsAllRequiredFields() throws Exception {
        // Arrange
        when(notificationService.getAllNotifications()).thenReturn(sampleNotifications);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testNotificationRecord_ContainsAllFields() throws Exception {
        // Arrange
        when(notificationService.getAllNotifications()).thenReturn(sampleNotifications);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].notificationId").exists())
                .andExpect(jsonPath("$.data[0].userId").exists())
                .andExpect(jsonPath("$.data[0].recipientEmail").exists())
                .andExpect(jsonPath("$.data[0].channel").exists())
                .andExpect(jsonPath("$.data[0].provider").exists())
                .andExpect(jsonPath("$.data[0].sesMessageId").exists())
                .andExpect(jsonPath("$.data[0].eventType").exists())
                .andExpect(jsonPath("$.data[0].subject").exists())
                .andExpect(jsonPath("$.data[0].body").exists())
                .andExpect(jsonPath("$.data[0].sentAt").exists());
    }

    // ========== Edge Case Tests ==========

    @Test
    void testGetUserNotifications_LargeUserId() throws Exception {
        // Arrange
        Long largeUserId = Long.MAX_VALUE;
        when(notificationService.getNotificationsForUser(largeUserId)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/user/" + largeUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService, times(1)).getNotificationsForUser(largeUserId);
    }

    @Test
    void testGetUserNotifications_MultipleRequestsSameUser() throws Exception {
        // Arrange
        when(notificationService.getNotificationsForUser(1L)).thenReturn(sampleNotifications);

        // Act & Assert - Make multiple requests
        mockMvc.perform(get("/api/v1/notifications/user/1"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/v1/notifications/user/1"))
                .andExpect(status().isOk());

        verify(notificationService, times(2)).getNotificationsForUser(1L);
    }
}
