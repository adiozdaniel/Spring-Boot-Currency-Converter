package com.example.authservice.event;

import java.time.Instant;
import java.util.UUID;

public class TokenEvent {

    private String eventId;
    private TokenEventType eventType;
    private String tokenId;
    private String clientId;
    private String clientType;
    private Instant timestamp;
    private Instant tokenExpiry;
    private String ipAddress;

    public TokenEvent() {
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

    public TokenEventType getEventType() {
        return eventType;
    }

    public void setEventType(TokenEventType eventType) {
        this.eventType = eventType;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Instant getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(Instant tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public enum TokenEventType {
        GENERATED,
        REFRESHED,
        REVOKED,
        VALIDATED,
        EXPIRED
    }

    public static class Builder {
        private final TokenEvent event = new TokenEvent();

        public Builder eventType(TokenEventType eventType) {
            event.eventType = eventType;
            return this;
        }

        public Builder tokenId(String tokenId) {
            event.tokenId = tokenId;
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

        public Builder tokenExpiry(Instant tokenExpiry) {
            event.tokenExpiry = tokenExpiry;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            event.ipAddress = ipAddress;
            return this;
        }

        public TokenEvent build() {
            return event;
        }
    }
}
