package com.ecommerce.payment.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transactionId;

    @Column(nullable = false, unique = true)
    private String receiptNumber;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String paymentMethod; // CREDIT_CARD, DEBIT_CARD, ONLINE_BANKING, UPI, WALLET

    private String maskedInstrument;
    private String gatewayProvider;
    private String status; // SUCCESS, FAILED
    private String cryptographicSignature;
    private String processedAt;

    public PaymentTransaction() {
        this.currency = "USD";
        this.processedAt = Instant.now().toString();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getMaskedInstrument() { return maskedInstrument; }
    public void setMaskedInstrument(String maskedInstrument) { this.maskedInstrument = maskedInstrument; }
    public String getGatewayProvider() { return gatewayProvider; }
    public void setGatewayProvider(String gatewayProvider) { this.gatewayProvider = gatewayProvider; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCryptographicSignature() { return cryptographicSignature; }
    public void setCryptographicSignature(String cryptographicSignature) { this.cryptographicSignature = cryptographicSignature; }
    public String getProcessedAt() { return processedAt; }
    public void setProcessedAt(String processedAt) { this.processedAt = processedAt; }
}
