package com.example.authservice.service.impl;

import com.example.authservice.event.AuthEvent;
import com.example.authservice.event.AuthEventType;
import com.example.authservice.event.TokenEvent;
import com.example.authservice.kafka.DlqService;
import com.example.authservice.service.AuthEventProducer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.producer.ProducerRecord;

import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link AuthEventProducer} interface for publishing
 * authentication-related events to Kafka.
 * <p>
 * This service handles various authentication and token events, logs their status,
 * collects metrics, and routes failed events to a Dead Letter Queue (DLQ).
 * PII is sanitized before publishing.
 * </p>
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AuthEventProducerImpl implements AuthEventProducer {

  private static final Logger logger = LoggerFactory.getLogger(AuthEventProducerImpl.class);
  private final DlqService dlqService;

  private final KafkaSender<String, Object> kafkaSender;
  private final Counter eventsPublishedCounter;
  private final Counter eventsFailedCounter;
  private final Timer publishLoginLatency;
  private final String loginSuccessTopic;
  private final String loginFailedTopic;
  private final String tokensTopic;

  @Value("${kafka.enabled:true}")
  private boolean kafkaEnabled;

  /**
   * Constructs an {@link AuthEventProducerImpl} with the necessary dependencies.
   *
   * @param kafkaSender         the Kafka sender for publishing messages.
   * @param dlqService          the service for sending failed events to the DLQ.
   * @param loginSuccessTopic   the Kafka topic for successful login events.
   * @param loginFailedTopic    the Kafka topic for failed login events.
   * @param tokensTopic         the Kafka topic for token events.
   * @param meterRegistry       the registry for collecting and managing metrics.
   */
  public AuthEventProducerImpl(
      KafkaSender<String, Object> kafkaSender,
      DlqService dlqService,
      @Value("${kafka.topics.auth-login-success.name:auth.login.success}") String loginSuccessTopic,
      @Value("${kafka.topics.auth-login-failed.name:auth.login.failed}") String loginFailedTopic,
      @Value("${kafka.topics.auth-tokens.name:auth.tokens}") String tokensTopic,
      MeterRegistry meterRegistry) {

    this.kafkaSender = kafkaSender;
    this.loginSuccessTopic = loginSuccessTopic;
    this.loginFailedTopic = loginFailedTopic;
    this.tokensTopic = tokensTopic;
    this.dlqService = dlqService;

    this.eventsPublishedCounter = Counter.builder("auth.events.published")
        .description("Number of authentication events published to Kafka")
        .register(meterRegistry);
    this.eventsFailedCounter = Counter.builder("auth.events.failed")
        .description("Number of authentication events failed to publish to Kafka")
        .register(meterRegistry);
    this.publishLoginLatency = Timer.builder("auth.events.publish.login.latency")
        .description("Latency of publishing authentication events to Kafka")
        .register(meterRegistry);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishLoginSuccess(
      String clientId, String clientType, String ipAddress) {

    if (!kafkaEnabled) {
      logger.debug("Kafka disabled, skipping login success event");
      return Mono.empty();
    }

    return Mono.defer(() -> {
      Timer.Sample latency = Timer.start();

      AuthEvent event = AuthEvent.builder()
          .eventType(AuthEventType.LOGIN_SUCCESS)
          .clientId(sanitizeClientId(clientId))
          .clientType(clientType)
          .ipAddress(maskIpAddress(ipAddress))
          .success(true)
          .build();

      ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(loginSuccessTopic, clientId, event);

      return kafkaSender.send(
          Mono.just(SenderRecord.create(producerRecord, null)))
          .then()
          .doOnSuccess(v -> {
            eventsPublishedCounter.increment();
            latency.stop(publishLoginLatency);
          })
          .doOnError(ex -> {
            logger.error("Failed to publish login success event", ex);
            eventsFailedCounter.increment();
          })
          .onErrorResume(ex -> dlqService.sendToDlq(loginSuccessTopic, clientId, event, ex.getMessage()));
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishLoginFailed(
      String clientId, String clientType, String ipAddress, String reason) {

    if (!kafkaEnabled) {
      logger.debug("Kafka disabled, skipping login failed event");
      return Mono.empty();
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

    ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(loginFailedTopic, sanitizedKey, event);

    return kafkaSender.send(Mono.just(SenderRecord.create(producerRecord, null)))
        .then()
        .doOnSuccess(v -> eventsPublishedCounter.increment())
        .doOnError(ex -> {
          eventsFailedCounter.increment();
          logger.error("Failed to publish login failed event", ex);
        })
        .onErrorResume(ex -> dlqService.sendToDlq(loginFailedTopic, sanitizedKey, event, ex.getMessage()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishInvalidApiKey(String ipAddress) {
    if (!kafkaEnabled) {
      return Mono.empty();
    }

    AuthEvent event = AuthEvent.builder()
        .eventType(AuthEventType.INVALID_API_KEY)
        .ipAddress(maskIpAddress(ipAddress))
        .success(false)
        .failureReason("Invalid API key")
        .build();

    String sanitizedKey = maskIpAddress(ipAddress);

    ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(loginFailedTopic, sanitizedKey, event);

    return kafkaSender.send(
        Mono.just(SenderRecord.create(producerRecord, null)))
        .then()
        .doOnSuccess(v -> eventsPublishedCounter.increment())
        .doOnError(ex -> {
          logger.error("Failed to publish invalid API key event", ex);
          eventsFailedCounter.increment();
        })
        .onErrorResume(ex -> dlqService.sendToDlq(loginFailedTopic, sanitizedKey, event, ex.getMessage()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishRateLimitExceeded(String clientId, String ipAddress) {
    if (!kafkaEnabled) {
      return Mono.empty();
    }

    AuthEvent event = AuthEvent.builder()
        .eventType(AuthEventType.RATE_LIMIT_EXCEEDED)
        .clientId(sanitizeClientId(clientId))
        .ipAddress(maskIpAddress(ipAddress))
        .success(false)
        .failureReason("Rate limit exceeded")
        .build();

    String sanitizedKey = clientId != null ? sanitizeClientId(clientId) : maskIpAddress(ipAddress);
    ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(loginFailedTopic, sanitizedKey, event);

    return kafkaSender.send(
        Mono.just(SenderRecord.create(producerRecord, null)))
        .then()
        .doOnSuccess(v -> eventsPublishedCounter.increment())
        .doOnError(ex -> {
          logger.error("Failed to publish rate limit exceeded event", ex);
          eventsFailedCounter.increment();
        })
        .onErrorResume(ex -> dlqService.sendToDlq(loginFailedTopic, sanitizedKey, event, ex.getMessage()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishTokenGenerated(
      String tokenId, String clientId, String clientType, String ipAddress) {

    if (!kafkaEnabled) {
      return Mono.empty();
    }

    TokenEvent event = TokenEvent.builder()
        .eventType(TokenEvent.TokenEventType.GENERATED)
        .tokenId(maskTokenId(tokenId))
        .clientId(sanitizeClientId(clientId))
        .clientType(clientType)
        .ipAddress(maskIpAddress(ipAddress))
        .build();

    ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(tokensTopic, sanitizeClientId(clientId), event);

    return kafkaSender.send(
        Mono.just(SenderRecord.create(producerRecord, null)))
        .then()
        .doOnSuccess(v -> eventsPublishedCounter.increment())
        .doOnError(ex -> {
          logger.error("Failed to publish token generated event", ex);
          eventsFailedCounter.increment();
        })
        .onErrorResume(ex -> dlqService.sendToDlq(tokensTopic, sanitizeClientId(clientId), event, ex.getMessage()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishTokenRefreshed(
      String tokenId, String clientId, String clientType, String ipAddress) {

    if (!kafkaEnabled) {
      return Mono.empty();
    }

    TokenEvent event = TokenEvent.builder()
        .eventType(TokenEvent.TokenEventType.REFRESHED)
        .tokenId(maskTokenId(tokenId))
        .clientId(sanitizeClientId(clientId))
        .clientType(clientType)
        .ipAddress(maskIpAddress(ipAddress))
        .build();

    ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(tokensTopic, sanitizeClientId(clientId), event);

    return kafkaSender.send(
        Mono.just(SenderRecord.create(producerRecord, null)))
        .then()
        .doOnSuccess(v -> eventsPublishedCounter.increment())
        .doOnError(ex -> {
          logger.error("Failed to publish token refreshed event", ex);
          eventsFailedCounter.increment();
        })
        .onErrorResume(ex -> dlqService.sendToDlq(tokensTopic, sanitizeClientId(clientId), event, ex.getMessage()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishTokenRevoked(String tokenId, String clientId) {
    if (!kafkaEnabled) {
      return Mono.empty();
    }

    TokenEvent event = TokenEvent.builder()
        .eventType(TokenEvent.TokenEventType.REVOKED)
        .tokenId(maskTokenId(tokenId))
        .clientId(sanitizeClientId(clientId))
        .build();

    ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(tokensTopic, sanitizeClientId(clientId), event);

    return kafkaSender.send(
        Mono.just(SenderRecord.create(producerRecord, null)))
        .then()
        .doOnSuccess(v -> eventsPublishedCounter.increment())
        .doOnError(ex -> {
          logger.error("Failed to publish token revoked event", ex);
          eventsFailedCounter.increment();
        })
        .onErrorResume(ex -> dlqService.sendToDlq(tokensTopic, sanitizeClientId(clientId), event, ex.getMessage()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> publishTokenValidated(String tokenId, String clientId) {
    if (!kafkaEnabled) {
      return Mono.empty();
    }

    TokenEvent event = TokenEvent.builder()
        .eventType(TokenEvent.TokenEventType.VALIDATED)
        .tokenId(maskTokenId(tokenId))
        .clientId(sanitizeClientId(clientId))
        .build();

    ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(tokensTopic, sanitizeClientId(clientId), event);

    return kafkaSender.send(
        Mono.just(SenderRecord.create(producerRecord, null)))
        .then()
        .doOnSuccess(v -> eventsPublishedCounter.increment())
        .doOnError(ex -> {
          logger.error("Failed to publish token validated event", ex);
          eventsFailedCounter.increment();
        })
        .onErrorResume(ex -> dlqService.sendToDlq(tokensTopic, sanitizeClientId(clientId), event, ex.getMessage()));
  }

  /**
   * Sanitizes a client ID to remove or mask sensitive information.
   *
   * @param clientId the original client ID.
   * @return the sanitized client ID.
   */
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

  /**
   * Masks an IP address to hide the last octet for IPv4 or segments for IPv6.
   *
   * @param ipAddress the original IP address.
   * @return the masked IP address.
   */
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
      }
      else {
        // For compressed or short IPv6 (e.g., ::1), return a fully masked placeholder
        return "xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx";
      }
    }
    return ipAddress;
  }

  /**
   * Masks a token ID to show only the first and last few characters.
   *
   * @param tokenId the original token ID.
   * @return the masked token ID.
   */
  private String maskTokenId(String tokenId) {
    if (tokenId == null || tokenId.length() < 8) {
      return "***";
    }
    // Show only first 4 and last 4 characters
    return tokenId.substring(0, 4) + "..." + tokenId.substring(tokenId.length() - 4);
  }

  /**
   * Sanitizes a failure reason string to remove potential sensitive data.
   *
   * @param reason the original failure reason.
   * @return the sanitized failure reason.
   */
  private String sanitizeFailureReason(String reason) {
    if (reason == null) {
      return null;
    }
    // Remove any potential sensitive data from error messages
    return reason.replaceAll("(?i)(password|secret|key|token)\\s*[:=]\\s*\\S+", "$1=***");
  }
}