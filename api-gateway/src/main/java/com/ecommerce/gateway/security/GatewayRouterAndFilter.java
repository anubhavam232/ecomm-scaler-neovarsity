package com.ecommerce.gateway.security;

import com.ecommerce.common.security.JwtTokenProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HLD Section 2: API Gateway Routing, Token-Bucket Rate Limiting, and JWT Authentication verification.
 */
public class GatewayRouterAndFilter {
    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider();
    private final Map<String, AtomicInteger> clientRequestCounts = new ConcurrentHashMap<>();
    private final int maxRequestsPerMinute = 120;

    private final Map<String, String> routeTable = Map.of(
            "/api/v1/users", "http://localhost:8081",
            "/api/v1/auth", "http://localhost:8081",
            "/api/v1/products", "http://localhost:8082",
            "/api/v1/cart", "http://localhost:8083",
            "/api/v1/orders", "http://localhost:8084",
            "/api/v1/payments", "http://localhost:8085",
            "/api/v1/notifications", "http://localhost:8086"
    );

    public String resolveUpstreamBaseUrl(String path) {
        for (Map.Entry<String, String> entry : routeTable.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public int checkRateLimitRemaining(String clientKey) {
        AtomicInteger counter = clientRequestCounts.computeIfAbsent(clientKey, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        return Math.max(0, maxRequestsPerMinute - current);
    }

    public boolean requiresAuthentication(String method, String path) {
        if (path.startsWith("/api/v1/users/register")
                || path.startsWith("/api/v1/users/oauth2/social-login")
                || path.startsWith("/api/v1/users/password-reset")
                || path.startsWith("/api/v1/auth/login")
                || ("GET".equalsIgnoreCase(method) && path.startsWith("/api/v1/products"))) {
            return false;
        }
        return true;
    }

    public boolean verifyJwtHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7).trim();
        return jwtTokenProvider.validateToken(token);
    }
}
