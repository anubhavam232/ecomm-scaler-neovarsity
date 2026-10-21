package com.ecommerce.payment.service;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.common.events.OrderPlacedEvent;
import com.ecommerce.common.events.PaymentCompletedEvent;
import com.ecommerce.common.security.JwtTokenProvider;
import com.ecommerce.payment.dto.PaymentDtos.ManualPaymentRequest;
import com.ecommerce.payment.dto.PaymentDtos.PaymentReceiptResponse;
import com.ecommerce.payment.entity.PaymentTransaction;
import com.ecommerce.payment.exception.InvalidPaymentException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.exception.PaymentProcessingException;
import com.ecommerce.payment.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

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
        try {
            logger.info("Processing payment for order: {}", event.orderNumber());
            processOrderPayment(
                    event.orderId(),
                    event.orderNumber(),
                    event.userId(),
                    event.userEmail(),
                    event.totalAmount(),
                    event.paymentMethod(),
                    event.paymentInstrumentDetails()
            );
            logger.info("Payment processed successfully for order: {}", event.orderNumber());
        } catch (Exception e) {
            logger.error("Failed to process payment for order: {}", event.orderNumber(), e);
            throw new PaymentProcessingException("Failed to process payment for order: " + event.orderNumber(), e);
        }
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
        // Validate input parameters
        if (orderId == null || orderId <= 0) {
            throw new InvalidPaymentException("Order ID must be a positive number");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new InvalidPaymentException("Order number is required");
        }
        if (userId == null || userId <= 0) {
            throw new InvalidPaymentException("User ID must be a positive number");
        }
        if (userEmail == null || userEmail.isBlank()) {
            throw new InvalidPaymentException("User email is required");
        }
        if (amount <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }

        String normalizedMethod = paymentMethod == null ? "CREDIT_CARD" : paymentMethod.toUpperCase();

        // Validate payment method
        if (!SUPPORTED_METHODS.contains(normalizedMethod)) {
            throw new InvalidPaymentException("Unsupported payment method: " + paymentMethod + ". Supported methods: " + SUPPORTED_METHODS);
        }

        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String receiptNo = "RCPT-" + System.currentTimeMillis() + "-" + orderId;

        String status = "SUCCESS";
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

        PaymentTransaction saved;
        try {
            saved = paymentRepository.save(txn);
            logger.info("Payment transaction saved: {} for order: {}", txnId, orderNumber);
        } catch (DataAccessException e) {
            logger.error("Failed to save payment transaction for order: {}", orderNumber, e);
            throw new PaymentProcessingException("Failed to save payment transaction", e);
        }

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

        try {
            kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, saved.getOrderNumber(), completedEvent);
            logger.info("Payment completed event published for order: {}", saved.getOrderNumber());
        } catch (Exception e) {
            logger.error("Failed to publish payment completed event for order: {}", saved.getOrderNumber(), e);
            // Don't throw exception here - payment is already saved, just log the error
        }

        return saved;
    }

    public PaymentReceiptResponse processManualPayment(ManualPaymentRequest req) {
        if (req == null) {
            throw new InvalidPaymentException("Payment request cannot be null");
        }

        logger.info("Processing manual payment for order: {}", req.orderNumber());

        try {
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
        } catch (InvalidPaymentException | PaymentProcessingException e) {
            throw e; // Re-throw known exceptions
        } catch (Exception e) {
            logger.error("Unexpected error processing manual payment for order: {}", req.orderNumber(), e);
            throw new PaymentProcessingException("Unexpected error processing payment", e);
        }
    }

    // PRD 5.3 Payment Receipt by Order ID
    public PaymentReceiptResponse getReceiptByOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new InvalidPaymentException("Order ID must be a positive number");
        }

        logger.info("Retrieving payment receipt for order ID: {}", orderId);

        PaymentTransaction txn = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(orderId));

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
