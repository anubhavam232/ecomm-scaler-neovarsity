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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderService(OrderRepository orderRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // PRD 3.3 Checkout + HLD Part 3 Flow: Create Order in MySQL -> Publish OrderPlacedEvent to Kafka
    public CustomerOrder checkout(CheckoutRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout with an empty item list");
        }
        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber("ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        order.setUserId(req.userId());
        order.setUserEmail(req.userEmail());
        order.setDeliveryAddress(req.deliveryAddress());
        order.setPaymentMethod(req.paymentMethod().toUpperCase());
        order.setPaymentStatus("PENDING");
        order.setOrderStatus("PAYMENT_PENDING");
        order.setTrackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCarrierName("BlueDart Express Logistics");
        order.setEstimatedDeliveryDate(Instant.now().plus(3, ChronoUnit.DAYS).toString().substring(0, 10));

        double subtotal = 0.0;
        for (CheckoutItemRequest itemReq : req.items()) {
            OrderItem item = new OrderItem(
                    itemReq.productId(),
                    itemReq.sku(),
                    itemReq.productName(),
                    itemReq.unitPrice(),
                    itemReq.quantity()
            );
            order.getItems().add(item);
            subtotal += item.getLineTotal();
        }
        subtotal = Math.round(subtotal * 100.0) / 100.0;
        double tax = Math.round(subtotal * 0.08 * 100.0) / 100.0;
        double shipping = subtotal >= 500.0 ? 0.0 : 15.0;
        double total = Math.round((subtotal + tax + shipping) * 100.0) / 100.0;

        order.setSubtotal(subtotal);
        order.setTaxAmount(tax);
        order.setShippingFee(shipping);
        order.setTotalAmount(total);
        order.getTrackingHistory().add(Instant.now() + " | ORDER_INITIATED | Checkout submitted; awaiting payment confirmation");

        CustomerOrder saved = orderRepository.save(order);

        // HLD Typical Flow Part 3: "After placing the order, a message is sent to Kafka. Payment Service consumes the Kafka message"
        OrderPlacedEvent placedEvent = new OrderPlacedEvent(
                UUID.randomUUID().toString(),
                saved.getId(),
                saved.getOrderNumber(),
                saved.getUserId(),
                saved.getUserEmail(),
                saved.getTotalAmount(),
                saved.getPaymentMethod(),
                req.paymentInstrumentDetails(),
                saved.getDeliveryAddress(),
                Instant.now().toString()
        );
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, saved.getOrderNumber(), placedEvent);

        return orderRepository.findById(saved.getId()).orElse(saved);
    }

    // HLD 3.4 & 3.5: Consume PaymentCompletedEvent from Payment Service via Kafka
    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "order-service-group")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.setPaymentTransactionId(event.transactionId());
            order.setPaymentReceiptNumber(event.receiptNumber());
            order.setPaymentStatus(event.status());
            if ("SUCCESS".equalsIgnoreCase(event.status())) {
                order.setOrderStatus("CONFIRMED");
                order.getTrackingHistory().add(
                        Instant.now() + " | CONFIRMED | Payment verified (Txn: " + event.transactionId() + ", Receipt: " + event.receiptNumber() + ") - Order Confirmed & Queued for Warehouse Dispatch"
                );
            } else {
                order.setOrderStatus("PAYMENT_FAILED");
                order.getTrackingHistory().add(Instant.now() + " | PAYMENT_FAILED | Payment authorization declined");
            }
            order.setUpdatedAt(Instant.now().toString());
            CustomerOrder updated = orderRepository.save(order);

            // Publish OrderStatusUpdatedEvent so Notification Service sends Order Confirmation to the customer
            OrderStatusUpdatedEvent statusEvent = new OrderStatusUpdatedEvent(
                    UUID.randomUUID().toString(),
                    updated.getId(),
                    updated.getOrderNumber(),
                    updated.getUserId(),
                    updated.getUserEmail(),
                    updated.getOrderStatus(),
                    updated.getTrackingNumber(),
                    "Order " + updated.getOrderNumber() + " is now " + updated.getOrderStatus(),
                    Instant.now().toString()
            );
            kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, updated.getOrderNumber(), statusEvent);
        });
    }

    // PRD 4.1 Order Confirmation Details
    public CustomerOrder getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    // PRD 4.2 Order History
    public List<CustomerOrder> getOrderHistoryForUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    // PRD 4.3 Order Tracking
    public OrderTrackingResponse getOrderTracking(Long orderId) {
        CustomerOrder order = getOrderById(orderId);
        return new OrderTrackingResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                order.getTrackingNumber(),
                order.getCarrierName(),
                order.getDeliveryAddress(),
                order.getEstimatedDeliveryDate(),
                order.getTrackingHistory()
        );
    }

    public OrderTrackingResponse updateOrderTrackingStatus(Long orderId, UpdateTrackingRequest req) {
        CustomerOrder order = getOrderById(orderId);
        order.setOrderStatus(req.status().toUpperCase());
        order.getTrackingHistory().add(Instant.now() + " | " + req.status().toUpperCase() + " | " + req.locationNote());
        order.setUpdatedAt(Instant.now().toString());
        CustomerOrder saved = orderRepository.save(order);

        OrderStatusUpdatedEvent statusEvent = new OrderStatusUpdatedEvent(
                UUID.randomUUID().toString(),
                saved.getId(),
                saved.getOrderNumber(),
                saved.getUserId(),
                saved.getUserEmail(),
                saved.getOrderStatus(),
                saved.getTrackingNumber(),
                req.locationNote(),
                Instant.now().toString()
        );
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, saved.getOrderNumber(), statusEvent);

        return getOrderTracking(orderId);
    }
}
