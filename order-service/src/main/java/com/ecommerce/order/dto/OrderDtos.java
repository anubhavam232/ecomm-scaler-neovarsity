package com.ecommerce.order.dto;

import java.util.List;

public class OrderDtos {
    public record CheckoutItemRequest(
            Long productId,
            String sku,
            String productName,
            double unitPrice,
            int quantity
    ) {}

    public record CheckoutRequest(
            Long userId,
            String userEmail,
            String deliveryAddress,
            String paymentMethod,
            String paymentInstrumentDetails,
            List<CheckoutItemRequest> items
    ) {}

    public record UpdateTrackingRequest(
            String status,
            String locationNote
    ) {}

    public record OrderTrackingResponse(
            Long orderId,
            String orderNumber,
            String orderStatus,
            String paymentStatus,
            String trackingNumber,
            String carrierName,
            String deliveryAddress,
            String estimatedDeliveryDate,
            List<String> trackingHistory
    ) {}
}
