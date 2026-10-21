package com.ecommerce.payment.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized error response structure for payment service.
 */
public class ErrorResponse {
    private boolean success;
    private String message;
    private String error;
    private String path;
    private Map<String, Object> details;

    public ErrorResponse() {
        this.success = false;
        this.details = new HashMap<>();
        this.details.put("timestamp", Instant.now().toString());
    }

    public ErrorResponse(String message, String error, String path) {
        this();
        this.message = message;
        this.error = error;
        this.path = path;
    }

    public ErrorResponse(String message, String error, String path, Map<String, Object> additionalDetails) {
        this(message, error, path);
        if (additionalDetails != null) {
            this.details.putAll(additionalDetails);
        }
    }

    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
