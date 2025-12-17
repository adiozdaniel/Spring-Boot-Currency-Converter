package com.example.authservice.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthEvent Tests")
class AuthEventTest {

    @Test
    @DisplayName("Should create event with default values")
    void shouldCreateEventWithDefaultValues() {
        AuthEvent event = new AuthEvent();

        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertFalse(event.getEventId().isEmpty());
    }

    @Test
    @DisplayName("Should build event using builder")
    void shouldBuildEventUsingBuilder() {
        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client123")
                .clientType("web")
                .ipAddress("192.168.1.1")
                .success(true)
                .build();

        assertEquals(AuthEventType.LOGIN_SUCCESS, event.getEventType());
        assertEquals("client123", event.getClientId());
        assertEquals("web", event.getClientType());
        assertEquals("192.168.1.1", event.getIpAddress());
        assertTrue(event.isSuccess());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
    }

    @Test
    @DisplayName("Should build failed event with reason")
    void shouldBuildFailedEventWithReason() {
        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId("client123")
                .ipAddress("192.168.1.1")
                .success(false)
                .failureReason("Invalid API key")
                .build();

        assertEquals(AuthEventType.LOGIN_FAILED, event.getEventType());
        assertFalse(event.isSuccess());
        assertEquals("Invalid API key", event.getFailureReason());
    }

    @Test
    @DisplayName("Should build event with metadata")
    void shouldBuildEventWithMetadata() {
        Map<String, Object> metadata = Map.of(
                "userAgent", "Mozilla/5.0",
                "requestId", "req-123"
        );

        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client123")
                .success(true)
                .metadata(metadata)
                .build();

        assertNotNull(event.getMetadata());
        assertEquals("Mozilla/5.0", event.getMetadata().get("userAgent"));
        assertEquals("req-123", event.getMetadata().get("requestId"));
    }

    @Test
    @DisplayName("Should set and get all properties")
    void shouldSetAndGetAllProperties() {
        AuthEvent event = new AuthEvent();
        Instant now = Instant.now();

        event.setEventId("event-123");
        event.setEventType(AuthEventType.RATE_LIMIT_EXCEEDED);
        event.setClientId("client456");
        event.setClientType("mobile");
        event.setIpAddress("10.0.0.1");
        event.setTimestamp(now);
        event.setSuccess(false);
        event.setFailureReason("Too many requests");
        event.setMetadata(Map.of("key", "value"));

        assertEquals("event-123", event.getEventId());
        assertEquals(AuthEventType.RATE_LIMIT_EXCEEDED, event.getEventType());
        assertEquals("client456", event.getClientId());
        assertEquals("mobile", event.getClientType());
        assertEquals("10.0.0.1", event.getIpAddress());
        assertEquals(now, event.getTimestamp());
        assertFalse(event.isSuccess());
        assertEquals("Too many requests", event.getFailureReason());
        assertEquals("value", event.getMetadata().get("key"));
    }

    @Test
    @DisplayName("Should generate unique event IDs")
    void shouldGenerateUniqueEventIds() {
        AuthEvent event1 = new AuthEvent();
        AuthEvent event2 = new AuthEvent();

        assertNotEquals(event1.getEventId(), event2.getEventId());
    }
}
