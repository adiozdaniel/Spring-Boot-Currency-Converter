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
        ReflectionTestUtils.setField(authEventProducer, "kafkaEnabled", true);

        // Mock kafka template to return completed future
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
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

        // Then
        verify(kafkaTemplate, times(2)).send(anyString(), eq(clientId), any(AuthEvent.class));

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);

        verify(kafkaTemplate, atLeastOnce()).send(topicCaptor.capture(), eq(clientId), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(AuthEventType.LOGIN_SUCCESS);
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
        assertThat(capturedEvent.getClientType()).isEqualTo(clientType);
        assertThat(capturedEvent.getIpAddress()).isEqualTo(ipAddress);
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
        assertThat(capturedEvent.getIpAddress()).isEqualTo(ipAddress);
        assertThat(capturedEvent.isSuccess()).isFalse();
        assertThat(capturedEvent.getFailureReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("Should publish invalid API key event")
    void shouldPublishInvalidApiKeyEvent() {
        // Given
        String ipAddress = "192.168.1.1";

        // When
        authEventProducer.publishInvalidApiKey(ipAddress);

        // Then
        verify(kafkaTemplate, times(2)).send(anyString(), eq(ipAddress), any(AuthEvent.class));

        ArgumentCaptor<AuthEvent> eventCaptor = ArgumentCaptor.forClass(AuthEvent.class);
        verify(kafkaTemplate, atLeastOnce()).send(anyString(), eq(ipAddress), eventCaptor.capture());

        AuthEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo(AuthEventType.INVALID_API_KEY);
        assertThat(capturedEvent.getIpAddress()).isEqualTo(ipAddress);
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
        assertThat(capturedEvent.getIpAddress()).isEqualTo(ipAddress);
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
        assertThat(capturedEvent.getTokenId()).isEqualTo(tokenId);
        assertThat(capturedEvent.getClientId()).isEqualTo(clientId);
        assertThat(capturedEvent.getClientType()).isEqualTo(clientType);
        assertThat(capturedEvent.getIpAddress()).isEqualTo(ipAddress);
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
        assertThat(capturedEvent.getTokenId()).isEqualTo(tokenId);
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
        assertThat(capturedEvent.getTokenId()).isEqualTo(tokenId);
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
        assertThat(capturedEvent.getTokenId()).isEqualTo(tokenId);
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

        // Then
        verify(kafkaTemplate, times(2)).send(anyString(), eq("client-123"), eq(event));
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
    @DisplayName("Should use IP address as key when clientId is null")
    void shouldUseIpAddressAsKeyWhenClientIdIsNull() {
        // Given
        String ipAddress = "192.168.1.1";

        // When
        authEventProducer.publishRateLimitExceeded(null, ipAddress);

        // Then
        verify(kafkaTemplate, times(2)).send(anyString(), eq(ipAddress), any(AuthEvent.class));
    }
}
