package com.ecommerce.user.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id
    private String sessionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1024)
    private String token;

    private boolean active;
    private String createdAt;
    private String expiresAt;
    private String loggedOutAt;

    public UserSession() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public String getLoggedOutAt() { return loggedOutAt; }
    public void setLoggedOutAt(String loggedOutAt) { this.loggedOutAt = loggedOutAt; }
}
