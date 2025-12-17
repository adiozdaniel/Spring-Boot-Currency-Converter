package com.example.mainservice.consumer;

import com.example.mainservice.event.AuthEvent;
import com.example.mainservice.event.AuthEventType;
import com.example.mainservice.service.IdempotencyService;
import com.example.mainservice.service.SessionManagementService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@ConditionalOnProperty(value = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class AuthEventConsumer {

    private final SessionManagementService sessionManagementService;
    private final IdempotencyService idempotencyService;
    private final Counter eventsProcessedCounter;
    private final Counter eventsDuplicateCounter;
    private final Timer eventProcessingTimer;

    public AuthEventConsumer(SessionManagementService sessionManagementService,
                            IdempotencyService idempotencyService,
                            MeterRegistry meterRegistry) {
        this.sessionManagementService = sessionManagementService;
        this.idempotencyService = idempotencyService;

        // Initialize metrics
        this.eventsProcessedCounter = Counter.builder("kafka.auth.events.processed")
                .description("Number of auth events processed")
                .register(meterRegistry);
        this.eventsDuplicateCounter = Counter.builder("kafka.auth.events.duplicate")
                .description("Number of duplicate auth events received")
                .register(meterRegistry);
        this.eventProcessingTimer = Timer.builder("kafka.auth.events.processing.time")
                .description("Time taken to process auth events")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = "${kafka.topics.auth-events}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "authEventKafkaListenerContainerFactory"
    )
    public void handleAuthEvent(@Payload AuthEvent event,
                               @Header(KafkaHeaders.RECEIVED_KEY) String key,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        processEvent(event, partition, offset,
                String.format("Received auth event: type=%s, clientId=%s, key=%s, partition=%d, offset=%d",
                        event != null ? event.getEventType() : null,
                        event != null ? event.getClientId() : null,
                        key, partition, offset),
                this::processAuthEvent);
    }

    @KafkaListener(
            topics = "${kafka.topics.auth-login-success}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "authEventKafkaListenerContainerFactory"
    )
    public void handleLoginSuccess(@Payload AuthEvent event,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String clientId,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset) {
        processEvent(event, partition, offset,
                String.format("Received login success event: clientId=%s, partition=%d, offset=%d",
                        clientId, partition, offset),
                sessionManagementService::handleLoginSuccess);
    }

    @KafkaListener(
            topics = "${kafka.topics.auth-login-failed}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "authEventKafkaListenerContainerFactory"
    )
    public void handleLoginFailure(@Payload AuthEvent event,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String clientId,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset) {
        processEvent(event, partition, offset,
                String.format("Received login failure event: clientId=%s, partition=%d, offset=%d",
                        clientId, partition, offset),
                sessionManagementService::handleLoginFailure);
    }

    /**
     * Common event processing logic with validation, idempotency, metrics, and error handling.
     *
     * @param event the auth event to process
     * @param partition the Kafka partition
     * @param offset the Kafka offset
     * @param logMessage the log message to display when receiving the event
     * @param processor the event processing logic to execute
     */
    private void processEvent(AuthEvent event, int partition, long offset,
                              String logMessage, Consumer<AuthEvent> processor) {
        eventProcessingTimer.record(() -> {
            validateAuthEvent(event, partition, offset);

            log.info(logMessage);

            if (idempotencyService.checkAndMarkProcessed(event.getEventId())) {
                log.warn("Duplicate event detected: {}, skipping processing", event.getEventId());
                eventsDuplicateCounter.increment();
                return;
            }

            try {
                processor.accept(event);
                eventsProcessedCounter.increment();
            } catch (Exception e) {
                log.error("Error processing auth event: eventId={}, type={}, clientId={}",
                        event.getEventId(), event.getEventType(), event.getClientId(), e);
                throw e;
            }
        });
    }

    private void processAuthEvent(AuthEvent event) {
        AuthEventType eventType = event.getEventType();

        switch (eventType) {
            case LOGIN_SUCCESS:
                sessionManagementService.handleLoginSuccess(event);
                break;
            case LOGIN_FAILED:
                sessionManagementService.handleLoginFailure(event);
                break;
            case RATE_LIMIT_EXCEEDED:
            case INVALID_API_KEY:
            case INVALID_TOKEN:
                log.warn("Security event detected: type={}, clientId={}, ip={}",
                        eventType, event.getClientId(), event.getIpAddress());
                sessionManagementService.handleLoginFailure(event);
                break;
            default:
                log.debug("Unhandled auth event type: {}", eventType);
        }
    }

    /**
     * Validates the AuthEvent to ensure all required fields are present.
     *
     * @param event the auth event to validate
     * @param partition the Kafka partition
     * @param offset the Kafka offset
     * @throws IllegalArgumentException if validation fails
     */
    private void validateAuthEvent(AuthEvent event, int partition, long offset) {
        if (event == null) {
            String errorMsg = String.format("Received null AuthEvent from partition=%d, offset=%d", partition, offset);
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            String errorMsg = String.format("AuthEvent missing eventId: partition=%d, offset=%d, clientId=%s",
                    partition, offset, event.getClientId());
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (event.getEventType() == null) {
            String errorMsg = String.format("AuthEvent missing eventType: eventId=%s, partition=%d, offset=%d",
                    event.getEventId(), partition, offset);
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (event.getClientId() == null || event.getClientId().trim().isEmpty()) {
            String errorMsg = String.format("AuthEvent missing clientId: eventId=%s, type=%s, partition=%d, offset=%d",
                    event.getEventId(), event.getEventType(), partition, offset);
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }
}
