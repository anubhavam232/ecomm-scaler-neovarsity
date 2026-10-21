package com.ecommerce.payment.service;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.common.events.OrderPlacedEvent;
import com.ecommerce.common.events.PaymentCompletedEvent;
import com.ecommerce.common.security.JwtTokenProvider;
import com.ecommerce.payment.dto.PaymentDtos.ManualPaymentRequest;
import com.ecommerce.payment.dto.PaymentDtos.PaymentReceiptResponse;
import com.ecommerce.payment.entity.PaymentTransaction;
import com.ecommerce.payment.repository.PaymentTransactionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Set<String> SUPPORTED_METHODS = Set.of(
            "CREDIT_CARD", "DEBIT_CARD", "ONLINE_BANKING", "NET_BANKING", "UPI", "WALLET"
    );

    private final PaymentTransactionRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentService(PaymentTransactionRepository paymentRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // HLD 3.5 & Typical Flow Part 3: Consumes OrderPlacedEvent from Kafka to process payment automatically
    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "payment-service-group")
    public void onOrderPlaced(OrderPlacedEvent event) {
        processOrderPayment(
                event.orderId(),
                event.orderNumber(),
                event.userId(),
                event.userEmail(),
                event.totalAmount(),
                event.paymentMethod(),
                event.paymentInstrumentDetails()
        );
    }

    // PRD 5.1 Multiple Payment Options & 5.2 Secure Transactions
    public PaymentTransaction processOrderPayment(
            Long orderId,
            String orderNumber,
            Long userId,
            String userEmail,
            double amount,
            String paymentMethod,
            String instrumentDetails
    ) {
        String normalizedMethod = paymentMethod == null ? "CREDIT_CARD" : paymentMethod.toUpperCase();
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String receiptNo = "RCPT-" + System.currentTimeMillis() + "-" + orderId;

        boolean validMethod = SUPPORTED_METHODS.contains(normalizedMethod) && amount > 0;
        String status = validMethod ? "SUCCESS" : "FAILED";
        String masked = maskPaymentInstrument(normalizedMethod, instrumentDetails);
        String gateway = resolveGateway(normalizedMethod);
        String signature = JwtTokenProvider.hashPassword(
                txnId + "|" + orderNumber + "|" + amount + "|" + status,
                "PCI_DSS_HMAC_SALT_2026"
        );

        PaymentTransaction txn = new PaymentTransaction();
        txn.setTransactionId(txnId);
        txn.setReceiptNumber(receiptNo);
        txn.setOrderId(orderId);
        txn.setOrderNumber(orderNumber);
        txn.setUserId(userId);
        txn.setUserEmail(userEmail);
        txn.setAmount(amount);
        txn.setPaymentMethod(normalizedMethod);
        txn.setMaskedInstrument(masked);
        txn.setGatewayProvider(gateway);
        txn.setStatus(status);
        txn.setCryptographicSignature(signature);
        txn.setProcessedAt(Instant.now().toString());

        PaymentTransaction saved = paymentRepository.save(txn);

        // HLD 3.5: "Once the payment is confirmed, it produces a message on Kafka to notify the Order Management Service."
        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                UUID.randomUUID().toString(),
                saved.getOrderId(),
                saved.getOrderNumber(),
                saved.getUserId(),
                saved.getUserEmail(),
                saved.getTransactionId(),
                saved.getReceiptNumber(),
                saved.getAmount(),
                saved.getPaymentMethod(),
                saved.getStatus(),
                saved.getCryptographicSignature(),
                saved.getProcessedAt()
        );
        kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, saved.getOrderNumber(), completedEvent);

        return saved;
    }

    public PaymentReceiptResponse processManualPayment(ManualPaymentRequest req) {
        PaymentTransaction txn = processOrderPayment(
                req.orderId(),
                req.orderNumber(),
                req.userId(),
                req.userEmail(),
                req.amount(),
                req.paymentMethod(),
                req.instrumentDetails()
        );
        return toReceipt(txn);
    }

    // PRD 5.3 Payment Receipt by Order ID
    public PaymentReceiptResponse getReceiptByOrderId(Long orderId) {
        PaymentTransaction txn = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No payment receipt found for orderId: " + orderId));
        return toReceipt(txn);
    }

    public List<String> getSupportedPaymentMethods() {
        return List.of("CREDIT_CARD", "DEBIT_CARD", "ONLINE_BANKING", "UPI", "WALLET");
    }

    private static String maskPaymentInstrument(String method, String details) {
        if (details == null || details.isBlank()) {
            return "****-****-****-4242";
        }
        if (method.contains("CARD")) {
            String digits = details.replaceAll("[^0-9]", "");
            String last4 = digits.length() >= 4 ? digits.substring(digits.length() - 4) : "4242";
            return "****-****-****-" + last4;
        }
        if (method.contains("BANK")) {
            return "NETBANKING-ACCT-***" + details.substring(Math.max(0, details.length() - 3));
        }
        return details;
    }

    private static String resolveGateway(String method) {
        return switch (method) {
            case "CREDIT_CARD", "DEBIT_CARD" -> "Stripe PCI-DSS Vault Gateway";
            case "ONLINE_BANKING", "NET_BANKING" -> "ACH / Instant NetBanking Clearinghouse";
            case "UPI" -> "NPCI Unified Payments Gateway";
            default -> "Ecommerce Secure Digital Wallet Gateway";
        };
    }

    private static PaymentReceiptResponse toReceipt(PaymentTransaction t) {
        return new PaymentReceiptResponse(
                t.getReceiptNumber(),
                t.getTransactionId(),
                t.getOrderId(),
                t.getOrderNumber(),
                t.getUserId(),
                t.getUserEmail(),
                t.getAmount(),
                t.getCurrency(),
                t.getPaymentMethod(),
                t.getMaskedInstrument(),
                t.getGatewayProvider(),
                t.getStatus(),
                t.getCryptographicSignature(),
                t.getProcessedAt()
        );
    }
}
