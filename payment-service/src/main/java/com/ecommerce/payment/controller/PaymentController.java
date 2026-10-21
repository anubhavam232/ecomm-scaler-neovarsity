package com.ecommerce.payment.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.payment.dto.PaymentDtos.ManualPaymentRequest;
import com.ecommerce.payment.dto.PaymentDtos.PaymentReceiptResponse;
import com.ecommerce.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/methods")
    public ResponseEntity<ApiResponse<List<String>>> getSupportedMethods() {
        return ResponseEntity.ok(ApiResponse.ok("Supported payment methods retrieved", paymentService.getSupportedPaymentMethods()));
    }

    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentReceiptResponse>> processPayment(@RequestBody ManualPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Payment processed securely", paymentService.processManualPayment(request)));
    }

    @GetMapping("/receipt/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentReceiptResponse>> getReceiptByOrder(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(ApiResponse.ok("Payment receipt retrieved", paymentService.getReceiptByOrderId(orderId)));
    }
}
