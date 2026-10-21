package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByOrderId(Long orderId);
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    Optional<PaymentTransaction> findByReceiptNumber(String receiptNumber);
    List<PaymentTransaction> findByUserId(Long userId);
}
