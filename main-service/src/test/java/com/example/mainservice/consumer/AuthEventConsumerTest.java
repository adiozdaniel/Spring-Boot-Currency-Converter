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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleAuthEvent(event, "client-123", 0, 100L);

        // Assert
        verify(idempotencyService).checkAndMarkProcessed(event.getEventId());
        verify(sessionManagementService).handleLoginSuccess(event);
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleAuthEvent(event, "client-456", 0, 101L);

        // Assert
        verify(idempotencyService).checkAndMarkProcessed(event.getEventId());
        verify(sessionManagementService).handleLoginFailure(event);
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(true);

        // Act
        authEventConsumer.handleAuthEvent(event, "client-789", 0, 102L);

        // Assert
        verify(idempotencyService).checkAndMarkProcessed(event.getEventId());
        verify(sessionManagementService, never()).handleLoginSuccess(any());
        verify(sessionManagementService, never()).handleLoginFailure(any());
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleLoginSuccess(event, "client-abc", 1, 200L);

        // Assert
        verify(sessionManagementService).handleLoginSuccess(event);
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleLoginFailure(event, "client-def", 2, 300L);

        // Assert
        verify(sessionManagementService).handleLoginFailure(event);
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);

        // Act
        authEventConsumer.handleAuthEvent(event, "client-rate", 0, 103L);

        // Assert
        verify(sessionManagementService).handleLoginFailure(event);
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

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);
        doThrow(new RuntimeException("Processing error")).when(sessionManagementService).handleLoginSuccess(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authEventConsumer.handleAuthEvent(event, "client-error", 0, 104L);
        });
    }

    @Test
    void testHandleLoginSuccess_DuplicateEvent_SkipsProcessing() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("duplicate-login-success")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-dup")
                .clientType("web")
                .ipAddress("192.168.1.10")
                .timestamp(Instant.now())
                .success(true)
                .build();

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(true);

        // Act
        authEventConsumer.handleLoginSuccess(event, "client-dup", 1, 201L);

        // Assert
        verify(idempotencyService).checkAndMarkProcessed(event.getEventId());
        verify(sessionManagementService, never()).handleLoginSuccess(any());
    }

    @Test
    void testHandleLoginSuccess_ThrowsException_PropagatesError() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("error-login-success")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-err")
                .clientType("web")
                .ipAddress("192.168.1.11")
                .timestamp(Instant.now())
                .success(true)
                .build();

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);
        doThrow(new RuntimeException("Login success processing error")).when(sessionManagementService).handleLoginSuccess(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authEventConsumer.handleLoginSuccess(event, "client-err", 1, 202L);
        });
    }

    @Test
    void testHandleLoginFailure_DuplicateEvent_SkipsProcessing() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("duplicate-login-failure")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId("client-dup-fail")
                .clientType("mobile")
                .ipAddress("192.168.1.12")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Invalid credentials")
                .build();

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(true);

        // Act
        authEventConsumer.handleLoginFailure(event, "client-dup-fail", 2, 301L);

        // Assert
        verify(idempotencyService).checkAndMarkProcessed(event.getEventId());
        verify(sessionManagementService, never()).handleLoginFailure(any());
    }

    @Test
    void testHandleLoginFailure_ThrowsException_PropagatesError() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("error-login-failure")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId("client-err-fail")
                .clientType("mobile")
                .ipAddress("192.168.1.13")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Invalid password")
                .build();

        when(idempotencyService.checkAndMarkProcessed(event.getEventId())).thenReturn(false);
        doThrow(new RuntimeException("Login failure processing error")).when(sessionManagementService).handleLoginFailure(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authEventConsumer.handleLoginFailure(event, "client-err-fail", 2, 302L);
        });
    }

    @Test
    void testHandleAuthEvent_NullEvent_ThrowsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authEventConsumer.handleAuthEvent(null, "client-null", 0, 500L);
        });

        assertTrue(exception.getMessage().contains("null AuthEvent"));
        assertTrue(exception.getMessage().contains("partition=0"));
        assertTrue(exception.getMessage().contains("offset=500"));
    }

    @Test
    void testHandleAuthEvent_MissingEventId_ThrowsIllegalArgumentException() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-no-id")
                .clientType("web")
                .ipAddress("192.168.1.14")
                .timestamp(Instant.now())
                .success(true)
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authEventConsumer.handleAuthEvent(event, "client-no-id", 0, 501L);
        });

        assertTrue(exception.getMessage().contains("missing eventId"));
    }

    @Test
    void testHandleAuthEvent_EmptyEventId_ThrowsIllegalArgumentException() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("   ")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-empty-id")
                .clientType("web")
                .ipAddress("192.168.1.15")
                .timestamp(Instant.now())
                .success(true)
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authEventConsumer.handleAuthEvent(event, "client-empty-id", 0, 502L);
        });

        assertTrue(exception.getMessage().contains("missing eventId"));
    }

    @Test
    void testHandleAuthEvent_MissingEventType_ThrowsIllegalArgumentException() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("event-no-type")
                .clientId("client-no-type")
                .clientType("web")
                .ipAddress("192.168.1.16")
                .timestamp(Instant.now())
                .success(true)
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authEventConsumer.handleAuthEvent(event, "client-no-type", 0, 503L);
        });

        assertTrue(exception.getMessage().contains("missing eventType"));
    }

    @Test
    void testHandleAuthEvent_MissingClientId_ThrowsIllegalArgumentException() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("event-no-client")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientType("web")
                .ipAddress("192.168.1.17")
                .timestamp(Instant.now())
                .success(true)
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authEventConsumer.handleAuthEvent(event, "key", 0, 504L);
        });

        assertTrue(exception.getMessage().contains("missing clientId"));
    }

    @Test
    void testHandleAuthEvent_EmptyClientId_ThrowsIllegalArgumentException() {
        // Arrange
        AuthEvent event = AuthEvent.builder()
                .eventId("event-empty-client")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("  ")
                .clientType("web")
                .ipAddress("192.168.1.18")
                .timestamp(Instant.now())
                .success(true)
                .build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authEventConsumer.handleAuthEvent(event, "key", 0, 505L);
        });

        assertTrue(exception.getMessage().contains("missing clientId"));
    }

    @Test
    void testHandleLoginSuccess_NullEvent_ThrowsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authEventConsumer.handleLoginSuccess(null, "client-null", 1, 600L);
        });

        assertTrue(exception.getMessage().contains("null AuthEvent"));
    }

    @Test
    void testHandleLoginFailure_NullEvent_ThrowsIllegalArgumentException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authEventConsumer.handleLoginFailure(null, "client-null", 2, 700L);
        });

        assertTrue(exception.getMessage().contains("null AuthEvent"));
    }
}
