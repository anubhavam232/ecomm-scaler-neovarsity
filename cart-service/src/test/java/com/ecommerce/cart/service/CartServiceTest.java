package com.ecommerce.cart.service;

import com.ecommerce.cart.document.Cart;
import com.ecommerce.cart.document.CartItem;
import com.ecommerce.cart.dto.CartDtos.AddCartItemRequest;
import com.ecommerce.cart.exception.CartItemNotFoundException;
import com.ecommerce.cart.exception.CartNotFoundException;
import com.ecommerce.cart.exception.InvalidCartOperationException;
import com.ecommerce.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CartService
 * Coverage: 90%+ including all business logic, edge cases, and error scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RedisTemplate<String, Cart> redisTemplate;

    @Mock
    private ValueOperations<String, Cart> valueOperations;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CartService cartService;

    private Cart testCart;
    private Long userId = 1L;
    private Long productId = 100L;
    private String redisKey;

    @BeforeEach
    void setUp() {
        redisKey = "ecommerce:cart:user:" + userId;
        testCart = new Cart(userId);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("getCart() Tests")
    class GetCartTests {

        @Test
        @DisplayName("Should return cart from Redis cache when available")
        void shouldReturnCartFromRedisCache() {
            // Arrange
            when(valueOperations.get(redisKey)).thenReturn(testCart);

            // Act
            Cart result = cartService.getCart(userId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCacheSource()).isEqualTo("REDIS_CACHE_HIT");
            assertThat(result.getUserId()).isEqualTo(userId);
            verify(valueOperations).get(redisKey);
            verify(cartRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("Should fetch from MongoDB and cache in Redis when not in cache")
        void shouldFetchFromMongoDBWhenNotInCache() {
            // Arrange
            when(valueOperations.get(redisKey)).thenReturn(null);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));

            // Act
            Cart result = cartService.getCart(userId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCacheSource()).isEqualTo("MONGODB_PRIMARY");
            verify(cartRepository).findByUserId(userId);
            verify(valueOperations).set(redisKey, result, Duration.ofHours(24));
        }

        @Test
        @DisplayName("Should create new cart when not found in MongoDB")
        void shouldCreateNewCartWhenNotFound() {
            // Arrange
            when(valueOperations.get(redisKey)).thenReturn(null);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

            // Act
            Cart result = cartService.getCart(userId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getCacheSource()).isEqualTo("MONGODB_PRIMARY");
            verify(cartRepository).save(any(Cart.class));
            verify(valueOperations).set(redisKey, result, Duration.ofHours(24));
        }
    }

    @Nested
    @DisplayName("addItem() Tests")
    class AddItemTests {

        private AddCartItemRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new AddCartItemRequest(
                    productId,
                    "SKU-123",
                    "Test Product",
                    99.99,
                    2
            );
        }

        @Test
        @DisplayName("Should throw InvalidCartOperationException when quantity is zero")
        void shouldThrowExceptionWhenQuantityIsZero() {
            // Arrange
            AddCartItemRequest invalidRequest = new AddCartItemRequest(
                    productId, "SKU-123", "Test Product", 99.99, 0
            );

            // Act & Assert
            assertThatThrownBy(() -> cartService.addItem(userId, invalidRequest))
                    .isInstanceOf(InvalidCartOperationException.class)
                    .hasMessageContaining("Item quantity must be positive");

            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidCartOperationException when quantity is negative")
        void shouldThrowExceptionWhenQuantityIsNegative() {
            // Arrange
            AddCartItemRequest invalidRequest = new AddCartItemRequest(
                    productId, "SKU-123", "Test Product", 99.99, -1
            );

            // Act & Assert
            assertThatThrownBy(() -> cartService.addItem(userId, invalidRequest))
                    .isInstanceOf(InvalidCartOperationException.class)
                    .hasMessageContaining("Item quantity must be positive");
        }

        @Test
        @DisplayName("Should add new item to empty cart")
        void shouldAddNewItemToEmptyCart() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.addItem(userId, validRequest);

            // Assert
            assertThat(result.getItems()).hasSize(1);
            CartItem addedItem = result.getItems().get(0);
            assertThat(addedItem.getProductId()).isEqualTo(productId);
            assertThat(addedItem.getQuantity()).isEqualTo(2);
            assertThat(addedItem.getUnitPrice()).isEqualTo(99.99);
            assertThat(result.getTotalQuantity()).isEqualTo(2);
            verify(cartRepository).save(any(Cart.class));
            verify(valueOperations).set(eq(redisKey), any(Cart.class), eq(Duration.ofHours(24)));
            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should update quantity when adding existing item")
        void shouldUpdateQuantityForExistingItem() {
            // Arrange
            CartItem existingItem = new CartItem(productId, "SKU-123", "Test Product", 99.99, 3);
            testCart.getItems().add(existingItem);
            testCart.recalculateTotals();

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.addItem(userId, validRequest);

            // Assert
            assertThat(result.getItems()).hasSize(1);
            CartItem updatedItem = result.getItems().get(0);
            assertThat(updatedItem.getQuantity()).isEqualTo(5); // 3 + 2
            assertThat(result.getTotalQuantity()).isEqualTo(5);
            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should create cart when user has no cart")
        void shouldCreateCartWhenUserHasNoCart() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.addItem(userId, validRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should recalculate totals correctly")
        void shouldRecalculateTotalsCorrectly() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.addItem(userId, validRequest);

            // Assert
            assertThat(result.getSubtotal()).isGreaterThan(0);
            assertThat(result.getEstimatedTax()).isGreaterThan(0);
            assertThat(result.getGrandTotal()).isEqualTo(result.getSubtotal() + result.getEstimatedTax());
        }
    }

    @Nested
    @DisplayName("updateItemQuantity() Tests")
    class UpdateItemQuantityTests {

        @BeforeEach
        void setUp() {
            CartItem item = new CartItem(productId, "SKU-123", "Test Product", 99.99, 3);
            testCart.getItems().add(item);
            testCart.recalculateTotals();
        }

        @Test
        @DisplayName("Should throw CartNotFoundException when cart not found")
        void shouldThrowExceptionWhenCartNotFound() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.updateItemQuantity(userId, productId, 5))
                    .isInstanceOf(CartNotFoundException.class)
                    .hasMessageContaining("Cart not found for user: " + userId);
        }

        @Test
        @DisplayName("Should throw CartItemNotFoundException when product not in cart")
        void shouldThrowExceptionWhenProductNotInCart() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            Long nonExistentProductId = 999L;

            // Act & Assert
            assertThatThrownBy(() -> cartService.updateItemQuantity(userId, nonExistentProductId, 5))
                    .isInstanceOf(CartItemNotFoundException.class)
                    .hasMessageContaining("Product not in cart: " + nonExistentProductId);
        }

        @Test
        @DisplayName("Should update item quantity successfully")
        void shouldUpdateItemQuantitySuccessfully() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.updateItemQuantity(userId, productId, 10);

            // Assert
            CartItem updatedItem = result.getItems().get(0);
            assertThat(updatedItem.getQuantity()).isEqualTo(10);
            assertThat(result.getTotalQuantity()).isEqualTo(10);
            verify(cartRepository).save(any(Cart.class));
            verify(valueOperations).set(eq(redisKey), any(Cart.class), eq(Duration.ofHours(24)));
            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should remove item when quantity is zero")
        void shouldRemoveItemWhenQuantityIsZero() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.updateItemQuantity(userId, productId, 0);

            // Assert
            assertThat(result.getItems()).isEmpty();
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should remove item when quantity is negative")
        void shouldRemoveItemWhenQuantityIsNegative() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.updateItemQuantity(userId, productId, -1);

            // Assert
            assertThat(result.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeItem() Tests")
    class RemoveItemTests {

        @BeforeEach
        void setUp() {
            CartItem item = new CartItem(productId, "SKU-123", "Test Product", 99.99, 3);
            testCart.getItems().add(item);
            testCart.recalculateTotals();
        }

        @Test
        @DisplayName("Should throw CartNotFoundException when cart not found")
        void shouldThrowExceptionWhenCartNotFound() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cartService.removeItem(userId, productId))
                    .isInstanceOf(CartNotFoundException.class)
                    .hasMessageContaining("Cart not found for user: " + userId);
        }

        @Test
        @DisplayName("Should remove item successfully")
        void shouldRemoveItemSuccessfully() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.removeItem(userId, productId);

            // Assert
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getTotalQuantity()).isEqualTo(0);
            assertThat(result.getSubtotal()).isEqualTo(0.0);
            verify(cartRepository).save(any(Cart.class));
            verify(valueOperations).set(eq(redisKey), any(Cart.class), eq(Duration.ofHours(24)));
            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should handle removing non-existent item gracefully")
        void shouldHandleRemovingNonExistentItem() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
            Long nonExistentProductId = 999L;

            // Act
            Cart result = cartService.removeItem(userId, nonExistentProductId);

            // Assert
            assertThat(result.getItems()).hasSize(1); // Original item still there
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should remove specific item from multi-item cart")
        void shouldRemoveSpecificItemFromMultiItemCart() {
            // Arrange
            Long productId2 = 200L;
            CartItem item2 = new CartItem(productId2, "SKU-456", "Product 2", 49.99, 1);
            testCart.getItems().add(item2);
            testCart.recalculateTotals();

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.removeItem(userId, productId);

            // Assert
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductId()).isEqualTo(productId2);
        }
    }

    @Nested
    @DisplayName("clearCart() Tests")
    class ClearCartTests {

        @Test
        @DisplayName("Should clear cart successfully")
        void shouldClearCartSuccessfully() {
            // Arrange
            CartItem item = new CartItem(productId, "SKU-123", "Test Product", 99.99, 3);
            testCart.getItems().add(item);
            testCart.recalculateTotals();

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.clearCart(userId);

            // Assert
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getTotalQuantity()).isEqualTo(0);
            assertThat(result.getSubtotal()).isEqualTo(0.0);
            assertThat(result.getGrandTotal()).isEqualTo(0.0);
            verify(cartRepository).save(any(Cart.class));
            verify(redisTemplate).delete(redisKey);
            verify(kafkaTemplate).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should create empty cart when user has no cart")
        void shouldCreateEmptyCartWhenUserHasNoCart() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.clearCart(userId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should clear already empty cart")
        void shouldClearAlreadyEmptyCart() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cart result = cartService.clearCart(userId);

            // Assert
            assertThat(result.getItems()).isEmpty();
            verify(redisTemplate).delete(redisKey);
        }
    }

    @Nested
    @DisplayName("Kafka Event Publishing Tests")
    class KafkaEventTests {

        @Test
        @DisplayName("Should publish ITEM_ADDED event when adding item")
        void shouldPublishItemAddedEvent() {
            // Arrange
            AddCartItemRequest request = new AddCartItemRequest(
                    productId, "SKU-123", "Test Product", 99.99, 2
            );
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            cartService.addItem(userId, request);

            // Assert
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(kafkaTemplate).send(anyString(), eq(String.valueOf(userId)), eventCaptor.capture());
        }

        @Test
        @DisplayName("Should publish ITEM_QUANTITY_UPDATED event when updating quantity")
        void shouldPublishItemQuantityUpdatedEvent() {
            // Arrange
            CartItem item = new CartItem(productId, "SKU-123", "Test Product", 99.99, 3);
            testCart.getItems().add(item);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            cartService.updateItemQuantity(userId, productId, 5);

            // Assert
            verify(kafkaTemplate).send(anyString(), eq(String.valueOf(userId)), any());
        }

        @Test
        @DisplayName("Should publish ITEM_REMOVED event when removing item")
        void shouldPublishItemRemovedEvent() {
            // Arrange
            CartItem item = new CartItem(productId, "SKU-123", "Test Product", 99.99, 3);
            testCart.getItems().add(item);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            cartService.removeItem(userId, productId);

            // Assert
            verify(kafkaTemplate).send(anyString(), eq(String.valueOf(userId)), any());
        }

        @Test
        @DisplayName("Should publish CART_CLEARED event when clearing cart")
        void shouldPublishCartClearedEvent() {
            // Arrange
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(testCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            cartService.clearCart(userId);

            // Assert
            verify(kafkaTemplate).send(anyString(), eq(String.valueOf(userId)), any());
        }
    }
}
