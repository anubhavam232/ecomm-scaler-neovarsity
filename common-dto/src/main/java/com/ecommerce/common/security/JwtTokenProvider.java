package com.ecommerce.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RFC 7519 compliant HMAC-SHA256 JWT Provider and Password Hasher (Requirements 6.1 & 6.2).
 */
public class JwtTokenProvider {
    private static final String HMAC_ALGO = "HmacSHA256";
    private final String secretKey;
    private final long validityInMilliseconds;
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    public JwtTokenProvider() {
        this("EcommerceSuperSecretJwtSigningKey2026ForMicroservicesAuth!", 3600_000L * 24);
    }

    public JwtTokenProvider(String secretKey, long validityInMilliseconds) {
        this.secretKey = secretKey;
        this.validityInMilliseconds = validityInMilliseconds;
    }

    public String createToken(Long userId, String email, String role, String sessionId) {
        long now = Instant.now().getEpochSecond();
        long exp = now + (validityInMilliseconds / 1000);
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format(
                "{\"sub\":\"%d\",\"email\":\"%s\",\"role\":\"%s\",\"sid\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId, escape(email), escape(role), escape(sessionId), now, exp
        );
        String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = sign(signingInput);
        return signingInput + "." + signature;
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank() || revokedTokens.contains(token)) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        String expectedSig = sign(parts[0] + "." + parts[1]);
        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        long exp = extractLongClaim(payload, "exp");
        return Instant.now().getEpochSecond() < exp;
    }

    public Long extractUserId(String token) {
        String payload = decodePayload(token);
        String sub = extractStringClaim(payload, "sub");
        return sub != null ? Long.parseLong(sub) : null;
    }

    public String extractEmail(String token) {
        return extractStringClaim(decodePayload(token), "email");
    }

    public String extractSessionId(String token) {
        return extractStringClaim(decodePayload(token), "sid");
    }

    public void revokeToken(String token) {
        if (token != null) {
            revokedTokens.add(token);
        }
    }

    public static String hashPassword(String rawPassword, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    private String decodePayload(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token format");
        }
        return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            return base64UrlEncode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String escape(String input) {
        return input == null ? "" : input.replace("\"", "\\\"");
    }

    private static String extractStringClaim(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        int start = idx + pattern.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }

    private static long extractLongClaim(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return 0L;
        int start = idx + pattern.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }
}
