package com.currencyconverter.mainservice.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import com.currencyconverter.mainservice.event.AuthEvent;
import com.currencyconverter.mainservice.event.AuthEventType;
import com.currencyconverter.mainservice.event.TokenEvent;
import com.currencyconverter.mainservice.model.UserSession;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionManagementService Tests")
class SessionManagementServiceTest {

    @Mock
    private SecurityAuditService securityAuditService;

    private CacheManager cacheManager;
    private SimpleMeterRegistry meterRegistry;
    private SessionManagementService sessionManagementService;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager("userSessions", "processedEvents");
        meterRegistry = new SimpleMeterRegistry();
        sessionManagementService = new SessionManagementService(
                cacheManager,
                securityAuditService,
                meterRegistry
        );

        // Use ReflectionTestUtils to set @Value fields
        ReflectionTestUtils.setField(sessionManagementService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(sessionManagementService, "lockDurationSeconds", 300L);
        ReflectionTestUtils.setField(sessionManagementService, "alertThreshold", 3);
    }

    @Test
    @DisplayName("Should create new session on successful login")
    void testHandleLoginSuccess_CreatesNewSession() {
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

        // Act
        sessionManagementService.handleLoginSuccess(event);

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession("client-123");
        assertTrue(session.isPresent());
        assertEquals("client-123", session.get().getClientId());
        assertEquals("web", session.get().getClientType());
        assertEquals("192.168.1.1", session.get().getLastLoginIp());
        assertEquals(0, session.get().getFailedLoginAttempts());
        assertNotNull(session.get().getLastLoginTime());
        verify(securityAuditService).logAuthentication(event);
    }

    @Test
    @DisplayName("Should increment failed attempts on login failure")
    void testHandleLoginFailure_IncrementsFailedAttempts() {
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

        // Act
        sessionManagementService.handleLoginFailure(event);

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession("client-456");
        assertTrue(session.isPresent());
        assertEquals(1, session.get().getFailedLoginAttempts());
        assertNotNull(session.get().getLastFailedLoginTime());
        verify(securityAuditService).logAuthentication(event);
    }

    @Test
    @DisplayName("Should lock account after max failed attempts")
    void testHandleLoginFailure_LocksAccountAfterMaxAttempts() {
        // Arrange
        String clientId = "client-789";
        AuthEvent event = AuthEvent.builder()
                .eventId("event-3")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId(clientId)
                .clientType("web")
                .ipAddress("192.168.1.3")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Invalid credentials")
                .build();

        // Act - fail 5 times
        for (int i = 0; i < 5; i++) {
            event.setEventId("event-" + i);
            sessionManagementService.handleLoginFailure(event);
        }

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession(clientId);
        assertTrue(session.isPresent());
        assertEquals(5, session.get().getFailedLoginAttempts());
        assertTrue(session.get().isLocked());
        assertNotNull(session.get().getLockExpiry());
        verify(securityAuditService, times(5)).logAuthentication(any(AuthEvent.class));
        verify(securityAuditService, atLeastOnce()).sendSecurityAlert(any(AuthEvent.class), any(UserSession.class));
    }

    @Test
    @DisplayName("Should send security alert when threshold exceeded")
    void testHandleLoginFailure_SendsAlertAtThreshold() {
        // Arrange
        String clientId = "client-alert";
        AuthEvent event = AuthEvent.builder()
                .eventId("event-alert")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId(clientId)
                .clientType("web")
                .ipAddress("192.168.1.4")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Invalid password")
                .build();

        // Act - fail 3 times (alert threshold)
        for (int i = 0; i < 3; i++) {
            event.setEventId("event-alert-" + i);
            sessionManagementService.handleLoginFailure(event);
        }

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession(clientId);
        assertTrue(session.isPresent());
        assertEquals(3, session.get().getFailedLoginAttempts());
        assertFalse(session.get().isLocked()); // Not locked yet, just alerted
        verify(securityAuditService, times(3)).logAuthentication(any(AuthEvent.class));
        verify(securityAuditService, atLeastOnce()).sendSecurityAlert(any(AuthEvent.class), any(UserSession.class));
    }

    @Test
    @DisplayName("Should reset failed attempts on successful login")
    void testHandleLoginSuccess_ResetsFailedAttempts() {
        // Arrange
        String clientId = "client-reset";
        AuthEvent failEvent = AuthEvent.builder()
                .eventId("fail-1")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId(clientId)
                .clientType("web")
                .ipAddress("192.168.1.5")
                .timestamp(Instant.now())
                .success(false)
                .build();

        AuthEvent successEvent = AuthEvent.builder()
                .eventId("success-1")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId(clientId)
                .clientType("web")
                .ipAddress("192.168.1.5")
                .timestamp(Instant.now())
                .success(true)
                .build();

        // Act - fail twice, then succeed
        sessionManagementService.handleLoginFailure(failEvent);
        failEvent.setEventId("fail-2");
        sessionManagementService.handleLoginFailure(failEvent);
        sessionManagementService.handleLoginSuccess(successEvent);

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession(clientId);
        assertTrue(session.isPresent());
        assertEquals(0, session.get().getFailedLoginAttempts());
        assertNull(session.get().getLastFailedLoginTime());
    }

    @Test
    @DisplayName("Should add token to session on token generation")
    void testHandleTokenGenerated_AddsTokenToSession() {
        // Arrange
        TokenEvent event = TokenEvent.builder()
                .eventId("token-1")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .clientId("client-token")
                .tokenId("token-123")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        // Act
        sessionManagementService.handleTokenGenerated(event);

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession("client-token");
        assertTrue(session.isPresent());
        assertTrue(session.get().getActiveTokens().contains("token-123"));
        assertEquals("web", session.get().getClientType());
    }

    @Test
    @DisplayName("Should handle multiple token generation")
    void testHandleTokenGenerated_HandlesMultipleTokens() {
        // Arrange
        String clientId = "client-multi-token";
        TokenEvent event1 = TokenEvent.builder()
                .eventId("token-1")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .clientId(clientId)
                .tokenId("token-abc")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        TokenEvent event2 = TokenEvent.builder()
                .eventId("token-2")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .clientId(clientId)
                .tokenId("token-def")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        // Act
        sessionManagementService.handleTokenGenerated(event1);
        sessionManagementService.handleTokenGenerated(event2);

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession(clientId);
        assertTrue(session.isPresent());
        assertEquals(2, session.get().getActiveTokens().size());
        assertTrue(session.get().getActiveTokens().contains("token-abc"));
        assertTrue(session.get().getActiveTokens().contains("token-def"));
    }

    @Test
    @DisplayName("Should remove token from session on token revocation")
    void testHandleTokenRevoked_RemovesTokenFromSession() {
        // Arrange
        String clientId = "client-revoke";
        TokenEvent generateEvent = TokenEvent.builder()
                .eventId("token-gen")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .clientId(clientId)
                .tokenId("token-456")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        TokenEvent revokeEvent = TokenEvent.builder()
                .eventId("token-rev")
                .eventType(TokenEvent.TokenEventType.REVOKED)
                .clientId(clientId)
                .tokenId("token-456")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        // Act
        sessionManagementService.handleTokenGenerated(generateEvent);
        sessionManagementService.handleTokenRevoked(revokeEvent);

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession(clientId);
        assertTrue(session.isPresent());
        assertFalse(session.get().getActiveTokens().contains("token-456"));
    }

    @Test
    @DisplayName("Should remove token from session on token expiration")
    void testHandleTokenExpired_RemovesTokenFromSession() {
        // Arrange
        String clientId = "client-expired";
        TokenEvent generateEvent = TokenEvent.builder()
                .eventId("token-gen")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .clientId(clientId)
                .tokenId("token-789")
                .clientType("mobile")
                .timestamp(Instant.now())
                .build();

        TokenEvent expireEvent = TokenEvent.builder()
                .eventId("token-exp")
                .eventType(TokenEvent.TokenEventType.EXPIRED)
                .clientId(clientId)
                .tokenId("token-789")
                .clientType("mobile")
                .timestamp(Instant.now())
                .build();

        // Act
        sessionManagementService.handleTokenGenerated(generateEvent);
        sessionManagementService.handleTokenExpired(expireEvent);

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession(clientId);
        assertTrue(session.isPresent());
        assertFalse(session.get().getActiveTokens().contains("token-789"));
    }

    @Test
    @DisplayName("Should return true for locked session")
    void testIsSessionLocked_ReturnsTrueForLockedSession() {
        // Arrange
        String clientId = "client-locked";
        AuthEvent event = AuthEvent.builder()
                .eventId("event-lock")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId(clientId)
                .clientType("web")
                .ipAddress("192.168.1.6")
                .timestamp(Instant.now())
                .success(false)
                .build();

        // Act - fail 5 times to lock
        for (int i = 0; i < 5; i++) {
            event.setEventId("event-lock-" + i);
            sessionManagementService.handleLoginFailure(event);
        }

        // Assert
        assertTrue(sessionManagementService.isSessionLocked(clientId));
    }

    @Test
    @DisplayName("Should return false for unlocked session")
    void testIsSessionLocked_ReturnsFalseForUnlockedSession() {
        // Arrange
        String clientId = "client-unlocked";

        // Assert
        assertFalse(sessionManagementService.isSessionLocked(clientId));
    }

    @Test
    @DisplayName("Should return false for non-existent session")
    void testIsSessionLocked_ReturnsFalseForNonExistentSession() {
        // Act & Assert
        assertFalse(sessionManagementService.isSessionLocked("non-existent-client"));
    }

    @Test
    @DisplayName("Should return empty optional for non-existent session")
    void testGetSession_ReturnsEmptyForNonExistentSession() {
        // Act
        Optional<UserSession> session = sessionManagementService.getSession("non-existent");

        // Assert
        assertFalse(session.isPresent());
    }

    @Test
    @DisplayName("Should unlock expired lock automatically")
    void testIsSessionLocked_UnlocksExpiredLock() {
        // Arrange
        String clientId = "client-auto-unlock";

        // Set lock duration to 0 so it's immediately expired
        ReflectionTestUtils.setField(sessionManagementService, "lockDurationSeconds", 0L);

        AuthEvent event = AuthEvent.builder()
                .eventId("event-unlock")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId(clientId)
                .clientType("web")
                .ipAddress("192.168.1.7")
                .timestamp(Instant.now())
                .success(false)
                .build();

        // Act - fail 5 times to lock
        for (int i = 0; i < 5; i++) {
            event.setEventId("event-unlock-" + i);
            sessionManagementService.handleLoginFailure(event);
        }

        // Verify account was locked
        Optional<UserSession> lockedSession = sessionManagementService.getSession(clientId);
        assertTrue(lockedSession.isPresent());
        assertTrue(lockedSession.get().isLocked());

        // Assert - lock should be auto-unlocked when checked (since duration is 0)
        assertFalse(sessionManagementService.isSessionLocked(clientId));

        // Verify session is now marked as unlocked
        Optional<UserSession> unlockedSession = sessionManagementService.getSession(clientId);
        assertTrue(unlockedSession.isPresent());
        assertFalse(unlockedSession.get().isLocked());
    }

    @Test
    @DisplayName("Should unlock account on successful login after lock expiry")
    void testHandleLoginSuccess_UnlocksExpiredAccount() {
        // Arrange
        String clientId = "client-unlock-success";

        // Set short lock duration
        ReflectionTestUtils.setField(sessionManagementService, "lockDurationSeconds", 0L);

        AuthEvent failEvent = AuthEvent.builder()
                .eventId("fail")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId(clientId)
                .clientType("web")
                .ipAddress("192.168.1.8")
                .timestamp(Instant.now())
                .success(false)
                .build();

        AuthEvent successEvent = AuthEvent.builder()
                .eventId("success")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId(clientId)
                .clientType("web")
                .ipAddress("192.168.1.8")
                .timestamp(Instant.now())
                .success(true)
                .build();

        // Act - lock account
        for (int i = 0; i < 5; i++) {
            failEvent.setEventId("fail-" + i);
            sessionManagementService.handleLoginFailure(failEvent);
        }

        // Then succeed (lock is already expired due to 0 duration)
        sessionManagementService.handleLoginSuccess(successEvent);

        // Assert
        Optional<UserSession> session = sessionManagementService.getSession(clientId);
        assertTrue(session.isPresent());
        assertFalse(session.get().isLocked());
        assertEquals(0, session.get().getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should track metrics correctly")
    void testMetrics_AreTrackedCorrectly() {
        // Arrange
        AuthEvent successEvent = AuthEvent.builder()
                .eventId("metrics-1")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("metrics-client")
                .clientType("web")
                .ipAddress("192.168.1.9")
                .timestamp(Instant.now())
                .success(true)
                .build();

        AuthEvent failEvent = AuthEvent.builder()
                .eventId("metrics-2")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId("metrics-client-2")
                .clientType("web")
                .ipAddress("192.168.1.10")
                .timestamp(Instant.now())
                .success(false)
                .build();

        TokenEvent tokenEvent = TokenEvent.builder()
                .eventId("metrics-token")
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .clientId("metrics-client-3")
                .tokenId("token-metrics")
                .clientType("web")
                .timestamp(Instant.now())
                .build();

        // Act
        sessionManagementService.handleLoginSuccess(successEvent);
        sessionManagementService.handleLoginFailure(failEvent);
        sessionManagementService.handleTokenGenerated(tokenEvent);

        // Assert
        assertEquals(1.0, meterRegistry.counter("auth.login.success").count());
        assertEquals(1.0, meterRegistry.counter("auth.login.failed").count());
        assertEquals(1.0, meterRegistry.counter("auth.token.generated").count());
    }
}
