package com.ecommerce.payment.service;

import com.ecommerce.common.events.OrderPlacedEvent;
import com.ecommerce.common.events.PaymentCompletedEvent;
import com.ecommerce.payment.dto.PaymentDtos.ManualPaymentRequest;
import com.ecommerce.payment.dto.PaymentDtos.PaymentReceiptResponse;
import com.ecommerce.payment.entity.PaymentTransaction;
import com.ecommerce.payment.exception.InvalidPaymentException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.exception.PaymentProcessingException;
import com.ecommerce.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PaymentService.
 * Tests all business logic, validation, exception handling, and Kafka integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Tests")
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentTransaction sampleTransaction;
    private ManualPaymentRequest validPaymentRequest;

    @BeforeEach
    void setUp() {
        // Setup sample transaction
        sampleTransaction = new PaymentTransaction();
        sampleTransaction.setId(1L);
        sampleTransaction.setTransactionId("TXN-TEST123");
        sampleTransaction.setReceiptNumber("RCPT-1234567890-100");
        sampleTransaction.setOrderId(100L);
        sampleTransaction.setOrderNumber("ORD-100");
        sampleTransaction.setUserId(1L);
        sampleTransaction.setUserEmail("test@example.com");
        sampleTransaction.setAmount(99.99);
        sampleTransaction.setCurrency("USD");
        sampleTransaction.setPaymentMethod("CREDIT_CARD");
        sampleTransaction.setMaskedInstrument("****-****-****-4242");
        sampleTransaction.setGatewayProvider("Stripe PCI-DSS Vault Gateway");
        sampleTransaction.setStatus("SUCCESS");
        sampleTransaction.setCryptographicSignature("test-signature");

        // Setup valid payment request
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

    // ========== processOrderPayment Tests ==========

    @Test
    @DisplayName("Should successfully process order payment with valid data")
    void processOrderPayment_WithValidData_ShouldSucceed() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        PaymentTransaction result = paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, "CREDIT_CARD", "4242424242424242"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.getTransactionId()).isEqualTo("TXN-TEST123");
        verify(paymentRepository, times(1)).save(any(PaymentTransaction.class));
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(PaymentCompletedEvent.class));
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when orderId is null")
    void processOrderPayment_WithNullOrderId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                null, "ORD-100", 1L, "test@example.com", 99.99, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Order ID must be a positive number");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when orderId is negative")
    void processOrderPayment_WithNegativeOrderId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                -1L, "ORD-100", 1L, "test@example.com", 99.99, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Order ID must be a positive number");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when orderNumber is null")
    void processOrderPayment_WithNullOrderNumber_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                100L, null, 1L, "test@example.com", 99.99, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Order number is required");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when orderNumber is blank")
    void processOrderPayment_WithBlankOrderNumber_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                100L, "  ", 1L, "test@example.com", 99.99, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Order number is required");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when userId is null")
    void processOrderPayment_WithNullUserId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                100L, "ORD-100", null, "test@example.com", 99.99, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("User ID must be a positive number");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when userId is zero")
    void processOrderPayment_WithZeroUserId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                100L, "ORD-100", 0L, "test@example.com", 99.99, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("User ID must be a positive number");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when userEmail is null")
    void processOrderPayment_WithNullUserEmail_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                100L, "ORD-100", 1L, null, 99.99, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("User email is required");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when amount is zero")
    void processOrderPayment_WithZeroAmount_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 0.0, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when amount is negative")
    void processOrderPayment_WithNegativeAmount_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", -50.0, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when payment method is unsupported")
    void processOrderPayment_WithUnsupportedPaymentMethod_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, "BITCOIN", "btc-address"
        ))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Unsupported payment method: BITCOIN");
    }

    @Test
    @DisplayName("Should use default CREDIT_CARD when payment method is null")
    void processOrderPayment_WithNullPaymentMethod_ShouldUseDefault() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, null, "4242"
        );

        // Assert
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("CREDIT_CARD");
    }

    @Test
    @DisplayName("Should normalize payment method to uppercase")
    void processOrderPayment_WithLowercasePaymentMethod_ShouldNormalize() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, "upi", "test@upi"
        );

        // Assert
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("UPI");
    }

    @Test
    @DisplayName("Should throw PaymentProcessingException when database save fails")
    void processOrderPayment_WhenDatabaseSaveFails_ShouldThrowException() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenThrow(new DataAccessException("Database error") {});

        // Act & Assert
        assertThatThrownBy(() -> paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, "CREDIT_CARD", "4242"
        ))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Failed to save payment transaction");
    }

    @Test
    @DisplayName("Should continue when Kafka publish fails after successful save")
    void processOrderPayment_WhenKafkaFails_ShouldNotThrowException() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka error"));

        // Act & Assert - Should not throw exception
        assertThatCode(() -> paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, "CREDIT_CARD", "4242"
        )).doesNotThrowAnyException();

        verify(paymentRepository, times(1)).save(any());
    }

    // ========== processManualPayment Tests ==========

    @Test
    @DisplayName("Should successfully process manual payment with valid request")
    void processManualPayment_WithValidRequest_ShouldSucceed() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        PaymentReceiptResponse response = paymentService.processManualPayment(validPaymentRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.receiptNumber()).isEqualTo("RCPT-1234567890-100");
        assertThat(response.transactionId()).isEqualTo("TXN-TEST123");
        assertThat(response.orderId()).isEqualTo(100L);
        assertThat(response.amountPaid()).isEqualTo(99.99);
        verify(paymentRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when request is null")
    void processManualPayment_WithNullRequest_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.processManualPayment(null))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Payment request cannot be null");
    }

    @Test
    @DisplayName("Should rethrow InvalidPaymentException from processOrderPayment")
    void processManualPayment_WithInvalidData_ShouldRethrowInvalidPaymentException() {
        // Arrange
        ManualPaymentRequest invalidRequest = new ManualPaymentRequest(
                null, "ORD-100", 1L, "test@example.com", 99.99, "CREDIT_CARD", "4242"
        );

        // Act & Assert
        assertThatThrownBy(() -> paymentService.processManualPayment(invalidRequest))
                .isInstanceOf(InvalidPaymentException.class);
    }

    // ========== getReceiptByOrderId Tests ==========

    @Test
    @DisplayName("Should successfully retrieve payment receipt by order ID")
    void getReceiptByOrderId_WithValidOrderId_ShouldReturnReceipt() {
        // Arrange
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(sampleTransaction));

        // Act
        PaymentReceiptResponse response = paymentService.getReceiptByOrderId(100L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(100L);
        assertThat(response.receiptNumber()).isEqualTo("RCPT-1234567890-100");
        verify(paymentRepository, times(1)).findByOrderId(100L);
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when payment not found")
    void getReceiptByOrderId_WhenPaymentNotFound_ShouldThrowException() {
        // Arrange
        when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getReceiptByOrderId(999L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found for order ID: 999");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when orderId is null")
    void getReceiptByOrderId_WithNullOrderId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.getReceiptByOrderId(null))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Order ID must be a positive number");
    }

    @Test
    @DisplayName("Should throw InvalidPaymentException when orderId is zero")
    void getReceiptByOrderId_WithZeroOrderId_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> paymentService.getReceiptByOrderId(0L))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Order ID must be a positive number");
    }

    // ========== getSupportedPaymentMethods Tests ==========

    @Test
    @DisplayName("Should return list of supported payment methods")
    void getSupportedPaymentMethods_ShouldReturnAllMethods() {
        // Act
        List<String> methods = paymentService.getSupportedPaymentMethods();

        // Assert
        assertThat(methods).isNotNull();
        assertThat(methods).hasSize(5);
        assertThat(methods).containsExactlyInAnyOrder(
                "CREDIT_CARD", "DEBIT_CARD", "ONLINE_BANKING", "UPI", "WALLET"
        );
    }

    // ========== onOrderPlaced (Kafka Listener) Tests ==========

    @Test
    @DisplayName("Should successfully process OrderPlacedEvent from Kafka")
    void onOrderPlaced_WithValidEvent_ShouldProcessPayment() {
        // Arrange
        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-123", 100L, "ORD-100", 1L, "test@example.com",
                99.99, "CREDIT_CARD", "4242424242424242",
                "GK 16", Instant.now().toString()
        );
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act & Assert - Should not throw exception
        assertThatCode(() -> paymentService.onOrderPlaced(event)).doesNotThrowAnyException();
        verify(paymentRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw PaymentProcessingException when OrderPlacedEvent processing fails")
    void onOrderPlaced_WhenProcessingFails_ShouldThrowException() {
        // Arrange
        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-123", 100L, "ORD-100", 1L, "test@example.com",
                99.99, "CREDIT_CARD", "4242424242424242",
                "GK 16", Instant.now().toString()
        );
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.onOrderPlaced(event))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Failed to process payment for order: ORD-100");
    }

    // ========== Payment Method Specific Tests ==========

    @Test
    @DisplayName("Should process DEBIT_CARD payment successfully")
    void processOrderPayment_WithDebitCard_ShouldSucceed() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, "DEBIT_CARD", "4242424242424242"
        );

        // Assert
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("DEBIT_CARD");
    }

    @Test
    @DisplayName("Should process UPI payment successfully")
    void processOrderPayment_WithUPI_ShouldSucceed() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, "UPI", "user@upi"
        );

        // Assert
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("UPI");
    }

    @Test
    @DisplayName("Should process WALLET payment successfully")
    void processOrderPayment_WithWallet_ShouldSucceed() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, "WALLET", "wallet-id"
        );

        // Assert
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("WALLET");
    }

    @Test
    @DisplayName("Should process ONLINE_BANKING payment successfully")
    void processOrderPayment_WithOnlineBanking_ShouldSucceed() {
        // Arrange
        when(paymentRepository.save(any(PaymentTransaction.class))).thenReturn(sampleTransaction);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        paymentService.processOrderPayment(
                100L, "ORD-100", 1L, "test@example.com", 99.99, "ONLINE_BANKING", "account123"
        );

        // Assert
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("ONLINE_BANKING");
    }
}