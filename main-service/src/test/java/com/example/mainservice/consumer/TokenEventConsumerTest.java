package com.example.mainservice.consumer;

import com.example.mainservice.event.TokenEvent;
import com.example.mainservice.service.IdempotencyService;
import com.example.mainservice.service.SessionManagementService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenEventConsumerTest {

    @Mock
    private SessionManagementService sessionManagementService;

    @Mock
    private IdempotencyService idempotencyService;

    private SimpleMeterRegistry meterRegistry;
    private TokenEventConsumer tokenEventConsumer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        tokenEventConsumer = new TokenEventConsumer(
                sessionManagementService,
                idempotencyService,
                meterRegistry
        );
    }

    @Test
    void testHandleTokenEvent_Generated() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("token-1")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .clientId("client-123")
                .tokenId("token-abc")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-123", 0, 100L);

        // Assert
        verify(idempotencyService).isEventProcessed(event.getEventId());
        verify(sessionManagementService).handleTokenGenerated(event);
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
    }

    @Test
    void testHandleTokenEvent_Refreshed() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("token-2")
                .eventType(TokenEvent.TokenEventType.REFRESHED)
                .clientId("client-456")
                .tokenId("token-def")
                .clientType("mobile")
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-456", 1, 101L);

        // Assert
        verify(sessionManagementService).handleTokenGenerated(event);
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
    }

    @Test
    void testHandleTokenEvent_Revoked() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("token-3")
                .eventType(TokenEvent.TokenEventType.REVOKED)
                .clientId("client-789")
                .tokenId("token-ghi")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-789", 2, 102L);

        // Assert
        verify(sessionManagementService).handleTokenRevoked(event);
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
    }

    @Test
    void testHandleTokenEvent_Expired() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("token-4")
                .eventType(TokenEvent.TokenEventType.EXPIRED)
                .clientId("client-abc")
                .tokenId("token-jkl")
                .clientType("mobile")
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-abc", 0, 103L);

        // Assert
        verify(sessionManagementService).handleTokenExpired(event);
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
    }

    @Test
    void testHandleTokenEvent_Validated() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("token-5")
                .eventType(TokenEvent.TokenEventType.VALIDATED)
                .clientId("client-def")
                .tokenId("token-mno")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-def", 1, 104L);

        // Assert
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
        // Validated events don't trigger session management
        verify(sessionManagementService, never()).handleTokenGenerated(any());
        verify(sessionManagementService, never()).handleTokenRevoked(any());
        verify(sessionManagementService, never()).handleTokenExpired(any());
    }

    @Test
    void testHandleTokenEvent_DuplicateEvent_SkipsProcessing() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("duplicate-token")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .clientId("client-dup")
                .tokenId("token-dup")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(true);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-dup", 0, 105L);

        // Assert
        verify(idempotencyService).isEventProcessed(event.getEventId());
        verify(sessionManagementService, never()).handleTokenGenerated(any());
        verify(idempotencyService, never()).markEventAsProcessed(any());
    }

    @Test
    void testHandleTokenEvent_ThrowsException_PropagatesError() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("error-token")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .clientId("client-error")
                .tokenId("token-error")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);
        doThrow(new RuntimeException("Processing error")).when(sessionManagementService).handleTokenGenerated(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            tokenEventConsumer.handleTokenEvent(event, "client-error", 0, 106L);
        });

        verify(idempotencyService, never()).markEventAsProcessed(any());
    }
}
