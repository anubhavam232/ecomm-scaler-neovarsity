package com.ecommerce.cart.exception;

import com.ecommerce.cart.controller.CartController;
import com.ecommerce.cart.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive tests for GlobalExceptionHandler
 * Tests all exception types and ensures proper HTTP status codes and error responses
 */
@WebMvcTest(CartController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    private Long userId = 1L;
    private Long productId = 100L;

    @Nested
    @DisplayName("CartNotFoundException Handling")
    class CartNotFoundExceptionTests {

        @Test
        @DisplayName("Should return 404 with proper error message")
        void shouldReturn404ForCartNotFoundException() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new CartNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Cart not found for user: " + userId)))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return 404 when updating quantity for non-existent cart")
        void shouldReturn404WhenUpdatingQuantityForNonExistentCart() throws Exception {
            // Arrange
            when(cartService.updateItemQuantity(userId, productId, 5))
                    .thenThrow(new CartNotFoundException("Cart not found for user: " + userId));

            String requestBody = "{\"quantity\": 5}";

            // Act & Assert
            mockMvc.perform(put("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Cart not found")));
        }

        @Test
        @DisplayName("Should return 404 when removing item from non-existent cart")
        void shouldReturn404WhenRemovingItemFromNonExistentCart() throws Exception {
            // Arrange
            when(cartService.removeItem(userId, productId))
                    .thenThrow(new CartNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("CartItemNotFoundException Handling")
    class CartItemNotFoundExceptionTests {

        @Test
        @DisplayName("Should return 404 with proper error message")
        void shouldReturn404ForCartItemNotFoundException() throws Exception {
            // Arrange
            when(cartService.updateItemQuantity(userId, productId, 5))
                    .thenThrow(new CartItemNotFoundException(productId));

            String requestBody = "{\"quantity\": 5}";

            // Act & Assert
            mockMvc.perform(put("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Product not in cart: " + productId)))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return 404 when product not found with custom message")
        void shouldReturn404WithCustomMessage() throws Exception {
            // Arrange
            String customMessage = "Product SKU-123 not found in cart";
            when(cartService.updateItemQuantity(userId, productId, 3))
                    .thenThrow(new CartItemNotFoundException(customMessage));

            String requestBody = "{\"quantity\": 3}";

            // Act & Assert
            mockMvc.perform(put("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(customMessage));
        }
    }

    @Nested
    @DisplayName("InvalidCartOperationException Handling")
    class InvalidCartOperationExceptionTests {

        @Test
        @DisplayName("Should return 400 for invalid quantity")
        void shouldReturn400ForInvalidQuantity() throws Exception {
            // Arrange
            String requestBody = "{\"productId\": 100, \"sku\": \"SKU-123\", \"productName\": \"Test\", \"unitPrice\": 99.99, \"quantity\": 0}";
            
            when(cartService.addItem(any(), any()))
                    .thenThrow(new InvalidCartOperationException("Item quantity must be positive"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Item quantity must be positive")))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return 400 for negative quantity")
        void shouldReturn400ForNegativeQuantity() throws Exception {
            // Arrange
            String requestBody = "{\"productId\": 100, \"sku\": \"SKU-123\", \"productName\": \"Test\", \"unitPrice\": 99.99, \"quantity\": -5}";
            
            when(cartService.addItem(any(), any()))
                    .thenThrow(new InvalidCartOperationException("Item quantity must be positive"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 400 with custom message")
        void shouldReturn400WithCustomMessage() throws Exception {
            // Arrange
            String customMessage = "Cannot add more than 10 items of the same product";
            String requestBody = "{\"productId\": 100, \"sku\": \"SKU-123\", \"productName\": \"Test\", \"unitPrice\": 99.99, \"quantity\": 15}";
            
            when(cartService.addItem(any(), any()))
                    .thenThrow(new InvalidCartOperationException(customMessage));

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(customMessage));
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException Handling")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("Should return 400 for IllegalArgumentException")
        void shouldReturn400ForIllegalArgumentException() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new IllegalArgumentException("Invalid argument provided"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid argument provided")))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should handle null argument exception")
        void shouldHandleNullArgumentException() throws Exception {
            // Arrange
            when(cartService.addItem(any(), any()))
                    .thenThrow(new IllegalArgumentException("Product ID cannot be null"));

            String requestBody = "{\"productId\": null, \"sku\": \"SKU-123\", \"productName\": \"Test\", \"unitPrice\": 99.99, \"quantity\": 1}";

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("MethodArgumentTypeMismatchException Handling")
    class MethodArgumentTypeMismatchExceptionTests {

        @Test
        @DisplayName("Should return 400 for invalid userId type")
        void shouldReturn400ForInvalidUserIdType() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", "invalid-user-id")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid value")));
        }

        @Test
        @DisplayName("Should return 400 for invalid productId type")
        void shouldReturn400ForInvalidProductIdType() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/cart/{userId}/items/{productId}", userId, "invalid-product-id")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 400 for special characters in path variable")
        void shouldReturn400ForSpecialCharactersInPathVariable() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", "@#$%")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("CartServiceException Handling")
    class CartServiceExceptionTests {

        @Test
        @DisplayName("Should return 500 for generic CartServiceException")
        void shouldReturn500ForCartServiceException() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new CartServiceException("Internal cart service error"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Cart service error")))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return 500 for CartServiceException with cause")
        void shouldReturn500ForCartServiceExceptionWithCause() throws Exception {
            // Arrange
            Exception cause = new RuntimeException("Database connection failed");
            when(cartService.addItem(any(), any()))
                    .thenThrow(new CartServiceException("Failed to add item", cause));

            String requestBody = "{\"productId\": 100, \"sku\": \"SKU-123\", \"productName\": \"Test\", \"unitPrice\": 99.99, \"quantity\": 1}";

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Generic Exception Handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should return 500 for unexpected exceptions")
        void shouldReturn500ForUnexpectedExceptions() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new RuntimeException("Unexpected error occurred"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("An unexpected error occurred")))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return 500 for NullPointerException")
        void shouldReturn500ForNullPointerException() throws Exception {
            // Arrange
            when(cartService.addItem(any(), any()))
                    .thenThrow(new NullPointerException("Null reference error"));

            String requestBody = "{\"productId\": 100, \"sku\": \"SKU-123\", \"productName\": \"Test\", \"unitPrice\": 99.99, \"quantity\": 1}";

            // Act & Assert
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should hide internal error details from client")
        void shouldHideInternalErrorDetails() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new RuntimeException("Database connection string: jdbc://secret-host:5432/db"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(containsString("An unexpected error occurred")))
                    .andExpect(jsonPath("$.message").value(containsString("Please try again later")));
        }
    }

    @Nested
    @DisplayName("Error Response Format Tests")
    class ErrorResponseFormatTests {

        @Test
        @DisplayName("Should return consistent error response structure")
        void shouldReturnConsistentErrorResponseStructure() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new CartNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").exists())
                    .andExpect(jsonPath("$.success").isBoolean())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.message").isString())
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("Should set success to false for all errors")
        void shouldSetSuccessToFalseForAllErrors() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new InvalidCartOperationException("Invalid operation"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should set data to null for error responses")
        void shouldSetDataToNullForErrorResponses() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new CartNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return application/json content type")
        void shouldReturnApplicationJsonContentType() throws Exception {
            // Arrange
            when(cartService.getCart(userId))
                    .thenThrow(new CartNotFoundException(userId));

            // Act & Assert
            mockMvc.perform(get("/api/v1/cart/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Multiple Exception Scenarios")
    class MultipleExceptionScenariosTests {

        @Test
        @DisplayName("Should handle different exceptions for different operations")
        void shouldHandleDifferentExceptionsForDifferentOperations() throws Exception {
            // Arrange - Cart not found
            when(cartService.getCart(999L))
                    .thenThrow(new CartNotFoundException(999L));

            // Act & Assert - Cart not found
            mockMvc.perform(get("/api/v1/cart/{userId}", 999L)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            // Arrange - Invalid operation
            when(cartService.addItem(any(), any()))
                    .thenThrow(new InvalidCartOperationException("Invalid quantity"));

            String requestBody = "{\"productId\": 100, \"sku\": \"SKU-123\", \"productName\": \"Test\", \"unitPrice\": 99.99, \"quantity\": 0}";

            // Act & Assert - Invalid operation
            mockMvc.perform(post("/api/v1/cart/{userId}/items", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should maintain exception handling across all endpoints")
        void shouldMaintainExceptionHandlingAcrossAllEndpoints() throws Exception {
            // Test exception handling on different endpoints
            when(cartService.updateItemQuantity(anyLong(), anyLong(), anyInt()))
                    .thenThrow(new CartItemNotFoundException(productId));

            String requestBody = "{\"quantity\": 5}";

            // PUT endpoint
            mockMvc.perform(put("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));

            // DELETE endpoint
            when(cartService.removeItem(anyLong(), anyLong()))
                    .thenThrow(new CartNotFoundException(userId));

            mockMvc.perform(delete("/api/v1/cart/{userId}/items/{productId}", userId, productId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
