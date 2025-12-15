package com.example.authservice.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AuthEvent {

    private String eventId;
    private AuthEventType eventType;
    private String clientId;
    private String clientType;
    private String ipAddress;
    private Instant timestamp;
    private boolean success;
    private String failureReason;
    private Map<String, Object> metadata;

    public AuthEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public AuthEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuthEventType eventType) {
        this.eventType = eventType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class Builder {
        private final AuthEvent event = new AuthEvent();

        public Builder eventType(AuthEventType eventType) {
            event.eventType = eventType;
            return this;
        }

        public Builder clientId(String clientId) {
            event.clientId = clientId;
            return this;
        }

        public Builder clientType(String clientType) {
            event.clientType = clientType;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            event.ipAddress = ipAddress;
            return this;
        }

        public Builder success(boolean success) {
            event.success = success;
            return this;
        }

        public Builder failureReason(String failureReason) {
            event.failureReason = failureReason;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            event.metadata = metadata;
            return this;
        }

        public AuthEvent build() {
            return event;
        }
    }
}
