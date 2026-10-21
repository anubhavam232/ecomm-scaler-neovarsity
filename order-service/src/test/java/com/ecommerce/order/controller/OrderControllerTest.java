package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.dto.OrderDtos.*;
import com.ecommerce.order.entity.CustomerOrder;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController REST API Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private CustomerOrder testOrder;
    private CheckoutRequest checkoutRequest;
    private OrderTrackingResponse trackingResponse;

    @BeforeEach
    void setUp() {
        // Setup test order
        testOrder = new CustomerOrder();
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD-123456");
        testOrder.setUserId(1L);
        testOrder.setUserEmail("test@example.com");
        testOrder.setDeliveryAddress("123 Test Street, Test City");
        testOrder.setPaymentMethod("CREDIT_CARD");
        testOrder.setPaymentStatus("PENDING");
        testOrder.setOrderStatus("PAYMENT_PENDING");
        testOrder.setSubtotal(250.0);
        testOrder.setTaxAmount(20.0);
        testOrder.setShippingFee(15.0);
        testOrder.setTotalAmount(285.0);

        // Setup checkout request
        List<CheckoutItemRequest> items = new ArrayList<>();
        items.add(new CheckoutItemRequest(1L, "SKU-001", "Test Product", 100.0, 2));
        items.add(new CheckoutItemRequest(2L, "SKU-002", "Test Product 2", 50.0, 1));

        checkoutRequest = new CheckoutRequest(
                1L,
                "test@example.com",
                "123 Test Street, Test City",
                "CREDIT_CARD",
                "4111111111111111",
                items
        );

        // Setup tracking response
        List<String> history = new ArrayList<>();
        history.add("2024-01-01 | ORDER_INITIATED | Order created");
        history.add("2024-01-02 | CONFIRMED | Payment confirmed");

        trackingResponse = new OrderTrackingResponse(
                1L,
                "ORD-123456",
                "CONFIRMED",
                "SUCCESS",
                "TRK-12345678",
                "BlueDart Express Logistics",
                "123 Test Street, Test City",
                "2024-01-10",
                history
        );
    }

    @Test
    @DisplayName("POST /api/v1/orders/checkout - Should checkout successfully")
    void testCheckout_Success() throws Exception {
        // Arrange
        when(orderService.checkout(any(CheckoutRequest.class))).thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order placed and payment processed via Kafka"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-123456"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.userEmail").value("test@example.com"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.orderStatus").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(285.0));

        verify(orderService, times(1)).checkout(any(CheckoutRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/orders/checkout - Should handle invalid request body")
    void testCheckout_InvalidRequestBody() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).checkout(any());
    }

    @Test
    @DisplayName("POST /api/v1/orders/checkout - Should handle empty items list")
    void testCheckout_EmptyItems() throws Exception {
        // Arrange
        when(orderService.checkout(any(CheckoutRequest.class)))
                .thenThrow(new IllegalArgumentException("Cannot checkout with an empty item list"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Cannot checkout with an empty item list"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Should get order confirmation successfully")
    void testGetOrderConfirmation_Success() throws Exception {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order confirmation retrieved"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-123456"))
                .andExpect(jsonPath("$.data.totalAmount").value(285.0));

        verify(orderService, times(1)).getOrderById(1L);
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Should return 404 when order not found")
    void testGetOrderConfirmation_NotFound() throws Exception {
        // Arrange
        when(orderService.getOrderById(999L))
                .thenThrow(new OrderNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order not found with ID: 999"));

        verify(orderService, times(1)).getOrderById(999L);
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Should handle invalid orderId type")
    void testGetOrderConfirmation_InvalidOrderIdType() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"));

        verify(orderService, never()).getOrderById(any());
    }

    @Test
    @DisplayName("GET /api/v1/orders/user/{userId} - Should get user order history successfully")
    void testGetUserOrderHistory_Success() throws Exception {
        // Arrange
        List<CustomerOrder> orders = new ArrayList<>();
        orders.add(testOrder);
        
        CustomerOrder order2 = new CustomerOrder();
        order2.setId(2L);
        order2.setOrderNumber("ORD-789012");
        order2.setUserId(1L);
        orders.add(order2);

        when(orderService.getOrderHistoryForUser(1L)).thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/user/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order history retrieved"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[1].id").value(2));

        verify(orderService, times(1)).getOrderHistoryForUser(1L);
    }

    @Test
    @DisplayName("GET /api/v1/orders/user/{userId} - Should return empty list when user has no orders")
    void testGetUserOrderHistory_EmptyList() throws Exception {
        // Arrange
        when(orderService.getOrderHistoryForUser(999L)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/user/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(orderService, times(1)).getOrderHistoryForUser(999L);
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId}/tracking - Should get order tracking successfully")
    void testTrackOrder_Success() throws Exception {
        // Arrange
        when(orderService.getOrderTracking(1L)).thenReturn(trackingResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/1/tracking")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order tracking retrieved"))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-123456"))
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.trackingNumber").value("TRK-12345678"))
                .andExpect(jsonPath("$.data.carrierName").value("BlueDart Express Logistics"))
                .andExpect(jsonPath("$.data.trackingHistory", hasSize(2)));

        verify(orderService, times(1)).getOrderTracking(1L);
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId}/tracking - Should return 404 when order not found")
    void testTrackOrder_NotFound() throws Exception {
        // Arrange
        when(orderService.getOrderTracking(999L))
                .thenThrow(new OrderNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(get("/api/v1/orders/999/tracking")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));

        verify(orderService, times(1)).getOrderTracking(999L);
    }

    @Test
    @DisplayName("PUT /api/v1/orders/{orderId}/tracking - Should update order tracking successfully")
    void testUpdateOrderTracking_Success() throws Exception {
        // Arrange
        UpdateTrackingRequest request = new UpdateTrackingRequest(
                "SHIPPED",
                "Package dispatched from warehouse"
        );

        OrderTrackingResponse updatedResponse = new OrderTrackingResponse(
                1L,
                "ORD-123456",
                "SHIPPED",
                "SUCCESS",
                "TRK-12345678",
                "BlueDart Express Logistics",
                "123 Test Street, Test City",
                "2024-01-10",
                List.of("2024-01-01 | ORDER_INITIATED | Order created",
                        "2024-01-02 | CONFIRMED | Payment confirmed",
                        "2024-01-03 | SHIPPED | Package dispatched from warehouse")
        );

        when(orderService.updateOrderTrackingStatus(eq(1L), any(UpdateTrackingRequest.class)))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/orders/1/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order tracking status updated"))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.orderStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.data.trackingHistory", hasSize(3)))
                .andExpect(jsonPath("$.data.trackingHistory[2]").value(containsString("SHIPPED")));

        verify(orderService, times(1)).updateOrderTrackingStatus(eq(1L), any(UpdateTrackingRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/orders/{orderId}/tracking - Should return 404 when order not found")
    void testUpdateOrderTracking_NotFound() throws Exception {
        // Arrange
        UpdateTrackingRequest request = new UpdateTrackingRequest("SHIPPED", "Test note");

        when(orderService.updateOrderTrackingStatus(eq(999L), any(UpdateTrackingRequest.class)))
                .thenThrow(new OrderNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(put("/api/v1/orders/999/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));

        verify(orderService, times(1)).updateOrderTrackingStatus(eq(999L), any(UpdateTrackingRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/orders/{orderId}/tracking - Should handle invalid request body")
    void testUpdateOrderTracking_InvalidRequestBody() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/orders/1/tracking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"));

        verify(orderService, never()).updateOrderTrackingStatus(any(), any());
    }
}