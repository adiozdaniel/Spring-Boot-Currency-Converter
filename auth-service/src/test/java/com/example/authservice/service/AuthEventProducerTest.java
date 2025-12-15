package com.example.authservice.service;

import com.example.authservice.event.AuthEvent;
import com.example.authservice.event.AuthEventType;
import com.example.authservice.event.TokenEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthEventProducer Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MeterRegistry meterRegistry;
    private AuthEventProducer authEventProducer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        authEventProducer = new AuthEventProducer(kafkaTemplate, meterRegistry);

        // Set topic names via reflection
        ReflectionTestUtils.setField(authEventProducer, "authEventsTopic", "auth.events");
        ReflectionTestUtils.setField(authEventProducer, "loginSuccessTopic", "auth.login.success");
        ReflectionTestUtils.setField(authEventProducer, "loginFailedTopic", "auth.login.failed");
        ReflectionTestUtils.setField(authEventProducer, "tokensTopic", "auth.tokens");
        ReflectionTestUtils.setField(authEventProducer, "dlqTopic", "auth.dlq");
        ReflectionTestUtils.setField(authEventProducer, "kafkaEnabled", true);

        // Mock kafka template to return completed future
        @SuppressWarnings("unchecked")
        SendResult<String, Object> mockSendResult = mock(SendResult.class);
        org.apache.kafka.clients.producer.RecordMetadata mockMetadata =
            new org.apache.kafka.clients.producer.RecordMetadata(
                new org.apache.kafka.common.TopicPartition("test", 0), 0, 0, 0, 0, 0);
        when(mockSendResult.getRecordMetadata()).thenReturn(mockMetadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mockSendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        when(kafkaTemplate.send(anyString(), isNull(), any())).thenReturn(future);
    }

    @Test
    @DisplayName("Should publish login success event")
    void shouldPublishLoginSuccessEvent() {
        // Given
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When
        authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress);

        // Then - key is now sanitized (client-123 has no @ so remains unchanged)
        verify(kafkaTemplate, times(2)).send(anyString(), eq(clientId), any(AuthEvent.class));

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);

        verify(kafkaTemplate, atLeastOnce()).send(topicCaptor.capture(), eq(clientId), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(AuthEventType.LOGIN_SUCCESS);
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
        assertThat(capturedEvent.getClientType()).isEqualTo(clientType);
        assertThat(capturedEvent.getIpAddress()).isEqualTo("192.168.1.xxx"); // PII masked
        assertThat(capturedEvent.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should publish login failed event")
    void shouldPublishLoginFailedEvent() {
        // Given
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.1";
        String reason = "Invalid credentials";

        // When
        authEventProducer.publishLoginFailed(clientId, clientType, ipAddress, reason);

        // Then
        verify(kafkaTemplate, times(2)).send(anyString(), eq(clientId), any(AuthEvent.class));

        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), eq(clientId), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(AuthEventType.LOGIN_FAILED);
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
        assertThat(capturedEvent.getClientType()).isEqualTo(clientType);
        assertThat(capturedEvent.getIpAddress()).isEqualTo("192.168.1.xxx"); // PII masked
        assertThat(capturedEvent.isSuccess()).isFalse();
        assertThat(capturedEvent.getFailureReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("Should publish invalid API key event")
    void shouldPublishInvalidApiKeyEvent() {
        // Given
        String ipAddress = "192.168.1.1";
        String maskedIp = "192.168.1.xxx";

        // When
        authEventProducer.publishInvalidApiKey(ipAddress);

        // Then - key is now masked IP
        verify(kafkaTemplate, times(2)).send(anyString(), eq(maskedIp), any(AuthEvent.class));

        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), eq(maskedIp), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(AuthEventType.INVALID_API_KEY);
        assertThat(capturedEvent.getIpAddress()).isEqualTo("192.168.1.xxx"); // PII masked
        assertThat(capturedEvent.isSuccess()).isFalse();
        assertThat(capturedEvent.getFailureReason()).isEqualTo("Invalid API key");
    }

    @Test
    @DisplayName("Should publish rate limit exceeded event")
    void shouldPublishRateLimitExceededEvent() {
        // Given
        String clientId = "client-123";
        String ipAddress = "192.168.1.1";

        // When
        authEventProducer.publishRateLimitExceeded(clientId, ipAddress);

        // Then
        verify(kafkaTemplate, times(2)).send(anyString(), eq(clientId), any(AuthEvent.class));

        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), eq(clientId), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(AuthEventType.RATE_LIMIT_EXCEEDED);
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
        assertThat(capturedEvent.getIpAddress()).isEqualTo("192.168.1.xxx"); // PII masked
        assertThat(capturedEvent.isSuccess()).isFalse();
        assertThat(capturedEvent.getFailureReason()).isEqualTo("Rate limit exceeded");
    }

    @Test
    @DisplayName("Should publish token generated event")
    void shouldPublishTokenGeneratedEvent() {
        // Given
        String tokenId = "token-123";
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When
        authEventProducer.publishTokenGenerated(tokenId, clientId, clientType, ipAddress);

        // Then
        verify(kafkaTemplate).send(eq("auth.tokens"), eq(clientId), any(TokenEvent.class));

        ArgumentCaptor<TokenEvent> eventCaptor = ArgumentCaptor.forClass(TokenEvent.class);
        verify(kafkaTemplate).send(anyString(), eq(clientId), eventCaptor.capture());

        TokenEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(TokenEvent.TokenEventType.GENERATED);
        assertThat(capturedEvent.getTokenId()).isEqualTo("toke...-123"); // Token masked
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
        assertThat(capturedEvent.getClientType()).isEqualTo(clientType);
        assertThat(capturedEvent.getIpAddress()).isEqualTo("192.168.1.xxx"); // IP masked
    }

    @Test
    @DisplayName("Should publish token refreshed event")
    void shouldPublishTokenRefreshedEvent() {
        // Given
        String tokenId = "token-123";
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When
        authEventProducer.publishTokenRefreshed(tokenId, clientId, clientType, ipAddress);

        // Then
        verify(kafkaTemplate).send(eq("auth.tokens"), eq(clientId), any(TokenEvent.class));

        ArgumentCaptor<TokenEvent> eventCaptor = ArgumentCaptor.forClass(TokenEvent.class);
        verify(kafkaTemplate).send(anyString(), eq(clientId), eventCaptor.capture());

        TokenEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(TokenEvent.TokenEventType.REFRESHED);
        assertThat(capturedEvent.getTokenId()).isEqualTo("toke...-123"); // Token masked
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
    }

    @Test
    @DisplayName("Should publish token revoked event")
    void shouldPublishTokenRevokedEvent() {
        // Given
        String tokenId = "token-123";
        String clientId = "client-123";

        // When
        authEventProducer.publishTokenRevoked(tokenId, clientId);

        // Then
        verify(kafkaTemplate).send(eq("auth.tokens"), eq(clientId), any(TokenEvent.class));

        ArgumentCaptor<TokenEvent> eventCaptor = ArgumentCaptor.forClass(TokenEvent.class);
        verify(kafkaTemplate).send(anyString(), eq(clientId), eventCaptor.capture());

        TokenEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(TokenEvent.TokenEventType.REVOKED);
        assertThat(capturedEvent.getTokenId()).isEqualTo("toke...-123"); // Token masked
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
    }

    @Test
    @DisplayName("Should publish token validated event")
    void shouldPublishTokenValidatedEvent() {
        // Given
        String tokenId = "token-123";
        String clientId = "client-123";

        // When
        authEventProducer.publishTokenValidated(tokenId, clientId);

        // Then
        verify(kafkaTemplate).send(eq("auth.tokens"), eq(clientId), any(TokenEvent.class));

        ArgumentCaptor<TokenEvent> eventCaptor = ArgumentCaptor.forClass(TokenEvent.class);
        verify(kafkaTemplate).send(anyString(), eq(clientId), eventCaptor.capture());

        TokenEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(TokenEvent.TokenEventType.VALIDATED);
        assertThat(capturedEvent.getTokenId()).isEqualTo("toke...-123"); // Token masked
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
    }

    @Test
    @DisplayName("Should publish auth event to multiple topics")
    void shouldPublishAuthEventToMultipleTopics() {
        // Given
        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId("client-123")
                .ipAddress("192.168.1.1")
                .success(true)
                .build();

        // When
        authEventProducer.publishAuthEvent(event);

        // Then - verify sanitized event is sent (not the original event)
        verify(kafkaTemplate, times(2)).send(anyString(), eq("client-123"), any(AuthEvent.class));
    }

    @Test
    @DisplayName("Should not publish when Kafka is disabled")
    void shouldNotPublishWhenKafkaDisabled() {
        // Given
        ReflectionTestUtils.setField(authEventProducer, "kafkaEnabled", false);

        // When
        authEventProducer.publishLoginSuccess("client-123", "web", "192.168.1.1");
        authEventProducer.publishInvalidApiKey("192.168.1.1");
        authEventProducer.publishTokenGenerated("token-123", "client-123", "web", "192.168.1.1");

        // Then
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should use masked IP address as key when clientId is null")
    void shouldUseMaskedIpAddressAsKeyWhenClientIdIsNull() {
        // Given
        String ipAddress = "192.168.1.1";
        String maskedIp = "192.168.1.xxx";

        // When
        authEventProducer.publishRateLimitExceeded(null, ipAddress);

        // Then - key is now masked IP
        verify(kafkaTemplate, times(2)).send(anyString(), eq(maskedIp), any(AuthEvent.class));
    }

    @Test
    @DisplayName("Should mask IPv4 address for PII filtering")
    void shouldMaskIpv4AddressForPiiFiltering() {
        // Given
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.100";

        // When
        authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress);

        // Then
        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), eq(clientId), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getIpAddress()).isEqualTo("192.168.1.xxx");
    }

    @Test
    @DisplayName("Should mask email in clientId for PII filtering")
    void shouldMaskEmailInClientIdForPiiFiltering() {
        // Given
        String clientId = "user@example.com";
        String maskedClientId = "use***@example.com";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When
        authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress);

        // Then - key is also masked
        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), eq(maskedClientId), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getClientId()).isEqualTo("use***@example.com");
    }

    @Test
    @DisplayName("Should mask token ID for PII filtering")
    void shouldMaskTokenIdForPiiFiltering() {
        // Given
        String tokenId = "abcd1234efgh5678ijkl";
        String clientId = "client-123";

        // When
        authEventProducer.publishTokenGenerated(tokenId, clientId, "web", "192.168.1.1");

        // Then
        ArgumentCaptor<TokenEvent> eventCaptor = ArgumentCaptor.forClass(TokenEvent.class);
        verify(kafkaTemplate).send(anyString(), eq(clientId), eventCaptor.capture());

        TokenEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getTokenId()).isEqualTo("abcd...ijkl");
    }

    @Test
    @DisplayName("Should sanitize failure reason containing sensitive data")
    void shouldSanitizeFailureReasonContainingSensitiveData() {
        // Given
        String clientId = "client-123";
        String reason = "Invalid password=secret123 for user";

        // When
        authEventProducer.publishLoginFailed(clientId, "web", "192.168.1.1", reason);

        // Then
        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), eq(clientId), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getFailureReason()).isEqualTo("Invalid password=*** for user");
    }

    @Test
    @DisplayName("Should handle null values gracefully in PII filtering")
    void shouldHandleNullValuesGracefullyInPiiFiltering() {
        // Given
        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId(null)
                .ipAddress(null)
                .success(false)
                .failureReason(null)
                .build();

        // When
        authEventProducer.publishAuthEvent(event);

        // Then
        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), isNull(), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getClientId()).isNull();
        assertThat(capturedEvent.getIpAddress()).isNull();
        assertThat(capturedEvent.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("Should send to DLQ when publishing fails")
    void shouldSendToDlqWhenPublishingFails() {
        // Given
        ReflectionTestUtils.setField(authEventProducer, "dlqTopic", "auth.dlq");

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka connection failed"));

        when(kafkaTemplate.send(eq("auth.login.success"), anyString(), any()))
                .thenReturn(failedFuture);
        when(kafkaTemplate.send(eq("auth.events"), anyString(), any()))
                .thenReturn(failedFuture);
        when(kafkaTemplate.send(eq("auth.dlq"), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        authEventProducer.publishLoginSuccess("client-123", "web", "192.168.1.1");

        // Then - verify DLQ was called after failure
        verify(kafkaTemplate, timeout(1000).atLeastOnce()).send(eq("auth.dlq"), anyString(), any(AuthEventProducer.DlqEvent.class));
    }

    @Test
    @DisplayName("Should increment metrics counters on publish")
    void shouldIncrementMetricsCountersOnPublish() {
        // Given
        String clientId = "client-123";

        // When
        authEventProducer.publishLoginSuccess(clientId, "web", "192.168.1.1");

        // Then
        assertThat(meterRegistry.find("auth.events.published").counter()).isNotNull();
    }

    @Test
    @DisplayName("Should mask short token ID")
    void shouldMaskShortTokenId() {
        // Given
        String tokenId = "abc";
        String clientId = "client-123";

        // When
        authEventProducer.publishTokenGenerated(tokenId, clientId, "web", "192.168.1.1");

        // Then
        ArgumentCaptor<TokenEvent> eventCaptor = ArgumentCaptor.forClass(TokenEvent.class);
        verify(kafkaTemplate).send(anyString(), eq(clientId), eventCaptor.capture());

        TokenEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getTokenId()).isEqualTo("***");
    }

    @Test
    @DisplayName("Should keep UUID client ID unchanged")
    void shouldKeepUuidClientIdUnchanged() {
        // Given
        String clientId = "550e8400-e29b-41d4-a716-446655440000";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When
        authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress);

        // Then
        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), eq(clientId), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
    }

    @Test
    @DisplayName("Should mask compressed IPv6 addresses")
    void shouldMaskCompressedIpv6Addresses() {
        // Given
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "::1"; // Compressed IPv6 loopback

        // When
        authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress);

        // Then
        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), eq(clientId), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getIpAddress()).isEqualTo("xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx");
    }

    @Test
    @DisplayName("Should use sanitized key when publishing email clientId")
    void shouldUseSanitizedKeyWhenPublishingEmailClientId() {
        // Given
        String clientId = "user@example.com";
        String maskedClientId = "use***@example.com";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When
        authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress);

        // Then - key should be sanitized
        verify(kafkaTemplate, times(2)).send(anyString(), eq(maskedClientId), any(AuthEvent.class));
    }
}
