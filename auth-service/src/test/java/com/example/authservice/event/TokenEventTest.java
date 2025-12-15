package com.example.authservice.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenEvent Tests")
class TokenEventTest {

    @Test
    @DisplayName("Should create event with default values")
    void shouldCreateEventWithDefaultValues() {
        TokenEvent event = new TokenEvent();

        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
        assertFalse(event.getEventId().isEmpty());
    }

    @Test
    @DisplayName("Should build token generated event")
    void shouldBuildTokenGeneratedEvent() {
        Instant expiry = Instant.now().plusSeconds(3600);

        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .tokenId("token-123")
                .clientId("client123")
                .clientType("web")
                .ipAddress("192.168.1.1")
                .tokenExpiry(expiry)
                .build();

        assertEquals(TokenEvent.TokenEventType.GENERATED, event.getEventType());
        assertEquals("token-123", event.getTokenId());
        assertEquals("client123", event.getClientId());
        assertEquals("web", event.getClientType());
        assertEquals("192.168.1.1", event.getIpAddress());
        assertEquals(expiry, event.getTokenExpiry());
    }

    @Test
    @DisplayName("Should build token refreshed event")
    void shouldBuildTokenRefreshedEvent() {
        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.REFRESHED)
                .tokenId("new-token-456")
                .clientId("client123")
                .clientType("mobile")
                .build();

        assertEquals(TokenEvent.TokenEventType.REFRESHED, event.getEventType());
        assertEquals("new-token-456", event.getTokenId());
    }

    @Test
    @DisplayName("Should build token revoked event")
    void shouldBuildTokenRevokedEvent() {
        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.REVOKED)
                .tokenId("revoked-token-789")
                .clientId("client123")
                .build();

        assertEquals(TokenEvent.TokenEventType.REVOKED, event.getEventType());
        assertEquals("revoked-token-789", event.getTokenId());
        assertEquals("client123", event.getClientId());
    }

    @Test
    @DisplayName("Should set and get all properties")
    void shouldSetAndGetAllProperties() {
        TokenEvent event = new TokenEvent();
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(7200);

        event.setEventId("event-abc");
        event.setEventType(TokenEvent.TokenEventType.VALIDATED);
        event.setTokenId("token-xyz");
        event.setClientId("client789");
        event.setClientType("platform");
        event.setTimestamp(now);
        event.setTokenExpiry(expiry);
        event.setIpAddress("172.16.0.1");

        assertEquals("event-abc", event.getEventId());
        assertEquals(TokenEvent.TokenEventType.VALIDATED, event.getEventType());
        assertEquals("token-xyz", event.getTokenId());
        assertEquals("client789", event.getClientId());
        assertEquals("platform", event.getClientType());
        assertEquals(now, event.getTimestamp());
        assertEquals(expiry, event.getTokenExpiry());
        assertEquals("172.16.0.1", event.getIpAddress());
    }

    @Test
    @DisplayName("Should generate unique event IDs")
    void shouldGenerateUniqueEventIds() {
        TokenEvent event1 = new TokenEvent();
        TokenEvent event2 = new TokenEvent();

        assertNotEquals(event1.getEventId(), event2.getEventId());
    }

    @Test
    @DisplayName("Should have all token event types")
    void shouldHaveAllTokenEventTypes() {
        TokenEvent.TokenEventType[] types = TokenEvent.TokenEventType.values();

        assertEquals(5, types.length);
        assertNotNull(TokenEvent.TokenEventType.valueOf("GENERATED"));
        assertNotNull(TokenEvent.TokenEventType.valueOf("REFRESHED"));
        assertNotNull(TokenEvent.TokenEventType.valueOf("REVOKED"));
        assertNotNull(TokenEvent.TokenEventType.valueOf("VALIDATED"));
        assertNotNull(TokenEvent.TokenEventType.valueOf("EXPIRED"));
    }
}
