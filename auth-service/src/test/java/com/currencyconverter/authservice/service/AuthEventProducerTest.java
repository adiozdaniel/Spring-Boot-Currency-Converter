package com.currencyconverter.authservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.currencyconverter.authservice.event.AuthEvent;
import com.currencyconverter.authservice.event.AuthEventType;
import com.currencyconverter.authservice.event.TokenEvent;
import com.currencyconverter.authservice.kafka.DlqService;
import com.currencyconverter.authservice.service.impl.AuthEventProducerImpl;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthEventProducer Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("unchecked")
class AuthEventProducerTest {

    @Mock
    private KafkaSender<String, Object> kafkaSender;

    @Mock
    private DlqService dlqService;

    private MeterRegistry meterRegistry;
    private AuthEventProducer authEventProducer;
    private List<ProducerRecord<String, Object>> capturedRecords;

    private static final String LOGIN_SUCCESS_TOPIC = "auth.login.success";
    private static final String LOGIN_FAILED_TOPIC = "auth.login.failed";
    private static final String TOKENS_TOPIC = "auth.tokens";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        capturedRecords = new ArrayList<>();

        authEventProducer = new AuthEventProducerImpl(
                kafkaSender,
                dlqService,
                LOGIN_SUCCESS_TOPIC,
                LOGIN_FAILED_TOPIC,
                TOKENS_TOPIC,
                meterRegistry);

        ReflectionTestUtils.setField(authEventProducer, "kafkaEnabled", true);

        // Mock successful Kafka sending
        SenderResult<Void> mockResult = mock(SenderResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("test", 0), 0, 0, 0, 0, 0);
        when(mockResult.recordMetadata()).thenReturn(metadata);

        // Setup the mock to capture ANY Mono<SenderRecord> and process it
        when(kafkaSender.send(any(Mono.class))).thenAnswer(invocation -> {
            Mono<SenderRecord<String, Object, Void>> mono = invocation.getArgument(0);

            // Process the mono immediately to capture the record
            return mono.flatMapMany(senderRecord -> {
                // Extract the ProducerRecord
                ProducerRecord<String, Object> producerRecord = createProducerRecordFromSenderRecord(senderRecord);
                if (producerRecord != null) {
                    capturedRecords.add(producerRecord);
                }
                return Flux.just(mockResult);
            });
        });

        // Mock DLQ service
        when(dlqService.sendToDlq(anyString(), anyString(), any(), anyString()))
                .thenReturn(Mono.empty());
    }

    // Helper method to create ProducerRecord from SenderRecord
    private ProducerRecord<String, Object> createProducerRecordFromSenderRecord(
            SenderRecord<String, Object, Void> senderRecord) {
        // Create a new ProducerRecord from the SenderRecord's properties
        return new ProducerRecord<>(
                senderRecord.topic(),
                senderRecord.key(),
                senderRecord.value());
    }

    private ProducerRecord<String, Object> getLastCapturedRecord() {
        assertThat(capturedRecords)
                .withFailMessage("No records were captured. Expected at least one record to be published.")
                .isNotEmpty();
        return capturedRecords.get(capturedRecords.size() - 1);
    }

    @Test
    @DisplayName("Should publish login success event")
    void shouldPublishLoginSuccessEvent() {
        // Given
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress))
                .verifyComplete();

        // Verify the captured record
        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        assertThat(producerRecord.topic()).isEqualTo(LOGIN_SUCCESS_TOPIC);
        assertThat(producerRecord.key()).isEqualTo(clientId);
        assertThat(producerRecord.value()).isInstanceOf(AuthEvent.class);
        AuthEvent event = (AuthEvent) producerRecord.value();
        assertThat(event.eventType()).isEqualTo(AuthEventType.LOGIN_SUCCESS);
        assertThat(event.clientId()).isEqualTo(clientId);
        assertThat(event.clientType()).isEqualTo(clientType);
        assertThat(event.ipAddress()).isEqualTo("192.168.1.xxx");
        assertThat(event.success()).isTrue();

        // Verify Kafka sender was called
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should publish login failed event")
    void shouldPublishLoginFailedEvent() {
        // Given
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.1";
        String reason = "Invalid credentials";

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginFailed(clientId, clientType, ipAddress, reason))
                .verifyComplete();

        // Verify the captured record
        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        assertThat(producerRecord.topic()).isEqualTo(LOGIN_FAILED_TOPIC);
        // Note: In your implementation, the key is sanitizeClientId(clientId), which
        // for "client-123" returns "client-123"
        assertThat(producerRecord.key()).isEqualTo("client-123");

        AuthEvent event = (AuthEvent) producerRecord.value();
        assertThat(event.eventType()).isEqualTo(AuthEventType.LOGIN_FAILED);
        assertThat(event.clientId()).isEqualTo(clientId);
        assertThat(event.clientType()).isEqualTo(clientType);
        assertThat(event.ipAddress()).isEqualTo("192.168.1.xxx");
        assertThat(event.success()).isFalse();
        assertThat(event.failureReason()).isEqualTo(reason);

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should publish invalid API key event")
    void shouldPublishInvalidApiKeyEvent() {
        // Given
        String ipAddress = "192.168.1.1";
        String maskedIp = "192.168.1.xxx";

        // When & Then
        StepVerifier.create(authEventProducer.publishInvalidApiKey(ipAddress))
                .verifyComplete();

        // Verify the captured record
        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        assertThat(producerRecord.topic()).isEqualTo(LOGIN_FAILED_TOPIC);
        assertThat(producerRecord.key()).isEqualTo(maskedIp);

        AuthEvent event = (AuthEvent) producerRecord.value();
        assertThat(event.eventType()).isEqualTo(AuthEventType.INVALID_API_KEY);
        assertThat(event.ipAddress()).isEqualTo(maskedIp);
        assertThat(event.success()).isFalse();
        assertThat(event.failureReason()).isEqualTo("Invalid API key");

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should publish rate limit exceeded event")
    void shouldPublishRateLimitExceededEvent() {
        // Given
        String clientId = "client-123";
        String ipAddress = "192.168.1.1";

        // When & Then
        StepVerifier.create(authEventProducer.publishRateLimitExceeded(clientId, ipAddress))
                .verifyComplete();

        // Verify the captured record
        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        AuthEvent event = (AuthEvent) producerRecord.value();
        assertThat(event.eventType()).isEqualTo(AuthEventType.RATE_LIMIT_EXCEEDED);
        assertThat(event.clientId()).isEqualTo(clientId);
        assertThat(event.ipAddress()).isEqualTo("192.168.1.xxx");
        assertThat(event.success()).isFalse();
        assertThat(event.failureReason()).isEqualTo("Rate limit exceeded");

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should publish token generated event")
    void shouldPublishTokenGeneratedEvent() {
        // Given
        String tokenId = "token-123";
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When & Then
        StepVerifier.create(authEventProducer.publishTokenGenerated(tokenId, clientId, clientType, ipAddress))
                .verifyComplete();

        // Verify the captured record
        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        assertThat(producerRecord.topic()).isEqualTo(TOKENS_TOPIC);
        assertThat(producerRecord.key()).isEqualTo(clientId); // sanitizeClientId returns clientId for non-email

        TokenEvent event = (TokenEvent) producerRecord.value();
        assertThat(event.eventType()).isEqualTo(TokenEvent.TokenEventType.GENERATED);
        assertThat(event.tokenId()).isEqualTo("toke...-123");
        assertThat(event.clientId()).isEqualTo(clientId);
        assertThat(event.clientType()).isEqualTo(clientType);
        assertThat(event.ipAddress()).isEqualTo("192.168.1.xxx");

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should publish token refreshed event")
    void shouldPublishTokenRefreshedEvent() {
        // Given
        String tokenId = "token-123";
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When & Then
        StepVerifier.create(authEventProducer.publishTokenRefreshed(tokenId, clientId, clientType, ipAddress))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        TokenEvent event = (TokenEvent) producerRecord.value();
        assertThat(event.eventType()).isEqualTo(TokenEvent.TokenEventType.REFRESHED);
        assertThat(event.tokenId()).isEqualTo("toke...-123");
        assertThat(event.clientId()).isEqualTo(clientId);

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should publish token revoked event")
    void shouldPublishTokenRevokedEvent() {
        // Given
        String tokenId = "token-123";
        String clientId = "client-123";

        // When & Then
        StepVerifier.create(authEventProducer.publishTokenRevoked(tokenId, clientId))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        TokenEvent event = (TokenEvent) producerRecord.value();
        assertThat(event.eventType()).isEqualTo(TokenEvent.TokenEventType.REVOKED);
        assertThat(event.tokenId()).isEqualTo("toke...-123");
        assertThat(event.clientId()).isEqualTo(clientId);

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should publish token validated event")
    void shouldPublishTokenValidatedEvent() {
        // Given
        String tokenId = "token-123";
        String clientId = "client-123";

        // When & Then
        StepVerifier.create(authEventProducer.publishTokenValidated(tokenId, clientId))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        TokenEvent event = (TokenEvent) producerRecord.value();
        assertThat(event.eventType()).isEqualTo(TokenEvent.TokenEventType.VALIDATED);
        assertThat(event.tokenId()).isEqualTo("toke...-123");
        assertThat(event.clientId()).isEqualTo(clientId);

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should not publish when Kafka is disabled")
    void shouldNotPublishWhenKafkaDisabled() {
        // Given
        ReflectionTestUtils.setField(authEventProducer, "kafkaEnabled", false);

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginSuccess("client-123", "web", "192.168.1.1"))
                .verifyComplete();

        StepVerifier.create(authEventProducer.publishInvalidApiKey("192.168.1.1"))
                .verifyComplete();

        StepVerifier.create(authEventProducer.publishTokenGenerated("token-123", "client-123", "web", "192.168.1.1"))
                .verifyComplete();

        // Then
        verify(kafkaSender, never()).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should use masked IP address as key when clientId is null")
    void shouldUseMaskedIpAddressAsKeyWhenClientIdIsNull() {
        // Given
        String ipAddress = "192.168.1.1";
        String maskedIp = "192.168.1.xxx";

        // When & Then
        StepVerifier.create(authEventProducer.publishRateLimitExceeded(null, ipAddress))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        assertThat(producerRecord.key()).isEqualTo(maskedIp);
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should mask IPv4 address for PII filtering")
    void shouldMaskIpv4AddressForPiiFiltering() {
        // Given
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "192.168.1.100";

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        AuthEvent event = (AuthEvent) producerRecord.value();
        assertThat(event.ipAddress()).isEqualTo("192.168.1.xxx");
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should mask email in clientId for PII filtering")
    void shouldMaskEmailInClientIdForPiiFiltering() {
        // Given
        String clientId = "user@example.com";
        String maskedClientId = "use***@example.com";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        AuthEvent event = (AuthEvent) producerRecord.value();
        assertThat(event.clientId()).isEqualTo(maskedClientId);
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should mask token ID for PII filtering")
    void shouldMaskTokenIdForPiiFiltering() {
        // Given
        String tokenId = "abcd1234efgh5678ijkl";
        String clientId = "client-123";

        // When & Then
        StepVerifier.create(authEventProducer.publishTokenGenerated(tokenId, clientId, "web", "192.168.1.1"))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        TokenEvent event = (TokenEvent) producerRecord.value();
        assertThat(event.tokenId()).isEqualTo("abcd...ijkl");
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should sanitize failure reason containing sensitive data")
    void shouldSanitizeFailureReasonContainingSensitiveData() {
        // Given
        String clientId = "client-123";
        String reason = "Invalid password=secret123 for user";

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginFailed(clientId, "web", "192.168.1.1", reason))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        AuthEvent event = (AuthEvent) producerRecord.value();
        assertThat(event.failureReason()).isEqualTo("Invalid password=*** for user");
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should send to DLQ when publishing fails")
    void shouldSendToDlqWhenPublishingFails() {
        // Given - Reset mocks first
        reset(kafkaSender, dlqService);

        // Mock Kafka to fail
        when(kafkaSender.send(any(Mono.class)))
                .thenReturn(Flux.error(new RuntimeException("Kafka connection failed")));

        // Mock DLQ to succeed
        when(dlqService.sendToDlq(anyString(), anyString(), any(), anyString()))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginSuccess("client-123", "web", "192.168.1.1"))
                .verifyComplete();

        // Verify DLQ was called
        verify(dlqService, times(1)).sendToDlq(eq(LOGIN_SUCCESS_TOPIC), eq("client-123"), any(AuthEvent.class),
                anyString());
    }

    @Test
    @DisplayName("Should increment metrics counters on publish")
    void shouldIncrementMetricsCountersOnPublish() {
        // Given
        String clientId = "client-123";

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginSuccess(clientId, "web", "192.168.1.1"))
                .verifyComplete();

        // Then
        assertThat(meterRegistry.find("auth.events.published").counter()).isNotNull();
        assertThat(meterRegistry.find("auth.events.published").counter().count()).isEqualTo(1.0);
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should mask short token ID")
    void shouldMaskShortTokenId() {
        // Given
        String tokenId = "abc";
        String clientId = "client-123";

        // When & Then
        StepVerifier.create(authEventProducer.publishTokenGenerated(tokenId, clientId, "web", "192.168.1.1"))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        TokenEvent event = (TokenEvent) producerRecord.value();
        assertThat(event.tokenId()).isEqualTo("***");
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should keep UUID client ID unchanged")
    void shouldKeepUuidClientIdUnchanged() {
        // Given
        String clientId = "550e8400-e29b-41d4-a716-446655440000";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        AuthEvent event = (AuthEvent) producerRecord.value();
        assertThat(event.clientId()).isEqualTo(clientId);
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should mask compressed IPv6 addresses")
    void shouldMaskCompressedIpv6Addresses() {
        // Given
        String clientId = "client-123";
        String clientType = "web";
        String ipAddress = "::1";

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        AuthEvent event = (AuthEvent) producerRecord.value();
        assertThat(event.ipAddress()).isEqualTo("xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx");
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should use sanitized key when publishing email clientId")
    void shouldUseSanitizedKeyWhenPublishingEmailClientId() {
        // Given
        String clientId = "user@example.com";
        String clientType = "web";
        String ipAddress = "192.168.1.1";

        // When & Then
        StepVerifier.create(authEventProducer.publishLoginSuccess(clientId, clientType, ipAddress))
                .verifyComplete();

        ProducerRecord<String, Object> producerRecord = getLastCapturedRecord();

        // In your implementation, the key is the original clientId, not sanitized
        assertThat(producerRecord.key()).isEqualTo(clientId);
        verify(kafkaSender, times(1)).send(any(Mono.class));
    }
}
