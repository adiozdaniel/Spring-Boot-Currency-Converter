package com.example.mainservice.consumer;

import com.example.mainservice.event.AuthEvent;
import com.example.mainservice.event.AuthEventType;
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
class AuthEventConsumerTest {

    @Mock
    private SessionManagementService sessionManagementService;

    @Mock
    private IdempotencyService idempotencyService;

    private SimpleMeterRegistry meterRegistry;
    private AuthEventConsumer authEventConsumer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        authEventConsumer = new AuthEventConsumer(
                sessionManagementService,
                idempotencyService,
                meterRegistry
        );
    }

    @Test
    void testHandleAuthEvent_LoginSuccess() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("event-1")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-123")
                .clientType("web")
                .ipAddress("192.168.1.1")
                .timestamp(Instant.now())
                .success(true)
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleAuthEvent(event, "client-123", 0, 100L);

        // Assert
        verify(idempotencyService).isEventProcessed(event.getEventId());
        verify(sessionManagementService).handleLoginSuccess(event);
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
    }

    @Test
    void testHandleAuthEvent_LoginFailed() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("event-2")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId("client-456")
                .clientType("mobile")
                .ipAddress("192.168.1.2")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Invalid credentials")
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleAuthEvent(event, "client-456", 0, 101L);

        // Assert
        verify(idempotencyService).isEventProcessed(event.getEventId());
        verify(sessionManagementService).handleLoginFailure(event);
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
    }

    @Test
    void testHandleAuthEvent_DuplicateEvent_SkipsProcessing() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("duplicate-event")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-789")
                .clientType("web")
                .ipAddress("192.168.1.3")
                .timestamp(Instant.now())
                .success(true)
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(true);

        // Act
        authEventConsumer.handleAuthEvent(event, "client-789", 0, 102L);

        // Assert
        verify(idempotencyService).isEventProcessed(event.getEventId());
        verify(sessionManagementService, never()).handleLoginSuccess(any());
        verify(sessionManagementService, never()).handleLoginFailure(any());
        verify(idempotencyService, never()).markEventAsProcessed(any());
    }

    @Test
    void testHandleLoginSuccess_ProcessesEvent() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("login-success-1")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-abc")
                .clientType("web")
                .ipAddress("192.168.1.4")
                .timestamp(Instant.now())
                .success(true)
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleLoginSuccess(event, "client-abc", 1, 200L);

        // Assert
        verify(sessionManagementService).handleLoginSuccess(event);
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
    }

    @Test
    void testHandleLoginFailure_ProcessesEvent() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("login-failed-1")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId("client-def")
                .clientType("mobile")
                .ipAddress("192.168.1.5")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Invalid password")
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleLoginFailure(event, "client-def", 2, 300L);

        // Assert
        verify(sessionManagementService).handleLoginFailure(event);
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
    }

    @Test
    void testHandleAuthEvent_RateLimitExceeded() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("rate-limit-1")
                .eventType(AuthEventType.RATE_LIMIT_EXCEEDED)
                .clientId("client-rate")
                .clientType("web")
                .ipAddress("192.168.1.6")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Rate limit exceeded")
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleAuthEvent(event, "client-rate", 0, 103L);

        // Assert
        verify(sessionManagementService).handleLoginFailure(event);
        verify(idempotencyService).markEventAsProcessed(event.getEventId());
    }

    @Test
    void testHandleAuthEvent_ThrowsException_PropagatesError() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("error-event")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-error")
                .clientType("web")
                .ipAddress("192.168.1.7")
                .timestamp(Instant.now())
                .success(true)
                .build();

        when(idempotencyService.isEventProcessed(event.getEventId())).thenReturn(false);
        doThrow(new RuntimeException("Processing error")).when(sessionManagementService).handleLoginSuccess(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authEventConsumer.handleAuthEvent(event, "client-error", 0, 104L);
        });

        verify(idempotencyService, never()).markEventAsProcessed(any());
    }
}
