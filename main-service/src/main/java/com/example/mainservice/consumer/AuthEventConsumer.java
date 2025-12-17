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

        eventProcessingTimer.record(() -> {
            log.info("Received auth event: type={}, clientId={}, key={}, partition={}, offset={}",
                    event.getEventType(), event.getClientId(), key, partition, offset);

            // Check idempotency
            if (idempotencyService.isEventProcessed(event.getEventId())) {
                log.warn("Duplicate event detected: {}, skipping processing", event.getEventId());
                eventsDuplicateCounter.increment();
                return;
            }

            try {
                processAuthEvent(event);
                idempotencyService.markEventAsProcessed(event.getEventId());
                eventsProcessedCounter.increment();
            } catch (Exception e) {
                log.error("Error processing auth event: {}", event, e);
                throw e; // Re-throw to trigger retry/DLQ
            }
        });
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

        eventProcessingTimer.record(() -> {
            log.info("Received login success event: clientId={}, partition={}, offset={}",
                    clientId, partition, offset);

            // Check idempotency
            if (idempotencyService.isEventProcessed(event.getEventId())) {
                log.warn("Duplicate event detected: {}, skipping processing", event.getEventId());
                eventsDuplicateCounter.increment();
                return;
            }

            try {
                sessionManagementService.handleLoginSuccess(event);
                idempotencyService.markEventAsProcessed(event.getEventId());
                eventsProcessedCounter.increment();
            } catch (Exception e) {
                log.error("Error processing login success event: {}", event, e);
                throw e;
            }
        });
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

        eventProcessingTimer.record(() -> {
            log.info("Received login failure event: clientId={}, partition={}, offset={}",
                    clientId, partition, offset);

            // Check idempotency
            if (idempotencyService.isEventProcessed(event.getEventId())) {
                log.warn("Duplicate event detected: {}, skipping processing", event.getEventId());
                eventsDuplicateCounter.increment();
                return;
            }

            try {
                sessionManagementService.handleLoginFailure(event);
                idempotencyService.markEventAsProcessed(event.getEventId());
                eventsProcessedCounter.increment();
            } catch (Exception e) {
                log.error("Error processing login failure event: {}", event, e);
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
                // Handle security events
                log.warn("Security event detected: type={}, clientId={}, ip={}",
                        eventType, event.getClientId(), event.getIpAddress());
                sessionManagementService.handleLoginFailure(event);
                break;
            default:
                log.debug("Unhandled auth event type: {}", eventType);
        }
    }
}
