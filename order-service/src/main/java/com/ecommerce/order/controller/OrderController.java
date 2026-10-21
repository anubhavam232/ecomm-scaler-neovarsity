package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.dto.OrderDtos.*;
import com.ecommerce.order.entity.CustomerOrder;
import com.ecommerce.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CustomerOrder>> checkout(@RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Order placed and payment processed via Kafka", orderService.checkout(request)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<CustomerOrder>> getOrderConfirmation(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok("Order confirmation retrieved", orderService.getOrderById(orderId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<CustomerOrder>>> getUserOrderHistory(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Order history retrieved", orderService.getOrderHistoryForUser(userId)));
    }

    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> trackOrder(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok("Order tracking retrieved", orderService.getOrderTracking(orderId)));
    }

    @PutMapping("/{orderId}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> updateOrderTracking(
            @PathVariable("orderId") Long orderId,
            @RequestBody UpdateTrackingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Order tracking status updated", orderService.updateOrderTrackingStatus(orderId, request)));
    }
}
