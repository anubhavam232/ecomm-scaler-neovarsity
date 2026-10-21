package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.PaymentTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for PaymentTransactionRepository.
 * Tests JPA queries and database operations using H2 in-memory database.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
@DisplayName("Payment Transaction Repository Tests")
class PaymentTransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PaymentTransactionRepository repository;

    private PaymentTransaction transaction1;
    private PaymentTransaction transaction2;
    private PaymentTransaction transaction3;

    @BeforeEach
    void setUp() {
        // Create sample transactions
        transaction1 = createPaymentTransaction(
                "TXN-001", "RCPT-001", 100L, "ORD-100", 1L, "user1@example.com", 99.99, "CREDIT_CARD"
        );

        transaction2 = createPaymentTransaction(
                "TXN-002", "RCPT-002", 101L, "ORD-101", 1L, "user1@example.com", 149.99, "DEBIT_CARD"
        );

        transaction3 = createPaymentTransaction(
                "TXN-003", "RCPT-003", 102L, "ORD-102", 2L, "user2@example.com", 199.99, "UPI"
        );

        // Persist transactions
        entityManager.persist(transaction1);
        entityManager.persist(transaction2);
        entityManager.persist(transaction3);
        entityManager.flush();
    }

    // ========== findByOrderId Tests ==========

    @Test
    @DisplayName("Should find payment transaction by order ID")
    void findByOrderId_WhenExists_ShouldReturnTransaction() {
        // Act
        Optional<PaymentTransaction> found = repository.findByOrderId(100L);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(100L);
        assertThat(found.get().getTransactionId()).isEqualTo("TXN-001");
        assertThat(found.get().getOrderNumber()).isEqualTo("ORD-100");
    }

    @Test
    @DisplayName("Should return empty when order ID does not exist")
    void findByOrderId_WhenNotExists_ShouldReturnEmpty() {
        // Act
        Optional<PaymentTransaction> found = repository.findByOrderId(999L);

        // Assert
        assertThat(found).isEmpty();
    }

    // ========== findByTransactionId Tests ==========

    @Test
    @DisplayName("Should find payment transaction by transaction ID")
    void findByTransactionId_WhenExists_ShouldReturnTransaction() {
        // Act
        Optional<PaymentTransaction> found = repository.findByTransactionId("TXN-002");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getTransactionId()).isEqualTo("TXN-002");
        assertThat(found.get().getOrderId()).isEqualTo(101L);
        assertThat(found.get().getAmount()).isEqualTo(149.99);
    }

    @Test
    @DisplayName("Should return empty when transaction ID does not exist")
    void findByTransactionId_WhenNotExists_ShouldReturnEmpty() {
        // Act
        Optional<PaymentTransaction> found = repository.findByTransactionId("TXN-999");

        // Assert
        assertThat(found).isEmpty();
    }

    // ========== findByReceiptNumber Tests ==========

    @Test
    @DisplayName("Should find payment transaction by receipt number")
    void findByReceiptNumber_WhenExists_ShouldReturnTransaction() {
        // Act
        Optional<PaymentTransaction> found = repository.findByReceiptNumber("RCPT-003");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getReceiptNumber()).isEqualTo("RCPT-003");
        assertThat(found.get().getTransactionId()).isEqualTo("TXN-003");
        assertThat(found.get().getPaymentMethod()).isEqualTo("UPI");
    }

    @Test
    @DisplayName("Should return empty when receipt number does not exist")
    void findByReceiptNumber_WhenNotExists_ShouldReturnEmpty() {
        // Act
        Optional<PaymentTransaction> found = repository.findByReceiptNumber("RCPT-999");

        // Assert
        assertThat(found).isEmpty();
    }

    // ========== findByUserId Tests ==========

    @Test
    @DisplayName("Should find all payment transactions by user ID")
    void findByUserId_WhenMultipleExist_ShouldReturnAllTransactions() {
        // Act
        List<PaymentTransaction> found = repository.findByUserId(1L);

        // Assert
        assertThat(found).hasSize(2);
        assertThat(found).extracting(PaymentTransaction::getTransactionId)
                .containsExactlyInAnyOrder("TXN-001", "TXN-002");
        assertThat(found).allMatch(txn -> txn.getUserId().equals(1L));
    }

    @Test
    @DisplayName("Should find single payment transaction by user ID")
    void findByUserId_WhenOneExists_ShouldReturnSingleTransaction() {
        // Act
        List<PaymentTransaction> found = repository.findByUserId(2L);

        // Assert
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getUserId()).isEqualTo(2L);
        assertThat(found.get(0).getTransactionId()).isEqualTo("TXN-003");
    }

    @Test
    @DisplayName("Should return empty list when user has no transactions")
    void findByUserId_WhenNoneExist_ShouldReturnEmptyList() {
        // Act
        List<PaymentTransaction> found = repository.findByUserId(999L);

        // Assert
        assertThat(found).isEmpty();
    }

    // ========== Save and Update Tests ==========

    @Test
    @DisplayName("Should save new payment transaction")
    void save_WithNewTransaction_ShouldPersist() {
        // Arrange
        PaymentTransaction newTransaction = createPaymentTransaction(
                "TXN-004", "RCPT-004", 103L, "ORD-103", 3L, "user3@example.com", 299.99, "WALLET"
        );

        // Act
        PaymentTransaction saved = repository.save(newTransaction);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findByOrderId(103L)).isPresent();
    }

    @Test
    @DisplayName("Should update existing payment transaction")
    void save_WithExistingTransaction_ShouldUpdate() {
        // Arrange
        PaymentTransaction existing = repository.findByOrderId(100L).orElseThrow();
        existing.setStatus("FAILED");

        // Act
        PaymentTransaction updated = repository.save(existing);

        // Assert
        assertThat(updated.getStatus()).isEqualTo("FAILED");
        assertThat(repository.findByOrderId(100L).orElseThrow().getStatus()).isEqualTo("FAILED");
    }

    // ========== Unique Constraint Tests ==========

    @Test
    @DisplayName("Should enforce unique constraint on transaction ID")
    void save_WithDuplicateTransactionId_ShouldFail() {
        // Arrange
        PaymentTransaction duplicate = createPaymentTransaction(
                "TXN-001", "RCPT-999", 999L, "ORD-999", 99L, "user99@example.com", 99.99, "CREDIT_CARD"
        );

        // Act & Assert
        try {
            repository.saveAndFlush(duplicate);
            assertThat(false).as("Should have thrown exception for duplicate transactionId").isTrue();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(Exception.class);
        }
    }

    @Test
    @DisplayName("Should enforce unique constraint on receipt number")
    void save_WithDuplicateReceiptNumber_ShouldFail() {
        // Arrange
        PaymentTransaction duplicate = createPaymentTransaction(
                "TXN-999", "RCPT-001", 999L, "ORD-999", 99L, "user99@example.com", 99.99, "CREDIT_CARD"
        );

        // Act & Assert
        try {
            repository.saveAndFlush(duplicate);
            assertThat(false).as("Should have thrown exception for duplicate receiptNumber").isTrue();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(Exception.class);
        }
    }

    // ========== Count and Delete Tests ==========

    @Test
    @DisplayName("Should count all payment transactions")
    void count_ShouldReturnTotalTransactions() {
        // Act
        long count = repository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should delete payment transaction")
    void delete_ShouldRemoveTransaction() {
        // Arrange
        PaymentTransaction toDelete = repository.findByOrderId(100L).orElseThrow();

        // Act
        repository.delete(toDelete);

        // Assert
        assertThat(repository.findByOrderId(100L)).isEmpty();
        assertThat(repository.count()).isEqualTo(2);
    }

    // ========== Helper Methods ==========

    private PaymentTransaction createPaymentTransaction(
            String transactionId, String receiptNumber, Long orderId, String orderNumber,
            Long userId, String userEmail, double amount, String paymentMethod
    ) {
        PaymentTransaction txn = new PaymentTransaction();
        txn.setTransactionId(transactionId);
        txn.setReceiptNumber(receiptNumber);
        txn.setOrderId(orderId);
        txn.setOrderNumber(orderNumber);
        txn.setUserId(userId);
        txn.setUserEmail(userEmail);
        txn.setAmount(amount);
        txn.setCurrency("USD");
        txn.setPaymentMethod(paymentMethod);
        txn.setMaskedInstrument("****-****-****-4242");
        txn.setGatewayProvider("Test Gateway");
        txn.setStatus("SUCCESS");
        txn.setCryptographicSignature("test-signature");
        txn.setProcessedAt(Instant.now().toString());
        return txn;
    }
}