package com.example.mainservice.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.mainservice.event.AuthEvent;
import com.example.mainservice.event.AuthEventType;
import com.example.mainservice.model.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityAuditService Tests")
class SecurityAuditServiceTest {

    private SecurityAuditService securityAuditService;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        securityAuditService = new SecurityAuditService();

        logger = (Logger) LoggerFactory.getLogger(SecurityAuditService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("Should log successful authentication event")
    void shouldLogSuccessfulAuthenticationEvent() {
        // Given
        AuthEvent event = AuthEvent.builder()
                .eventId("event-123")
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-456")
                .ipAddress("192.168.1.1")
                .timestamp(Instant.now())
                .success(true)
                .build();

        // When
        securityAuditService.logAuthentication(event);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);

        ILoggingEvent logEvent = logsList.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage()).contains("Auth Event");
        assertThat(logEvent.getFormattedMessage()).contains("LOGIN_SUCCESS");
        assertThat(logEvent.getFormattedMessage()).contains("client-456");
        assertThat(logEvent.getFormattedMessage()).contains("true");
        assertThat(logEvent.getFormattedMessage()).contains("192.168.1.1");
    }

    @Test
    @DisplayName("Should log failed authentication event with warning")
    void shouldLogFailedAuthenticationEventWithWarning() {
        // Given
        AuthEvent event = AuthEvent.builder()
                .eventId("event-789")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId("client-999")
                .ipAddress("10.0.0.1")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Invalid credentials")
                .build();

        // When
        securityAuditService.logAuthentication(event);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(2);

        // First log - INFO level
        ILoggingEvent infoLog = logsList.get(0);
        assertThat(infoLog.getLevel()).isEqualTo(Level.INFO);
        assertThat(infoLog.getFormattedMessage()).contains("Auth Event");
        assertThat(infoLog.getFormattedMessage()).contains("false");

        // Second log - WARN level for failed attempt
        ILoggingEvent warnLog = logsList.get(1);
        assertThat(warnLog.getLevel()).isEqualTo(Level.WARN);
        assertThat(warnLog.getFormattedMessage()).contains("Failed authentication attempt");
        assertThat(warnLog.getFormattedMessage()).contains("client-999");
        assertThat(warnLog.getFormattedMessage()).contains("Invalid credentials");
        assertThat(warnLog.getFormattedMessage()).contains("10.0.0.1");
    }

    @Test
    @DisplayName("Should send security alert with error log")
    void shouldSendSecurityAlertWithErrorLog() {
        // Given
        AuthEvent event = AuthEvent.builder()
                .eventId("event-alert")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId("suspicious-client")
                .ipAddress("203.0.113.5")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Account locked")
                .build();

        UserSession session = UserSession.builder()
                .clientId("suspicious-client")
                .failedLoginAttempts(5)
                .locked(true)
                .build();

        // When
        securityAuditService.sendSecurityAlert(event, session);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);

        ILoggingEvent errorLog = logsList.get(0);
        assertThat(errorLog.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorLog.getFormattedMessage()).contains("SECURITY ALERT");
        assertThat(errorLog.getFormattedMessage()).contains("suspicious-client");
        assertThat(errorLog.getFormattedMessage()).contains("5");
        assertThat(errorLog.getFormattedMessage()).contains("true");
        assertThat(errorLog.getFormattedMessage()).contains("203.0.113.5");
        assertThat(errorLog.getFormattedMessage()).contains("Account locked");
    }

    @Test
    @DisplayName("Should log token event")
    void shouldLogTokenEvent() {
        // Given
        String eventType = "TOKEN_GENERATED";
        String clientId = "token-client-123";
        String tokenId = "token-id-abc";

        // When
        securityAuditService.logTokenEvent(eventType, clientId, tokenId);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);

        ILoggingEvent logEvent = logsList.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage()).contains("Token Event");
        assertThat(logEvent.getFormattedMessage()).contains("TOKEN_GENERATED");
        assertThat(logEvent.getFormattedMessage()).contains("token-client-123");
        assertThat(logEvent.getFormattedMessage()).contains("token-id-abc");
    }

    @Test
    @DisplayName("Should log token revocation event")
    void shouldLogTokenRevocationEvent() {
        // Given
        String eventType = "TOKEN_REVOKED";
        String clientId = "client-xyz";
        String tokenId = "revoked-token-456";

        // When
        securityAuditService.logTokenEvent(eventType, clientId, tokenId);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);

        ILoggingEvent logEvent = logsList.get(0);
        assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(logEvent.getFormattedMessage()).contains("TOKEN_REVOKED");
        assertThat(logEvent.getFormattedMessage()).contains("client-xyz");
        assertThat(logEvent.getFormattedMessage()).contains("revoked-token-456");
    }

    @Test
    @DisplayName("Should send security alert for unlocked account with failed attempts")
    void shouldSendSecurityAlertForUnlockedAccountWithFailedAttempts() {
        // Given
        AuthEvent event = AuthEvent.builder()
                .eventId("event-warning")
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId("warning-client")
                .ipAddress("172.16.0.100")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Wrong password")
                .build();

        UserSession session = UserSession.builder()
                .clientId("warning-client")
                .failedLoginAttempts(3)
                .locked(false)
                .build();

        // When
        securityAuditService.sendSecurityAlert(event, session);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);

        ILoggingEvent errorLog = logsList.get(0);
        assertThat(errorLog.getLevel()).isEqualTo(Level.ERROR);
        assertThat(errorLog.getFormattedMessage()).contains("SECURITY ALERT");
        assertThat(errorLog.getFormattedMessage()).contains("warning-client");
        assertThat(errorLog.getFormattedMessage()).contains("3");
        assertThat(errorLog.getFormattedMessage()).contains("false");
    }

    @Test
    @DisplayName("Should log authentication event with rate limit exceeded")
    void shouldLogAuthenticationEventWithRateLimitExceeded() {
        // Given
        AuthEvent event = AuthEvent.builder()
                .eventId("event-rate-limit")
                .eventType(AuthEventType.RATE_LIMIT_EXCEEDED)
                .clientId("rate-limited-client")
                .ipAddress("192.168.100.50")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Rate limit exceeded")
                .build();

        // When
        securityAuditService.logAuthentication(event);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(2);

        ILoggingEvent infoLog = logsList.get(0);
        assertThat(infoLog.getFormattedMessage()).contains("RATE_LIMIT_EXCEEDED");

        ILoggingEvent warnLog = logsList.get(1);
        assertThat(warnLog.getLevel()).isEqualTo(Level.WARN);
        assertThat(warnLog.getFormattedMessage()).contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("Should log authentication event with invalid API key")
    void shouldLogAuthenticationEventWithInvalidApiKey() {
        // Given
        AuthEvent event = AuthEvent.builder()
                .eventId("event-invalid-key")
                .eventType(AuthEventType.INVALID_API_KEY)
                .clientId("unknown-client")
                .ipAddress("8.8.8.8")
                .timestamp(Instant.now())
                .success(false)
                .failureReason("Invalid API key")
                .build();

        // When
        securityAuditService.logAuthentication(event);

        // Then
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(2);

        ILoggingEvent warnLog = logsList.get(1);
        assertThat(warnLog.getLevel()).isEqualTo(Level.WARN);
        assertThat(warnLog.getFormattedMessage()).contains("Invalid API key");
        assertThat(warnLog.getFormattedMessage()).contains("8.8.8.8");
    }
}
