package com.ecommerce.notification;

import com.ecommerce.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.ses.SesClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private SesClient sesClient;

    @Test
    void contextLoads() {
        // Verify that the application context loads successfully
        assertNotNull(notificationService);
    }

    @Test
    void testNotificationServiceBeanIsCreated() {
        // Verify that NotificationService bean is created
        assertNotNull(notificationService);
    }

    @Test
    void testSesClientBeanIsMocked() {
        // Verify that SesClient is properly mocked
        assertNotNull(sesClient);
    }
}
