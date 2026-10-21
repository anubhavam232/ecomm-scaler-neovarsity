package com.ecommerce.user.service;

import com.ecommerce.common.events.KafkaTopics;
import com.ecommerce.common.events.PasswordResetRequestedEvent;
import com.ecommerce.common.events.UserRegisteredEvent;
import com.ecommerce.common.security.JwtTokenProvider;
import com.ecommerce.user.dto.UserDtos.*;
import com.ecommerce.user.entity.PasswordResetToken;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.entity.UserSession;
import com.ecommerce.user.exception.*;
import com.ecommerce.user.repository.PasswordResetTokenRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.repository.UserSessionRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(
            UserRepository userRepository,
            UserSessionRepository sessionRepository,
            PasswordResetTokenRepository resetTokenRepository,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.jwtTokenProvider = new JwtTokenProvider();
    }

    // PRD 1.1 Registration (Email/Password) + HLD 3.1 Kafka Event
    public UserProfileResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new DuplicateResourceException("Account already exists for email: " + req.email());
        }
        String salt = UUID.randomUUID().toString();
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordSalt(salt);
        user.setPasswordHash(JwtTokenProvider.hashPassword(req.password(), salt));
        user.setFullName(req.fullName());
        user.setPhone(req.phone());
        user.setDefaultAddress(req.defaultAddress());
        user.setAuthProvider("LOCAL");

        User saved = userRepository.save(user);

        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID().toString(),
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                "EMAIL_REGISTRATION",
                Instant.now().toString()
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, String.valueOf(saved.getId()), event);

        return toProfileResponse(saved);
    }

    // PRD 1.1 Social Media Registration / Login (Google, Facebook, GitHub)
    public AuthSessionResponse socialLogin(SocialLoginRequest req) {
        User user = userRepository.findByEmail(req.email()).orElseGet(() -> {
            String salt = UUID.randomUUID().toString();
            User newUser = new User();
            newUser.setEmail(req.email());
            newUser.setPasswordSalt(salt);
            newUser.setPasswordHash(JwtTokenProvider.hashPassword(UUID.randomUUID().toString(), salt));
            newUser.setFullName(req.fullName());
            newUser.setAuthProvider(req.provider().toUpperCase());
            newUser.setSocialProviderId(req.providerId());
            User created = userRepository.save(newUser);

            UserRegisteredEvent event = new UserRegisteredEvent(
                    UUID.randomUUID().toString(),
                    created.getId(),
                    created.getEmail(),
                    created.getFullName(),
                    "SOCIAL_" + req.provider().toUpperCase(),
                    Instant.now().toString()
            );
            kafkaTemplate.send(KafkaTopics.USER_EVENTS, String.valueOf(created.getId()), event);
            return created;
        });

        return createSessionForUser(user);
    }

    // PRD 1.2 Login & PRD 6.1/6.2 Secure Authentication and Session Management
    public AuthSessionResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        String computedHash = JwtTokenProvider.hashPassword(req.password(), user.getPasswordSalt());
        if (!computedHash.equals(user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return createSessionForUser(user);
    }

    // PRD 1.3 View Profile
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return toProfileResponse(user);
    }

    // PRD 1.3 Modify Profile Details
    public UserProfileResponse updateProfile(Long userId, ProfileUpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (req.fullName() != null && !req.fullName().isBlank()) {
            user.setFullName(req.fullName());
        }
        if (req.phone() != null) {
            user.setPhone(req.phone());
        }
        if (req.defaultAddress() != null) {
            user.setDefaultAddress(req.defaultAddress());
        }
        user.setUpdatedAt(Instant.now().toString());
        User saved = userRepository.save(user);
        return toProfileResponse(saved);
    }

    // PRD 1.4 Password Reset Request (generates secure link & publishes Kafka event)
    public Map<String, String> requestPasswordReset(PasswordResetRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResourceNotFoundException("No user found with email: " + req.email()));
        String token = "rst_" + UUID.randomUUID().toString().replace("-", "");
        String expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES).toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUserId(user.getId());
        resetToken.setEmail(user.getEmail());
        resetToken.setUsed(false);
        resetToken.setCreatedAt(Instant.now().toString());
        resetToken.setExpiresAt(expiresAt);
        resetTokenRepository.save(resetToken);

        String secureLink = "https://shop.ecommerce.example.com/reset-password?token=" + token;
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
                UUID.randomUUID().toString(),
                user.getId(),
                user.getEmail(),
                token,
                secureLink,
                expiresAt
        );
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, String.valueOf(user.getId()), event);

        return Map.of(
                "email", user.getEmail(),
                "resetToken", token,
                "secureResetLink", secureLink,
                "expiresAt", expiresAt
        );
    }

    // PRD 1.4 Password Reset Confirm
    public Map<String, String> confirmPasswordReset(PasswordResetConfirmRequest req) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(req.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));
        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }
        if (Instant.parse(resetToken.getExpiresAt()).isBefore(Instant.now())) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String newSalt = UUID.randomUUID().toString();
        user.setPasswordSalt(newSalt);
        user.setPasswordHash(JwtTokenProvider.hashPassword(req.newPassword(), newSalt));
        user.setUpdatedAt(Instant.now().toString());
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        return Map.of(
                "userId", String.valueOf(user.getId()),
                "email", user.getEmail(),
                "status", "PASSWORD_UPDATED_SUCCESSFULLY"
        );
    }

    // PRD 6.2 Session Management (Validate & Logout)
    public AuthSessionResponse validateSession(String bearerToken) {
        String token = stripBearer(bearerToken);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new InvalidTokenException("Session token is invalid or expired");
        }
        UserSession session = sessionRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.isActive()) {
            throw new InvalidTokenException("Session has been logged out");
        }
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new AuthSessionResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                session.getSessionId(),
                session.getToken(),
                "Bearer",
                session.getExpiresAt(),
                session.isActive()
        );
    }

    public Map<String, String> logout(String bearerToken) {
        String token = stripBearer(bearerToken);
        jwtTokenProvider.revokeToken(token);
        sessionRepository.findByToken(token).ifPresent(session -> {
            session.setActive(false);
            session.setLoggedOutAt(Instant.now().toString());
            sessionRepository.save(session);
        });
        return Map.of("status", "LOGGED_OUT", "timestamp", Instant.now().toString());
    }

    private AuthSessionResponse createSessionForUser(User user) {
        String sessionId = "sess_" + UUID.randomUUID().toString().substring(0, 12);
        String jwt = jwtTokenProvider.createToken(user.getId(), user.getEmail(), user.getRole(), sessionId);
        String now = Instant.now().toString();
        String expiresAt = Instant.now().plus(24, ChronoUnit.HOURS).toString();

        UserSession session = new UserSession();
        session.setSessionId(sessionId);
        session.setUserId(user.getId());
        session.setToken(jwt);
        session.setActive(true);
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);
        sessionRepository.save(session);

        return new AuthSessionResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                sessionId,
                jwt,
                "Bearer",
                expiresAt,
                true
        );
    }

    private static String stripBearer(String header) {
        if (header == null) return "";
        return header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
    }

    private static UserProfileResponse toProfileResponse(User u) {
        return new UserProfileResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getDefaultAddress(),
                u.getAuthProvider(),
                u.getRole(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}
