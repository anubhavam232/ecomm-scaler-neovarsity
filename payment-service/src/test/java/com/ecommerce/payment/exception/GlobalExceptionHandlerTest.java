package com.ecommerce.payment.exception;

import com.ecommerce.payment.controller.PaymentController;
import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.kafka.KafkaException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for GlobalExceptionHandler.
 * Verifies exception handling and error response formatting.
 */
@WebMvcTest(PaymentController.class)
@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    // ========== PaymentNotFoundException Tests ==========

    @Test
    @DisplayName("Should handle PaymentNotFoundException with 404 status")
    void handlePaymentNotFoundException_ShouldReturn404() throws Exception {
        // Arrange
        when(paymentService.getReceiptByOrderId(999L))
                .thenThrow(new PaymentNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("PAYMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Payment not found for order ID: 999"))
                .andExpect(jsonPath("$.path").value("/api/v1/payments/receipt/order/999"))
                .andExpect(jsonPath("$.details.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle PaymentNotFoundException with custom identifier")
    void handlePaymentNotFoundException_WithIdentifier_ShouldReturn404() throws Exception {
        // Arrange
        when(paymentService.getReceiptByOrderId(any()))
                .thenThrow(new PaymentNotFoundException("TXN-123", "transaction ID"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PAYMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Payment not found with transaction ID: TXN-123"));
    }

    // ========== InvalidPaymentException Tests ==========

    @Test
    @DisplayName("Should handle InvalidPaymentException with 400 status")
    void handleInvalidPaymentException_ShouldReturn400() throws Exception {
        // Arrange
        when(paymentService.processManualPayment(any()))
                .thenThrow(new InvalidPaymentException("Payment amount must be greater than zero"));

        String requestBody = "{\"orderId\":100,\"orderNumber\":\"ORD-100\",\"userId\":1,\"userEmail\":\"test@example.com\",\"amount\":0,\"paymentMethod\":\"CREDIT_CARD\",\"instrumentDetails\":\"4242\"}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_PAYMENT"))
                .andExpect(jsonPath("$.message").value("Payment amount must be greater than zero"))
                .andExpect(jsonPath("$.path").value("/api/v1/payments/process"));
    }

    // ========== IllegalArgumentException Tests ==========

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400 status")
    void handleIllegalArgumentException_ShouldReturn400() throws Exception {
        // Arrange
        when(paymentService.getReceiptByOrderId(any()))
                .thenThrow(new IllegalArgumentException("Invalid argument provided"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid argument provided"));
    }

    // ========== HttpMessageNotReadableException Tests ==========

    @Test
    @DisplayName("Should handle malformed JSON with 400 status")
    void handleHttpMessageNotReadableException_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.details.details").exists());
    }

    // ========== MethodArgumentTypeMismatchException Tests ==========

    @Test
    @DisplayName("Should handle type mismatch with 400 status")
    void handleMethodArgumentTypeMismatchException_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details.parameter").value("orderId"))
                .andExpect(jsonPath("$.details.requiredType").value("Long"));
    }

    // ========== NullPointerException Tests ==========

    @Test
    @DisplayName("Should handle NullPointerException with 500 status")
    void handleNullPointerException_ShouldReturn500() throws Exception {
        // Arrange
        when(paymentService.getReceiptByOrderId(any()))
                .thenThrow(new NullPointerException("Null value encountered"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("NULL_POINTER_ERROR"))
                .andExpect(jsonPath("$.message").value("A required value was null"))
                .andExpect(jsonPath("$.details.details").value("Null value encountered"));
    }

    // ========== Generic Exception Tests ==========

    @Test
    @DisplayName("Should handle generic Exception with 500 status")
    void handleGenericException_ShouldReturn500() throws Exception {
        // Arrange
        when(paymentService.getReceiptByOrderId(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details.errorType").value("RuntimeException"))
                .andExpect(jsonPath("$.details.details").value("Unexpected error"));
    }
}