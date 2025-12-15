package com.example.authservice.service;

import com.example.authservice.event.AuthEvent;
import com.example.authservice.event.AuthEventType;
import com.example.authservice.event.TokenEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AuthEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(AuthEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter eventsPublishedCounter;
    private final Counter eventsFailedCounter;

    @Value("${kafka.topics.auth-events.name:auth.events}")
    private String authEventsTopic;

    @Value("${kafka.topics.auth-login-success.name:auth.login.success}")
    private String loginSuccessTopic;

    @Value("${kafka.topics.auth-login-failed.name:auth.login.failed}")
    private String loginFailedTopic;

    @Value("${kafka.topics.auth-tokens.name:auth.tokens}")
    private String tokensTopic;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    public AuthEventProducer(KafkaTemplate<String, Object> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventsPublishedCounter = Counter.builder("auth.events.published")
                .description("Number of authentication events published to Kafka")
                .register(meterRegistry);
        this.eventsFailedCounter = Counter.builder("auth.events.failed")
                .description("Number of authentication events that failed to publish")
                .register(meterRegistry);
    }

    public void publishLoginSuccess(String clientId, String clientType, String ipAddress) {
        if (!kafkaEnabled) {
            logger.debug("Kafka disabled, skipping login success event");
            return;
        }

        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId(clientId)
                .clientType(clientType)
                .ipAddress(ipAddress)
                .success(true)
                .build();

        sendToTopic(loginSuccessTopic, clientId, event);
        sendToTopic(authEventsTopic, clientId, event);
    }

    public void publishLoginFailed(String clientId, String clientType, String ipAddress, String reason) {
        if (!kafkaEnabled) {
            logger.debug("Kafka disabled, skipping login failed event");
            return;
        }

        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId(clientId)
                .clientType(clientType)
                .ipAddress(ipAddress)
                .success(false)
                .failureReason(reason)
                .build();

        sendToTopic(loginFailedTopic, clientId != null ? clientId : ipAddress, event);
        sendToTopic(authEventsTopic, clientId != null ? clientId : ipAddress, event);
    }

    public void publishInvalidApiKey(String ipAddress) {
        if (!kafkaEnabled) {
            return;
        }

        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.INVALID_API_KEY)
                .ipAddress(ipAddress)
                .success(false)
                .failureReason("Invalid API key")
                .build();

        sendToTopic(loginFailedTopic, ipAddress, event);
        sendToTopic(authEventsTopic, ipAddress, event);
    }

    public void publishRateLimitExceeded(String clientId, String ipAddress) {
        if (!kafkaEnabled) {
            return;
        }

        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.RATE_LIMIT_EXCEEDED)
                .clientId(clientId)
                .ipAddress(ipAddress)
                .success(false)
                .failureReason("Rate limit exceeded")
                .build();

        sendToTopic(loginFailedTopic, clientId != null ? clientId : ipAddress, event);
        sendToTopic(authEventsTopic, clientId != null ? clientId : ipAddress, event);
    }

    public void publishTokenGenerated(String tokenId, String clientId, String clientType, String ipAddress) {
        if (!kafkaEnabled) {
            return;
        }

        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .tokenId(tokenId)
                .clientId(clientId)
                .clientType(clientType)
                .ipAddress(ipAddress)
                .build();

        sendToTopic(tokensTopic, clientId, event);
    }

    public void publishTokenRefreshed(String tokenId, String clientId, String clientType, String ipAddress) {
        if (!kafkaEnabled) {
            return;
        }

        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.REFRESHED)
                .tokenId(tokenId)
                .clientId(clientId)
                .clientType(clientType)
                .ipAddress(ipAddress)
                .build();

        sendToTopic(tokensTopic, clientId, event);
    }

    public void publishTokenRevoked(String tokenId, String clientId) {
        if (!kafkaEnabled) {
            return;
        }

        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.REVOKED)
                .tokenId(tokenId)
                .clientId(clientId)
                .build();

        sendToTopic(tokensTopic, clientId, event);
    }

    public void publishTokenValidated(String tokenId, String clientId) {
        if (!kafkaEnabled) {
            return;
        }

        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.VALIDATED)
                .tokenId(tokenId)
                .clientId(clientId)
                .build();

        sendToTopic(tokensTopic, clientId, event);
    }

    public void publishAuthEvent(AuthEvent event) {
        if (!kafkaEnabled) {
            return;
        }

        String key = event.getClientId() != null ? event.getClientId() : event.getIpAddress();
        sendToTopic(authEventsTopic, key, event);

        if (event.isSuccess()) {
            sendToTopic(loginSuccessTopic, key, event);
        } else {
            sendToTopic(loginFailedTopic, key, event);
        }
    }

    private void sendToTopic(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.debug("Event sent to topic {} with key {}: offset={}",
                        topic, key, result.getRecordMetadata().offset());
                eventsPublishedCounter.increment();
            } else {
                logger.error("Failed to send event to topic {} with key {}: {}",
                        topic, key, ex.getMessage());
                eventsFailedCounter.increment();
            }
        });
    }
}
