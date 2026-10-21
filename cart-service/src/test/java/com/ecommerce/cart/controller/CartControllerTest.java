package com.ecommerce.cart.controller;

import com.ecommerce.cart.document.Cart;
import com.ecommerce.cart.document.CartItem;
import com.ecommerce.cart.dto.CartDtos.AddCartItemRequest;
import com.ecommerce.cart.dto.CartDtos.UpdateQuantityRequest;
import com.ecommerce.cart.exception.CartItemNotFoundException;
import com.ecommerce.cart.exception.CartNotFoundException;
import com.ecommerce.cart.exception.InvalidCartOperationException;
import com.ecommerce.cart.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CartController using MockMvc
 * Tests all REST endpoints with success and error scenarios
 */
@WebMvcTest(CartController.class)
@DisplayName("CartController Integration Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private Cart testCart;
    private Long userId = 1L;
    private Long productId = 100L;

    @BeforeEach
    void setUp() {
        testCart = new Cart(userId);
    }

    @Nested
    @DisplayName("GET /api/v1/cart/{userId} - Review Cart")
    class ReviewCartTests {

        @Test
        @DisplayName("Should return cart successfully")
        void shouldReturnCartSuccessfully() throws Exception {
            // Arrange
            CartItem item = new CartItem(productId, "SKU-123", "Test Product", 99.99, 2);
            testCart.getItems().add(item);
            testCart.recalculateTotals();
            testCart.setCacheSource("REDIS_CACHE_HIT");

            when(cartService.getCart(userId)).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Cart retrieved"))
                    .andExpect(jsonPath("$.data.userId").value(userId))
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].productId").value(productId))
                    .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.data.cacheSource").value("REDIS_CACHE_HIT"))
                    .andExpect(jsonPath("$.data.totalQuantity").value(2))
                    .andExpect(jsonPath("$.data.subtotal").value(greaterThan(0.0)))
                    .andExpect(jsonPath("$.data.grandTotal").value(greaterThan(0.0)));

            verify(cartService).getCart(userId);
        }

        @Test
        @DisplayName("Should return empty cart when cart has no items")
        void shouldReturnEmptyCart() throws Exception {
            // Arrange
            when(cartService.getCart(userId)).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalQuantity").value(0))
                    .andExpect(jsonPath("$.data.grandTotal").value(0.0));
        }

        @Test
        @DisplayName("Should handle invalid userId parameter")
        void shouldHandleInvalidUserIdParameter() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", "invalid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/cart/{userId}/items - Add Item to Cart")
    class AddItemToCartTests {

        @Test
        @DisplayName("Should add item to cart successfully")
        void shouldAddItemSuccessfully() throws Exception {
            // Arrange
            AddCartItemRequest request = new AddCartItemRequest(
                    productId,
                    "SKU-123",
                    "Test Product",
                    99.99,
                    2
            );

            CartItem item = new CartItem(productId, "SKU-123", "Test Product", 99.99, 2);
            testCart.getItems().add(item);
            testCart.recalculateTotals();

            when(cartService.addItem(eq(userId), any(AddCartItemRequest.class))).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(containsString("Item added to cart")))
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].productId").value(productId))
                    .andExpect(jsonPath("$.data.items[0].quantity").value(2));

            verify(cartService).addItem(eq(userId), any(AddCartItemRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when quantity is zero")
        void shouldReturn400WhenQuantityIsZero() throws Exception {
            // Arrange
            AddCartItemRequest request = new AddCartItemRequest(
                    productId, "SKU-123", "Test Product", 99.99, 0
            );

            when(cartService.addItem(eq(userId), any(AddCartItemRequest.class)))
                    .thenThrow(new InvalidCartOperationException("Item quantity must be positive"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Item quantity must be positive")));
        }

        @Test
        @DisplayName("Should return 400 when quantity is negative")
        void shouldReturn400WhenQuantityIsNegative() throws Exception {
            // Arrange
            AddCartItemRequest request = new AddCartItemRequest(
                    productId, "SKU-123", "Test Product", 99.99, -1
            );

            when(cartService.addItem(eq(userId), any(AddCartItemRequest.class)))
                    .thenThrow(new InvalidCartOperationException("Item quantity must be positive"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should add multiple different items")
        void shouldAddMultipleDifferentItems() throws Exception {
            // Arrange
            AddCartItemRequest request = new AddCartItemRequest(
                    productId, "SKU-123", "Test Product", 99.99, 2
            );

            CartItem item1 = new CartItem(100L, "SKU-123", "Product 1", 99.99, 2);
            CartItem item2 = new CartItem(200L, "SKU-456", "Product 2", 49.99, 1);
            testCart.getItems().add(item1);
            testCart.getItems().add(item2);
            testCart.recalculateTotals();

            when(cartService.addItem(eq(userId), any(AddCartItemRequest.class))).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/cart/{userId}/items/{productId} - Update Quantity")
    class UpdateQuantityTests {

        @Test
        @DisplayName("Should update item quantity successfully")
        void shouldUpdateQuantitySuccessfully() throws Exception {
            // Arrange
            UpdateQuantityRequest request = new UpdateQuantityRequest(5);

            CartItem item = new CartItem(productId, "SKU-123", "Test Product", 99.99, 5);
            testCart.getItems().add(item);
            testCart.recalculateTotals();

            when(cartService.updateItemQuantity(userId, productId, 5)).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(put("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(containsString("quantity updated")))
                    .andExpect(jsonPath("$.data.items[0].quantity").value(5));

            verify(cartService).updateItemQuantity(userId, productId, 5);
        }

        @Test
        @DisplayName("Should return 404 when cart not found")
        void shouldReturn404WhenCartNotFound() throws Exception {
            // Arrange
            UpdateQuantityRequest request = new UpdateQuantityRequest(5);

            when(cartService.updateItemQuantity(userId, productId, 5))
                    .thenThrow(new CartNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(put("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Cart not found")));
        }

        @Test
        @DisplayName("Should return 404 when product not in cart")
        void shouldReturn404WhenProductNotInCart() throws Exception {
            // Arrange
            UpdateQuantityRequest request = new UpdateQuantityRequest(5);

            when(cartService.updateItemQuantity(userId, productId, 5))
                    .thenThrow(new CartItemNotFoundException(productId));

            // Act & Assert
            mockMvc.perform(put("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Product not in cart")));
        }

        @Test
        @DisplayName("Should remove item when quantity is zero")
        void shouldRemoveItemWhenQuantityIsZero() throws Exception {
            // Arrange
            UpdateQuantityRequest request = new UpdateQuantityRequest(0);
            testCart.recalculateTotals();

            when(cartService.updateItemQuantity(userId, productId, 0)).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(put("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/cart/{userId}/items/{productId} - Remove Item")
    class RemoveItemTests {

        @Test
        @DisplayName("Should remove item successfully")
        void shouldRemoveItemSuccessfully() throws Exception {
            // Arrange
            testCart.recalculateTotals();
            when(cartService.removeItem(userId, productId)).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(containsString("Item removed")))
                    .andExpect(jsonPath("$.data.items", hasSize(0)));

            verify(cartService).removeItem(userId, productId);
        }

        @Test
        @DisplayName("Should return 404 when cart not found")
        void shouldReturn404WhenCartNotFound() throws Exception {
            // Arrange
            when(cartService.removeItem(userId, productId))
                    .thenThrow(new CartNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should handle removing non-existent item gracefully")
        void shouldHandleRemovingNonExistentItem() throws Exception {
            // Arrange
            CartItem item = new CartItem(200L, "SKU-456", "Product 2", 49.99, 1);
            testCart.getItems().add(item);
            testCart.recalculateTotals();

            when(cartService.removeItem(userId, productId)).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/cart/{userId} - Clear Cart")
    class ClearCartTests {

        @Test
        @DisplayName("Should clear cart successfully")
        void shouldClearCartSuccessfully() throws Exception {
            // Arrange
            testCart.recalculateTotals();
            when(cartService.clearCart(userId)).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(containsString("Cart cleared")))
                    .andExpect(jsonPath("$.data.items", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalQuantity").value(0))
                    .andExpect(jsonPath("$.data.grandTotal").value(0.0));

            verify(cartService).clearCart(userId);
        }

        @Test
        @DisplayName("Should clear cart even if already empty")
        void shouldClearEmptyCart() throws Exception {
            // Arrange
            when(cartService.clearCart(userId)).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(0)));
        }

        @Test
        @DisplayName("Should handle invalid userId parameter")
        void shouldHandleInvalidUserId() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart/{userId}", "invalid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle generic exception with 500 status")
        void shouldHandleGenericException() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON")
        void shouldReturn400ForMalformedJson() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{malformed json"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should calculate totals correctly when adding items")
        void shouldCalculateTotalsCorrectly() throws Exception {
            // Arrange
            AddCartItemRequest request = new AddCartItemRequest(
                    productId, "SKU-123", "Test Product", 100.0, 2
            );

            CartItem item = new CartItem(productId, "SKU-123", "Test Product", 100.0, 2);
            testCart.getItems().add(item);
            testCart.recalculateTotals();

            when(cartService.addItem(eq(userId), any(AddCartItemRequest.class))).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.subtotal").value(200.0))
                    .andExpect(jsonPath("$.data.estimatedTax").value(16.0)) // 8% tax
                    .andExpect(jsonPath("$.data.grandTotal").value(216.0));
        }

        @Test
        @DisplayName("Should maintain cart state across operations")
        void shouldMaintainCartStateAcrossOperations() throws Exception {
            // Arrange
            CartItem item1 = new CartItem(100L, "SKU-123", "Product 1", 50.0, 2);
            CartItem item2 = new CartItem(200L, "SKU-456", "Product 2", 30.0, 1);
            testCart.getItems().add(item1);
            testCart.getItems().add(item2);
            testCart.recalculateTotals();

            when(cartService.getCart(userId)).thenReturn(testCart);

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalQuantity").value(3))
                    .andExpect(jsonPath("$.data.subtotal").value(130.0));
        }
    }
}
