package com.ecommerce.order.exception;

import com.ecommerce.order.controller.OrderController;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;
    
    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Should handle OrderNotFoundException and return 404")
    void testOrderNotFoundException() throws Exception {
        // Arrange
        when(orderService.getOrderById(999L))
                .thenThrow(new OrderNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found with ID: 999"))
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/999"))
                .andExpect(jsonPath("$.details.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle OrderNotFoundException with custom message")
    void testOrderNotFoundExceptionWithCustomMessage() throws Exception {
        // Arrange
        when(orderService.getOrderById(888L))
                .thenThrow(new OrderNotFoundException("Custom order not found message"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/888")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Custom order not found message"))
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should handle InvalidOrderException and return 400")
    void testInvalidOrderException() throws Exception {
        // Arrange
        when(orderService.checkout(any()))
                .thenThrow(new InvalidOrderException("Invalid order data provided"));

        String requestBody = """
            {
                "userId": 1,
                "userEmail": "test@example.com",
                "deliveryAddress": "123 Test St",
                "paymentMethod": "CREDIT_CARD",
                "paymentInstrumentDetails": "4111111111111111",
                "items": []
            }""";

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid order data provided"))
                .andExpect(jsonPath("$.error").value("INVALID_ORDER"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/checkout"))
                .andExpect(jsonPath("$.details.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException and return 400")
    void testIllegalArgumentException() throws Exception {
        // Arrange
        when(orderService.checkout(any()))
                .thenThrow(new IllegalArgumentException("Cannot checkout with an empty item list"));

        String requestBody = """
            {
                "userId": 1,
                "userEmail": "test@example.com",
                "deliveryAddress": "123 Test St",
                "paymentMethod": "CREDIT_CARD",
                "paymentInstrumentDetails": "4111111111111111",
                "items": []
            }""";

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot checkout with an empty item list"))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.details.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException for malformed JSON")
    void testHttpMessageNotReadableException() throws Exception {
        // Act & Assert - Send malformed JSON
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/checkout"))
                .andExpect(jsonPath("$.details.details").exists());
    }

    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException for invalid path variable type")
    void testMethodArgumentTypeMismatchException() throws Exception {
        // Act & Assert - Send string instead of Long for orderId
        mockMvc.perform(get("/api/v1/orders/invalid-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid value")))
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/invalid-id"))
                .andExpect(jsonPath("$.details.parameter").value("orderId"))
                .andExpect(jsonPath("$.details.providedValue").value("invalid-id"))
                .andExpect(jsonPath("$.details.requiredType").value("Long"));
    }

    @Test
    @DisplayName("Should handle DataIntegrityViolationException and return 409")
    void testDataIntegrityViolationException() throws Exception {
        // Arrange
        when(orderService.checkout(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate order number"));

        String requestBody = """
            {
                "userId": 1,
                "userEmail": "test@example.com",
                "deliveryAddress": "123 Test St",
                "paymentMethod": "CREDIT_CARD",
                "paymentInstrumentDetails": "4111111111111111",
                "items": [{"productId": 1, "sku": "SKU-001", "productName": "Test", "unitPrice": 100.0, "quantity": 1}]
            }""";

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Database constraint violation"))
                .andExpect(jsonPath("$.error").value("DATA_INTEGRITY_VIOLATION"))
                .andExpect(jsonPath("$.details.details").exists());
    }

    @Test
    @DisplayName("Should handle NullPointerException and return 500")
    void testNullPointerException() throws Exception {
        // Arrange
        when(orderService.getOrderById(1L))
                .thenThrow(new NullPointerException("Required value is null"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("A required value was null"))
                .andExpect(jsonPath("$.error").value("NULL_POINTER_ERROR"))
                .andExpect(jsonPath("$.details.details").value("Required value is null"));
    }

    @Test
    @DisplayName("Should handle generic Exception and return 500")
    void testGenericException() throws Exception {
        // Arrange
        when(orderService.getOrderById(1L))
                .thenThrow(new RuntimeException("Unexpected error occurred"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.details.errorType").value("RuntimeException"))
                .andExpect(jsonPath("$.details.details").value("Unexpected error occurred"));
    }

    @Test
    @DisplayName("Should include path in error response")
    void testErrorResponseIncludesPath() throws Exception {
        // Arrange
        when(orderService.getOrderById(555L))
                .thenThrow(new OrderNotFoundException(555L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/555")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/v1/orders/555"));
    }

    @Test
    @DisplayName("Should include timestamp in error response")
    void testErrorResponseIncludesTimestamp() throws Exception {
        // Arrange
        when(orderService.getOrderById(777L))
                .thenThrow(new OrderNotFoundException(777L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/777")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.timestamp").exists())
                .andExpect(jsonPath("$.details.timestamp").isString());
    }

    @Test
    @DisplayName("Should set success to false in all error responses")
    void testErrorResponseSuccessIsFalse() throws Exception {
        // Arrange
        when(orderService.getOrderById(123L))
                .thenThrow(new OrderNotFoundException(123L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should handle missing request body")
    void testMissingRequestBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
