package com.ecommerce.cart.controller;

import com.ecommerce.cart.document.Cart;
import com.ecommerce.cart.dto.CartDtos.AddCartItemRequest;
import com.ecommerce.cart.dto.CartDtos.UpdateQuantityRequest;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Cart>> reviewCart(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Cart retrieved", cartService.getCart(userId)));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<ApiResponse<Cart>> addItemToCart(
            @PathVariable("userId") Long userId,
            @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart and published to Kafka", cartService.addItem(userId, request)));
    }

    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<ApiResponse<Cart>> updateQuantity(
            @PathVariable("userId") Long userId,
            @PathVariable("productId") Long productId,
            @RequestBody UpdateQuantityRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cart item quantity updated", cartService.updateItemQuantity(userId, productId, request.quantity())));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<ApiResponse<Cart>> removeItem(
            @PathVariable("userId") Long userId,
            @PathVariable("productId") Long productId) {
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", cartService.removeItem(userId, productId)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Cart>> clearCart(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", cartService.clearCart(userId)));
    }
}
