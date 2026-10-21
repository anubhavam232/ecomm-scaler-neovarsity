package com.ecommerce.order.service;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.common.events.OrderPlacedEvent;
import com.ecommerce.common.events.OrderStatusUpdatedEvent;
import com.ecommerce.common.events.PaymentCompletedEvent;
import com.ecommerce.order.dto.OrderDtos.*;
import com.ecommerce.order.entity.CustomerOrder;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private CheckoutRequest validCheckoutRequest;
    private CustomerOrder savedOrder;

    @BeforeEach
    void setUp() {
        // Setup valid checkout request
        List<CheckoutItemRequest> items = new ArrayList<>();
        items.add(new CheckoutItemRequest(1L, "SKU-001", "Test Product", 100.0, 2));
        items.add(new CheckoutItemRequest(2L, "SKU-002", "Test Product 2", 50.0, 1));

        validCheckoutRequest = new CheckoutRequest(
                1L,
                "test@example.com",
                "123 Test Street, Test City",
                "CREDIT_CARD",
                "4111111111111111",
                items
        );

        // Setup saved order mock
        savedOrder = new CustomerOrder();
        savedOrder.setId(1L);
        savedOrder.setOrderNumber("ORD-123456");
        savedOrder.setUserId(1L);
        savedOrder.setUserEmail("test@example.com");
        savedOrder.setDeliveryAddress("123 Test Street, Test City");
        savedOrder.setPaymentMethod("CREDIT_CARD");
        savedOrder.setPaymentStatus("PENDING");
        savedOrder.setOrderStatus("PAYMENT_PENDING");
        savedOrder.setSubtotal(250.0);
        savedOrder.setTaxAmount(20.0);
        savedOrder.setShippingFee(15.0);
        savedOrder.setTotalAmount(285.0);
    }

    @Test
    @DisplayName("Should successfully checkout with valid items")
    void testCheckout_Success() {
        // Arrange
        when(orderRepository.save(any(CustomerOrder.class))).thenReturn(savedOrder);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // Act
        CustomerOrder result = orderService.checkout(validCheckoutRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getUserEmail()).isEqualTo("test@example.com");
        assertThat(result.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(result.getOrderStatus()).isEqualTo("PAYMENT_PENDING");

        // Verify repository interaction
        verify(orderRepository, times(1)).save(any(CustomerOrder.class));
        verify(orderRepository, times(1)).findById(1L);

        // Verify Kafka event published
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq(KafkaTopics.ORDER_EVENTS), anyString(), eventCaptor.capture());

        OrderPlacedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.orderId()).isEqualTo(1L);
        assertThat(publishedEvent.userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when checkout with null items")
    void testCheckout_NullItems() {
        // Arrange
        CheckoutRequest invalidRequest = new CheckoutRequest(
                1L, "test@example.com", "123 Test Street",
                "CREDIT_CARD", "4111111111111111", null
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.checkout(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot checkout with an empty item list");

        verify(orderRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when checkout with empty items")
    void testCheckout_EmptyItems() {
        // Arrange
        CheckoutRequest invalidRequest = new CheckoutRequest(
                1L, "test@example.com", "123 Test Street",
                "CREDIT_CARD", "4111111111111111", new ArrayList<>()
        );

        // Act & Assert
        assertThatThrownBy(() -> orderService.checkout(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot checkout with an empty item list");
    }

    @Test
    @DisplayName("Should calculate subtotal correctly with multiple items")
    void testCheckout_CalculatesSubtotalCorrectly() {
        // Arrange
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderRepository.findById(1L)).thenAnswer(invocation -> {
            CustomerOrder order = new CustomerOrder();
            order.setId(1L);
            return Optional.of(order);
        });

        // Act
        orderService.checkout(validCheckoutRequest);

        // Assert
        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(orderCaptor.capture());

        CustomerOrder capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getSubtotal()).isEqualTo(250.0); // (100*2) + (50*1)
        assertThat(capturedOrder.getTaxAmount()).isEqualTo(20.0); // 250 * 0.08
        assertThat(capturedOrder.getShippingFee()).isEqualTo(15.0); // subtotal < 500
    }

    @Test
    @DisplayName("Should apply free shipping when subtotal >= 500")
    void testCheckout_FreeShippingApplied() {
        // Arrange
        List<CheckoutItemRequest> items = new ArrayList<>();
        items.add(new CheckoutItemRequest(1L, "SKU-001", "Expensive Product", 500.0, 1));

        CheckoutRequest request = new CheckoutRequest(
                1L, "test@example.com", "123 Test Street",
                "CREDIT_CARD", "4111111111111111", items
        );

        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderRepository.findById(1L)).thenReturn(Optional.of(new CustomerOrder()));

        // Act
        orderService.checkout(request);

        // Assert
        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(orderCaptor.capture());

        CustomerOrder capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getShippingFee()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should process payment completed event successfully")
    void testOnPaymentCompleted_Success() {
        // Arrange
        savedOrder.setId(1L);
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "evt-123", 1L, "ORD-123456", 1L, "test@test.com",
                "TXN-789",
                "RCP-456", 285.0, "paypal",
                "SUCCESS", "hash", Instant.now().toString()
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(CustomerOrder.class))).thenReturn(savedOrder);

        // Act
        orderService.onPaymentCompleted(event);

        // Assert
        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(orderCaptor.capture());

        CustomerOrder updatedOrder = orderCaptor.getValue();
        assertThat(updatedOrder.getPaymentTransactionId()).isEqualTo("TXN-789");
        assertThat(updatedOrder.getPaymentReceiptNumber()).isEqualTo("RCP-456");
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(updatedOrder.getOrderStatus()).isEqualTo("CONFIRMED");

        // Verify status update event published
        verify(kafkaTemplate, times(1)).send(eq(KafkaTopics.ORDER_EVENTS), anyString(), any(OrderStatusUpdatedEvent.class));
    }

    @Test
    @DisplayName("Should handle payment failed event")
    void testOnPaymentCompleted_Failed() {
        // Arrange
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "evt-123", 1L, "ORD-123456", 1L, "test@test.com",
                "TXN-789",
                null, 285.0,"paypal","FAILED", "hash",
                Instant.now().toString()
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(CustomerOrder.class))).thenReturn(savedOrder);

        // Act
        orderService.onPaymentCompleted(event);

        // Assert
        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(orderCaptor.capture());

        CustomerOrder updatedOrder = orderCaptor.getValue();
        assertThat(updatedOrder.getPaymentStatus()).isEqualTo("FAILED");
        assertThat(updatedOrder.getOrderStatus()).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    @DisplayName("Should not process payment event when order not found")
    void testOnPaymentCompleted_OrderNotFound() {
        // Arrange
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                "evt-123", 999L, "ORD-123456", 1L, "test@test.com",
                "TXN-789",
                "RCP-456", 285.0,"paypal","SUCCESS", "hash",
                Instant.now().toString()
        );

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        orderService.onPaymentCompleted(event);

        // Assert
        verify(orderRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should get order by id successfully")
    void testGetOrderById_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        // Act
        CustomerOrder result = orderService.getOrderById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void testGetOrderById_NotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with ID: 999");

        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should get user order history")
    void testGetOrderHistoryForUser_Success() {
        // Arrange
        List<CustomerOrder> orders = new ArrayList<>();
        orders.add(savedOrder);
        orders.add(savedOrder);

        when(orderRepository.findByUserId(1L)).thenReturn(orders);

        // Act
        List<CustomerOrder> result = orderService.getOrderHistoryForUser(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(orderRepository, times(1)).findByUserId(1L);
    }

    @Test
    @DisplayName("Should return empty list when user has no orders")
    void testGetOrderHistoryForUser_EmptyList() {
        // Arrange
        when(orderRepository.findByUserId(999L)).thenReturn(new ArrayList<>());

        // Act
        List<CustomerOrder> result = orderService.getOrderHistoryForUser(999L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get order tracking successfully")
    void testGetOrderTracking_Success() {
        // Arrange
        savedOrder.setTrackingNumber("TRK-12345678");
        savedOrder.setCarrierName("BlueDart Express Logistics");
        savedOrder.getTrackingHistory().add("2024-01-01 | ORDER_INITIATED | Order created");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        // Act
        OrderTrackingResponse result = orderService.getOrderTracking(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.trackingNumber()).isEqualTo("TRK-12345678");
        assertThat(result.carrierName()).isEqualTo("BlueDart Express Logistics");
        assertThat(result.trackingHistory()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when tracking non-existent order")
    void testGetOrderTracking_OrderNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderTracking(999L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Should update order tracking status successfully")
    void testUpdateOrderTrackingStatus_Success() {
        // Arrange
        UpdateTrackingRequest request = new UpdateTrackingRequest(
                "SHIPPED",
                "Package dispatched from warehouse"
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(CustomerOrder.class))).thenReturn(savedOrder);

        // Act
        OrderTrackingResponse result = orderService.updateOrderTrackingStatus(1L, request);

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(orderCaptor.capture());

        CustomerOrder updatedOrder = orderCaptor.getValue();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo("SHIPPED");
        assertThat(updatedOrder.getTrackingHistory()).anyMatch(entry -> entry.contains("SHIPPED"));
        assertThat(updatedOrder.getTrackingHistory()).anyMatch(entry -> entry.contains("Package dispatched from warehouse"));

        // Verify status update event published
        verify(kafkaTemplate, times(1)).send(eq(KafkaTopics.ORDER_EVENTS), anyString(), any(OrderStatusUpdatedEvent.class));
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when updating tracking for non-existent order")
    void testUpdateOrderTrackingStatus_OrderNotFound() {
        // Arrange
        UpdateTrackingRequest request = new UpdateTrackingRequest("SHIPPED", "Test note");
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.updateOrderTrackingStatus(999L, request))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }
}
