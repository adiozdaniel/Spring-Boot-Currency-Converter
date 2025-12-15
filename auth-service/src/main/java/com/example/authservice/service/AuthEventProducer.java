package com.example.authservice.service;

import com.example.authservice.event.AuthEvent;
import com.example.authservice.event.AuthEventType;
import com.example.authservice.event.TokenEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AuthEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(AuthEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter eventsPublishedCounter;
    private final Counter eventsFailedCounter;
    private final Counter eventsDlqCounter;
    private final Timer publishLatencyTimer;

    @Value("${kafka.topics.auth-events.name:auth.events}")
    private String authEventsTopic;

    @Value("${kafka.topics.auth-login-success.name:auth.login.success}")
    private String loginSuccessTopic;

    @Value("${kafka.topics.auth-login-failed.name:auth.login.failed}")
    private String loginFailedTopic;

    @Value("${kafka.topics.auth-tokens.name:auth.tokens}")
    private String tokensTopic;

    @Value("${kafka.topics.auth-dlq.name:auth.dlq}")
    private String dlqTopic;

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
        this.eventsDlqCounter = Counter.builder("auth.events.dlq")
                .description("Number of authentication events sent to DLQ")
                .register(meterRegistry);
        this.publishLatencyTimer = Timer.builder("auth.events.publish.latency")
                .description("Latency of publishing authentication events to Kafka")
                .register(meterRegistry);
    }

    @Async("kafkaAsyncExecutor")
    public void publishLoginSuccess(String clientId, String clientType, String ipAddress) {
        if (!kafkaEnabled) {
            logger.debug("Kafka disabled, skipping login success event");
            return;
        }

        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_SUCCESS)
                .clientId(sanitizeClientId(clientId))
                .clientType(clientType)
                .ipAddress(maskIpAddress(ipAddress))
                .success(true)
                .build();

        String sanitizedKey = sanitizeClientId(clientId);
        sendToTopic(loginSuccessTopic, sanitizedKey, event);
        sendToTopic(authEventsTopic, sanitizedKey, event);
    }

    @Async("kafkaAsyncExecutor")
    public void publishLoginFailed(String clientId, String clientType, String ipAddress, String reason) {
        if (!kafkaEnabled) {
            logger.debug("Kafka disabled, skipping login failed event");
            return;
        }

        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.LOGIN_FAILED)
                .clientId(sanitizeClientId(clientId))
                .clientType(clientType)
                .ipAddress(maskIpAddress(ipAddress))
                .success(false)
                .failureReason(sanitizeFailureReason(reason))
                .build();

        String sanitizedKey = clientId != null ? sanitizeClientId(clientId) : maskIpAddress(ipAddress);
        sendToTopic(loginFailedTopic, sanitizedKey, event);
        sendToTopic(authEventsTopic, sanitizedKey, event);
    }

    @Async("kafkaAsyncExecutor")
    public void publishInvalidApiKey(String ipAddress) {
        if (!kafkaEnabled) {
            return;
        }

        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.INVALID_API_KEY)
                .ipAddress(maskIpAddress(ipAddress))
                .success(false)
                .failureReason("Invalid API key")
                .build();

        String sanitizedKey = maskIpAddress(ipAddress);
        sendToTopic(loginFailedTopic, sanitizedKey, event);
        sendToTopic(authEventsTopic, sanitizedKey, event);
    }

    @Async("kafkaAsyncExecutor")
    public void publishRateLimitExceeded(String clientId, String ipAddress) {
        if (!kafkaEnabled) {
            return;
        }

        AuthEvent event = AuthEvent.builder()
                .eventType(AuthEventType.RATE_LIMIT_EXCEEDED)
                .clientId(sanitizeClientId(clientId))
                .ipAddress(maskIpAddress(ipAddress))
                .success(false)
                .failureReason("Rate limit exceeded")
                .build();

        String sanitizedKey = clientId != null ? sanitizeClientId(clientId) : maskIpAddress(ipAddress);
        sendToTopic(loginFailedTopic, sanitizedKey, event);
        sendToTopic(authEventsTopic, sanitizedKey, event);
    }

    @Async("kafkaAsyncExecutor")
    public void publishTokenGenerated(String tokenId, String clientId, String clientType, String ipAddress) {
        if (!kafkaEnabled) {
            return;
        }

        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.GENERATED)
                .tokenId(maskTokenId(tokenId))
                .clientId(sanitizeClientId(clientId))
                .clientType(clientType)
                .ipAddress(maskIpAddress(ipAddress))
                .build();

        sendToTopic(tokensTopic, sanitizeClientId(clientId), event);
    }

    @Async("kafkaAsyncExecutor")
    public void publishTokenRefreshed(String tokenId, String clientId, String clientType, String ipAddress) {
        if (!kafkaEnabled) {
            return;
        }

        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.REFRESHED)
                .tokenId(maskTokenId(tokenId))
                .clientId(sanitizeClientId(clientId))
                .clientType(clientType)
                .ipAddress(maskIpAddress(ipAddress))
                .build();

        sendToTopic(tokensTopic, sanitizeClientId(clientId), event);
    }

    @Async("kafkaAsyncExecutor")
    public void publishTokenRevoked(String tokenId, String clientId) {
        if (!kafkaEnabled) {
            return;
        }

        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.REVOKED)
                .tokenId(maskTokenId(tokenId))
                .clientId(sanitizeClientId(clientId))
                .build();

        sendToTopic(tokensTopic, sanitizeClientId(clientId), event);
    }

    @Async("kafkaAsyncExecutor")
    public void publishTokenValidated(String tokenId, String clientId) {
        if (!kafkaEnabled) {
            return;
        }

        TokenEvent event = TokenEvent.builder()
                .eventType(TokenEvent.TokenEventType.VALIDATED)
                .tokenId(maskTokenId(tokenId))
                .clientId(sanitizeClientId(clientId))
                .build();

        sendToTopic(tokensTopic, sanitizeClientId(clientId), event);
    }

    @Async("kafkaAsyncExecutor")
    public void publishAuthEvent(AuthEvent event) {
        if (!kafkaEnabled) {
            return;
        }

        // Apply PII filtering to the event
        AuthEvent sanitizedEvent = AuthEvent.builder()
                .eventType(event.getEventType())
                .clientId(sanitizeClientId(event.getClientId()))
                .clientType(event.getClientType())
                .ipAddress(maskIpAddress(event.getIpAddress()))
                .success(event.isSuccess())
                .failureReason(sanitizeFailureReason(event.getFailureReason()))
                .metadata(event.getMetadata())
                .build();

        String sanitizedKey = event.getClientId() != null
                ? sanitizeClientId(event.getClientId())
                : maskIpAddress(event.getIpAddress());
        sendToTopic(authEventsTopic, sanitizedKey, sanitizedEvent);

        if (event.isSuccess()) {
            sendToTopic(loginSuccessTopic, sanitizedKey, sanitizedEvent);
        } else {
            sendToTopic(loginFailedTopic, sanitizedKey, sanitizedEvent);
        }
    }

    private void sendToTopic(String topic, String key, Object event) {
        Timer.Sample sample = Timer.start();

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            sample.stop(publishLatencyTimer);

            if (ex == null) {
                logger.debug("Event sent to topic {} with key {}: offset={}",
                        topic, key, result.getRecordMetadata().offset());
                eventsPublishedCounter.increment();
            } else {
                logger.error("Failed to send event to topic {} with key {}: {}",
                        topic, key, ex.getMessage());
                eventsFailedCounter.increment();
                // Send to DLQ
                sendToDlq(topic, key, event, ex.getMessage());
            }
        });
    }

    private void sendToDlq(String originalTopic, String key, Object event, String errorMessage) {
        try {
            // Create a wrapper with error context
            DlqEvent dlqEvent = new DlqEvent(originalTopic, key, event, errorMessage);
            kafkaTemplate.send(dlqTopic, key, dlqEvent)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Event sent to DLQ for topic {}: key={}", originalTopic, key);
                            eventsDlqCounter.increment();
                        } else {
                            logger.error("Failed to send event to DLQ: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            logger.error("Critical: Failed to send to DLQ: {}", e.getMessage());
        }
    }

    // PII filtering methods - ensure no sensitive data is published

    private String sanitizeClientId(String clientId) {
        if (clientId == null) {
            return null;
        }
        // Keep client ID as-is if it's a UUID or system identifier
        // Mask if it looks like an email or personal identifier
        if (clientId.contains("@")) {
            int atIndex = clientId.indexOf("@");
            return clientId.substring(0, Math.min(3, atIndex)) + "***" + clientId.substring(atIndex);
        }
        return clientId;
    }

    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }
        // Mask last octet for IPv4, keep prefix for analytics
        if (ipAddress.contains(".")) {
            int lastDot = ipAddress.lastIndexOf(".");
            return ipAddress.substring(0, lastDot) + ".xxx";
        }
        // For IPv6, mask last segments
        if (ipAddress.contains(":")) {
            String[] parts = ipAddress.split(":");
            if (parts.length >= 4) {
                return parts[0] + ":" + parts[1] + ":" + parts[2] + ":xxxx:xxxx:xxxx";
            } else {
                // For compressed or short IPv6 (e.g., ::1), return a fully masked placeholder
                return "xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx";
            }
        }
        return ipAddress;
    }

    private String maskTokenId(String tokenId) {
        if (tokenId == null || tokenId.length() < 8) {
            return "***";
        }
        // Show only first 4 and last 4 characters
        return tokenId.substring(0, 4) + "..." + tokenId.substring(tokenId.length() - 4);
    }

    private String sanitizeFailureReason(String reason) {
        if (reason == null) {
            return null;
        }
        // Remove any potential sensitive data from error messages
        return reason.replaceAll("(?i)(password|secret|key|token)\\s*[:=]\\s*\\S+", "$1=***");
    }

    // DLQ wrapper class
    public static class DlqEvent {
        private final String originalTopic;
        private final String key;
        private final Object event;
        private final String errorMessage;
        private final long timestamp;

        public DlqEvent(String originalTopic, String key, Object event, String errorMessage) {
            this.originalTopic = originalTopic;
            this.key = key;
            this.event = event;
            this.errorMessage = errorMessage;
            this.timestamp = System.currentTimeMillis();
        }

        public String getOriginalTopic() { return originalTopic; }
        public String getKey() { return key; }
        public Object getEvent() { return event; }
        public String getErrorMessage() { return errorMessage; }
        public long getTimestamp() { return timestamp; }
    }
}
