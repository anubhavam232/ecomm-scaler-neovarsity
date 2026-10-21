package com.ecommerce.cart.service;

import com.ecommerce.cart.document.Cart;
import com.ecommerce.cart.document.CartItem;
import com.ecommerce.cart.dto.CartDtos.AddCartItemRequest;
import com.ecommerce.cart.repository.CartRepository;
import com.ecommerce.common.events.CartUpdatedEvent;
import com.ecommerce.common.events.KafkaTopics;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {
    private static final String REDIS_KEY_PREFIX = "ecommerce:cart:user:";
    private final CartRepository cartRepository;
    private final RedisTemplate<String, Cart> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CartService(
            CartRepository cartRepository,
            RedisTemplate<String, Cart> redisTemplate,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.cartRepository = cartRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    // PRD 3.2 Cart Review + HLD 3.3/6 Redis Fast In-Memory Access with MongoDB fallback
    public Cart getCart(Long userId) {
        String redisKey = REDIS_KEY_PREFIX + userId;
        Cart cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            cached.setCacheSource("REDIS_CACHE_HIT");
            return cached;
        }
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart empty = new Cart(userId);
            return cartRepository.save(empty);
        });
        cart.setCacheSource("MONGODB_PRIMARY");
        redisTemplate.opsForValue().set(redisKey, cart, Duration.ofHours(24));
        return cart;
    }

    // PRD 3.1 Add to Cart + HLD Part 2 Flow: Persist to MongoDB, Update Redis Cache, Publish Kafka Event
    public Cart addItem(Long userId, AddCartItemRequest req) {
        if (req.quantity() <= 0) {
            throw new IllegalArgumentException("Item quantity must be positive");
        }
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> new Cart(userId));
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(req.productId()))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + req.quantity());
            item.setUnitPrice(req.unitPrice());
            item.recalculate();
        } else {
            cart.getItems().add(new CartItem(
                    req.productId(),
                    req.sku(),
                    req.productName(),
                    req.unitPrice(),
                    req.quantity()
            ));
        }

        cart.recalculateTotals();
        cart.setCacheSource("WRITE_THROUGH_MONGODB_AND_REDIS");
        Cart saved = cartRepository.save(cart);
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + userId, saved, Duration.ofHours(24));

        // HLD Typical Flow Part 2: "User adds a product to the cart. Cart Service produces a message to Kafka"
        CartUpdatedEvent event = new CartUpdatedEvent(
                UUID.randomUUID().toString(),
                userId,
                "ITEM_ADDED",
                req.productId(),
                req.productName(),
                req.quantity(),
                req.unitPrice(),
                saved.getGrandTotal(),
                Instant.now().toString()
        );
        kafkaTemplate.send(KafkaTopics.CART_EVENTS, String.valueOf(userId), event);
        return saved;
    }

    public Cart updateItemQuantity(Long userId, Long productId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));
        if (quantity <= 0) {
            return removeItem(userId, productId);
        }
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not in cart: " + productId));
        item.setQuantity(quantity);
        cart.recalculateTotals();
        Cart saved = cartRepository.save(cart);
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + userId, saved, Duration.ofHours(24));

        CartUpdatedEvent event = new CartUpdatedEvent(
                UUID.randomUUID().toString(),
                userId,
                "ITEM_QUANTITY_UPDATED",
                productId,
                item.getProductName(),
                quantity,
                item.getUnitPrice(),
                saved.getGrandTotal(),
                Instant.now().toString()
        );
        kafkaTemplate.send(KafkaTopics.CART_EVENTS, String.valueOf(userId), event);
        return saved;
    }

    public Cart removeItem(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        cart.recalculateTotals();
        Cart saved = cartRepository.save(cart);
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + userId, saved, Duration.ofHours(24));

        CartUpdatedEvent event = new CartUpdatedEvent(
                UUID.randomUUID().toString(),
                userId,
                "ITEM_REMOVED",
                productId,
                "",
                0,
                0.0,
                saved.getGrandTotal(),
                Instant.now().toString()
        );
        kafkaTemplate.send(KafkaTopics.CART_EVENTS, String.valueOf(userId), event);
        return saved;
    }

    public Cart clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> new Cart(userId));
        cart.getItems().clear();
        cart.recalculateTotals();
        Cart saved = cartRepository.save(cart);
        redisTemplate.delete(REDIS_KEY_PREFIX + userId);

        CartUpdatedEvent event = new CartUpdatedEvent(
                UUID.randomUUID().toString(),
                userId,
                "CART_CLEARED",
                0L,
                "",
                0,
                0.0,
                0.0,
                Instant.now().toString()
        );
        kafkaTemplate.send(KafkaTopics.CART_EVENTS, String.valueOf(userId), event);
        return saved;
    }
}
