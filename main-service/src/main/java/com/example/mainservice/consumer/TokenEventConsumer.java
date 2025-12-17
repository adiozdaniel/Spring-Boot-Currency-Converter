package com.example.mainservice.consumer;

import com.example.mainservice.event.TokenEvent;
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
public class TokenEventConsumer {

    private final SessionManagementService sessionManagementService;
    private final IdempotencyService idempotencyService;
    private final Counter eventsProcessedCounter;
    private final Counter eventsDuplicateCounter;
    private final Timer eventProcessingTimer;

    public TokenEventConsumer(SessionManagementService sessionManagementService,
                             IdempotencyService idempotencyService,
                             MeterRegistry meterRegistry) {
        this.sessionManagementService = sessionManagementService;
        this.idempotencyService = idempotencyService;

        // Initialize metrics
        this.eventsProcessedCounter = Counter.builder("kafka.token.events.processed")
                .description("Number of token events processed")
                .register(meterRegistry);
        this.eventsDuplicateCounter = Counter.builder("kafka.token.events.duplicate")
                .description("Number of duplicate token events received")
                .register(meterRegistry);
        this.eventProcessingTimer = Timer.builder("kafka.token.events.processing.time")
                .description("Time taken to process token events")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = "${kafka.topics.auth-tokens}",
            groupId = "${kafka.consumer.group-id}",
            containerFactory = "tokenEventKafkaListenerContainerFactory"
    )
    public void handleTokenEvent(@Payload TokenEvent event,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                @Header(KafkaHeaders.OFFSET) long offset) {

        eventProcessingTimer.record(() -> {
            log.info("Received token event: type={}, clientId={}, tokenId={}, key={}, partition={}, offset={}",
                    event.getEventType(), event.getClientId(), event.getTokenId(), key, partition, offset);

            // Check idempotency
            if (idempotencyService.isEventProcessed(event.getEventId())) {
                log.warn("Duplicate token event detected: {}, skipping processing", event.getEventId());
                eventsDuplicateCounter.increment();
                return;
            }

            try {
                processTokenEvent(event);
                idempotencyService.markEventAsProcessed(event.getEventId());
                eventsProcessedCounter.increment();
            } catch (Exception e) {
                log.error("Error processing token event: {}", event, e);
                throw e; // Re-throw to trigger retry/DLQ
            }
        });
    }

    private void processTokenEvent(TokenEvent event) {
        TokenEvent.TokenEventType eventType = event.getEventType();

        switch (eventType) {
            case GENERATED:
                sessionManagementService.handleTokenGenerated(event);
                log.info("Token generated for client: {}", event.getClientId());
                break;
            case REFRESHED:
                sessionManagementService.handleTokenGenerated(event);
                log.info("Token refreshed for client: {}", event.getClientId());
                break;
            case REVOKED:
                sessionManagementService.handleTokenRevoked(event);
                log.info("Token revoked for client: {}", event.getClientId());
                break;
            case EXPIRED:
                sessionManagementService.handleTokenExpired(event);
                log.info("Token expired for client: {}", event.getClientId());
                break;
            case VALIDATED:
                log.debug("Token validated for client: {}", event.getClientId());
                break;
            default:
                log.warn("Unhandled token event type: {}", eventType);
        }
    }
}
