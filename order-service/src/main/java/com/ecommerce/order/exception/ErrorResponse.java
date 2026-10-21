package com.ecommerce.order.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ErrorResponse {
    private boolean success;
    private String message;
    private String error;
    private Map<String, Object> details;
    private String timestamp;
    private String path;

    public ErrorResponse() {
        this.success = false;
        this.timestamp = Instant.now().toString();
        this.details = new HashMap<>();
    }

    public ErrorResponse(String message, String error) {
        this();
        this.message = message;
        this.error = error;
    }

    public ErrorResponse(String message, String error, String path) {
        this(message, error);
        this.path = path;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public ErrorResponse addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
