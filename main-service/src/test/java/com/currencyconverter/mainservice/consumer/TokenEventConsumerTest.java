package com.currencyconverter.mainservice.consumer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.currencyconverter.mainservice.event.TokenEvent;
import com.currencyconverter.mainservice.service.IdempotencyService;
import com.currencyconverter.mainservice.service.SessionManagementService;

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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-123", 0, 100L);

        // Assert
        verify(idempotencyService).checkAndMarkProcessed(event.getEventId());
        verify(sessionManagementService).handleTokenGenerated(event);
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-456", 1, 101L);

        // Assert
        verify(sessionManagementService).handleTokenGenerated(event);
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-789", 2, 102L);

        // Assert
        verify(sessionManagementService).handleTokenRevoked(event);
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-abc", 0, 103L);

        // Assert
        verify(sessionManagementService).handleTokenExpired(event);
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-def", 1, 104L);

        // Assert
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(true);

        // Act
        tokenEventConsumer.handleTokenEvent(event, "client-dup", 0, 105L);

        // Assert
        verify(idempotencyService).checkAndMarkProcessed(event.getEventId());
        verify(sessionManagementService, never()).handleTokenGenerated(any());
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);
        doThrow(new RuntimeException("Processing error")).when(sessionManagementService).handleTokenGenerated(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            tokenEventConsumer.handleTokenEvent(event, "client-error", 0, 106L);
        });
    }

    @Test
    void testHandleTokenEvent_NullEvent_ThrowsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                tokenEventConsumer.handleTokenEvent(null, "key", 0, 100L)
        );

        assertTrue(exception.getMessage().contains("null TokenEvent"));
        assertTrue(exception.getMessage().contains("partition=0"));
        assertTrue(exception.getMessage().contains("offset=100"));
    }

    @Test
    void testHandleTokenEvent_NullEventId_ThrowsIllegalArgumentException() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId(null)
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .tokenId("token-123")
                .clientId("client-123")
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                tokenEventConsumer.handleTokenEvent(event, "key", 1, 200L)
        );

        assertTrue(exception.getMessage().contains("missing eventId"));
        assertTrue(exception.getMessage().contains("partition=1"));
        assertTrue(exception.getMessage().contains("offset=200"));
    }

    @Test
    void testHandleTokenEvent_EmptyEventId_ThrowsIllegalArgumentException() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("   ")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .tokenId("token-456")
                .clientId("client-456")
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                tokenEventConsumer.handleTokenEvent(event, "key", 2, 300L)
        );

        assertTrue(exception.getMessage().contains("missing eventId"));
    }

    @Test
    void testHandleTokenEvent_NullEventType_ThrowsIllegalArgumentException() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("event-789")
                .eventType(null)
                .tokenId("token-789")
                .clientId("client-789")
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                tokenEventConsumer.handleTokenEvent(event, "key", 0, 400L)
        );

        assertTrue(exception.getMessage().contains("missing eventType"));
        assertTrue(exception.getMessage().contains("eventId=event-789"));
        assertTrue(exception.getMessage().contains("partition=0"));
    }

    @Test
    void testHandleTokenEvent_NullClientId_ThrowsIllegalArgumentException() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("event-abc")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .tokenId("token-abc")
                .clientId(null)
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                tokenEventConsumer.handleTokenEvent(event, "key", 1, 500L)
        );

        assertTrue(exception.getMessage().contains("missing clientId"));
        assertTrue(exception.getMessage().contains("eventId=event-abc"));
        assertTrue(exception.getMessage().contains("type=GENERATED"));
    }

    @Test
    void testHandleTokenEvent_EmptyClientId_ThrowsIllegalArgumentException() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("event-def")
                .eventType(TokenEvent.TokenEventType.REVOKED)
                .tokenId("token-def")
                .clientId("")
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                tokenEventConsumer.handleTokenEvent(event, "key", 2, 600L)
        );

        assertTrue(exception.getMessage().contains("missing clientId"));
        assertTrue(exception.getMessage().contains("eventId=event-def"));
    }

    @Test
    void testHandleTokenEvent_NullTokenId_ThrowsIllegalArgumentException() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("event-ghi")
                .eventType(TokenEvent.TokenEventType.EXPIRED)
                .tokenId(null)
                .clientId("client-ghi")
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                tokenEventConsumer.handleTokenEvent(event, "key", 0, 700L)
        );

        assertTrue(exception.getMessage().contains("missing tokenId"));
        assertTrue(exception.getMessage().contains("eventId=event-ghi"));
        assertTrue(exception.getMessage().contains("clientId=client-ghi"));
        assertTrue(exception.getMessage().contains("type=EXPIRED"));
    }

    @Test
    void testHandleTokenEvent_EmptyTokenId_ThrowsIllegalArgumentException() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("event-jkl")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .tokenId("  ")
                .clientId("client-jkl")
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                tokenEventConsumer.handleTokenEvent(event, "key", 1, 800L)
        );

        assertTrue(exception.getMessage().contains("missing tokenId"));
        assertTrue(exception.getMessage().contains("eventId=event-jkl"));
        assertTrue(exception.getMessage().contains("clientId=client-jkl"));
    }
}
