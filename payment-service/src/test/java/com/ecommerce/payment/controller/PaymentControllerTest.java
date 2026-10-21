package com.ecommerce.payment.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.payment.dto.PaymentDtos.ManualPaymentRequest;
import com.ecommerce.payment.dto.PaymentDtos.PaymentReceiptResponse;
import com.ecommerce.payment.exception.InvalidPaymentException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PaymentController.
 * Tests REST endpoints, request/response handling, and exception scenarios.
 */
@WebMvcTest(PaymentController.class)
@DisplayName("Payment Controller Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentReceiptResponse sampleReceipt;
    private ManualPaymentRequest validPaymentRequest;

    @BeforeEach
    void setUp() {
        sampleReceipt = new PaymentReceiptResponse(
                "RCPT-1234567890-100",
                "TXN-TEST123",
                100L,
                "ORD-100",
                1L,
                "test@example.com",
                99.99,
                "USD",
                "CREDIT_CARD",
                "****-****-****-4242",
                "Stripe PCI-DSS Vault Gateway",
                "SUCCESS",
                "test-signature",
                "2026-10-21T10:00:00Z"
        );

        validPaymentRequest = new ManualPaymentRequest(
                100L,
                "ORD-100",
                1L,
                "test@example.com",
                99.99,
                "CREDIT_CARD",
                "4242424242424242"
        );
    }

    // ========== GET /api/v1/payments/methods Tests ==========

    @Test
    @DisplayName("GET /api/v1/payments/methods - Should return supported payment methods")
    void getSupportedMethods_ShouldReturnListOfMethods() throws Exception {
        // Arrange
        List<String> methods = List.of("CREDIT_CARD", "DEBIT_CARD", "ONLINE_BANKING", "UPI", "WALLET");
        when(paymentService.getSupportedPaymentMethods()).thenReturn(methods);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/methods")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Supported payment methods retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0]").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.data[1]").value("DEBIT_CARD"))
                .andExpect(jsonPath("$.data[2]").value("ONLINE_BANKING"))
                .andExpect(jsonPath("$.data[3]").value("UPI"))
                .andExpect(jsonPath("$.data[4]").value("WALLET"));
    }

    // ========== POST /api/v1/payments/process Tests ==========

    @Test
    @DisplayName("POST /api/v1/payments/process - Should process payment successfully")
    void processPayment_WithValidRequest_ShouldReturnReceipt() throws Exception {
        // Arrange
        when(paymentService.processManualPayment(any(ManualPaymentRequest.class)))
                .thenReturn(sampleReceipt);

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment processed securely"))
                .andExpect(jsonPath("$.data.receiptNumber").value("RCPT-1234567890-100"))
                .andExpect(jsonPath("$.data.transactionId").value("TXN-TEST123"))
                .andExpect(jsonPath("$.data.orderId").value(100))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-100"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.userEmail").value("test@example.com"))
                .andExpect(jsonPath("$.data.amountPaid").value(99.99))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.data.maskedInstrument").value("****-****-****-4242"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/process - Should return 400 when request is invalid")
    void processPayment_WithInvalidRequest_ShouldReturn400() throws Exception {
        // Arrange
        when(paymentService.processManualPayment(any(ManualPaymentRequest.class)))
                .thenThrow(new InvalidPaymentException("Payment amount must be greater than zero"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPaymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_PAYMENT"))
                .andExpect(jsonPath("$.message").value("Payment amount must be greater than zero"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/process - Should return 400 when JSON is malformed")
    void processPayment_WithMalformedJson_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"));
    }

    // ========== GET /api/v1/payments/receipt/order/{orderId} Tests ==========

    @Test
    @DisplayName("GET /api/v1/payments/receipt/order/{orderId} - Should return payment receipt")
    void getReceiptByOrder_WithValidOrderId_ShouldReturnReceipt() throws Exception {
        // Arrange
        when(paymentService.getReceiptByOrderId(eq(100L))).thenReturn(sampleReceipt);

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment receipt retrieved"))
                .andExpect(jsonPath("$.data.receiptNumber").value("RCPT-1234567890-100"))
                .andExpect(jsonPath("$.data.orderId").value(100))
                .andExpect(jsonPath("$.data.transactionId").value("TXN-TEST123"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/receipt/order/{orderId} - Should return 404 when payment not found")
    void getReceiptByOrder_WhenPaymentNotFound_ShouldReturn404() throws Exception {
        // Arrange
        when(paymentService.getReceiptByOrderId(eq(999L)))
                .thenThrow(new PaymentNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("PAYMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Payment not found for order ID: 999"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/receipt/order/{orderId} - Should return 400 when orderId is invalid type")
    void getReceiptByOrder_WithInvalidOrderIdType_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/abc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/receipt/order/{orderId} - Should return 400 when orderId is zero")
    void getReceiptByOrder_WithZeroOrderId_ShouldReturn400() throws Exception {
        // Arrange
        when(paymentService.getReceiptByOrderId(eq(0L)))
                .thenThrow(new InvalidPaymentException("Order ID must be a positive number"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/payments/receipt/order/0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_PAYMENT"));
    }
}