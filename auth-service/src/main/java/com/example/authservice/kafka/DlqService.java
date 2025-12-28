package com.example.authservice.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

/**
 * Service responsible for sending failed events to a Dead Letter Queue (DLQ) topic.
 * <p>
 * This ensures that events that could not be processed successfully by the
 * main Kafka topics are not lost and can be inspected later.
 * </p>
 */
@Service
public class DlqService {
  private static final Logger logger = LoggerFactory.getLogger(DlqService.class);
  private final KafkaSender<String, Object> kafkaSender;
  private final String dlqTopic;
  private final Counter dlqCounter;
  private final Counter dlqFailureCounter;

  @Value("${kafka.enabled:true}")
  private boolean kafkaEnabled;

  /**
   * Constructs a new {@link DlqService}.
   *
   * @param kafkaSender   the Kafka sender for publishing messages.
   * @param dlqTopic      the name of the Dead Letter Queue topic.
   * @param meterRegistry the registry for collecting and managing metrics.
   */
  public DlqService(
      KafkaSender<String, Object> kafkaSender,
      @Value("${kafka.topics.auth-dlq.name:auth.dlq}") String dlqTopic,
      MeterRegistry meterRegistry) {

    this.kafkaSender = kafkaSender;
    this.dlqTopic = dlqTopic;

    this.dlqCounter = Counter.builder("auth.events.dlq")
        .description("Number of authentication events sent to DLQ")
        .register(meterRegistry);

    this.dlqFailureCounter = Counter.builder("auth.events.dlq.failure")
        .description("Number of failures sending authentication events to DLQ")
        .register(meterRegistry);
  }

  /**
   * Sends a failed event to the Dead Letter Queue (DLQ).
   *
   * @param originalTopic the name of the Kafka topic where the event originally failed.
   * @param key           the key of the Kafka message.
   * @param event         the original event object that failed to be processed.
   * @param errorMessage  a description of the error that caused the event to be sent to the DLQ.
   * @return a {@link Mono<Void>} that completes when the message is sent to the DLQ
   *         or an error occurs. Returns {@link Mono#empty()} if Kafka is disabled.
   */
  public Mono<Void> sendToDlq(
      String originalTopic, String key,
      Object event, String errorMessage) {

    if (!kafkaEnabled) {
      return Mono.empty();
    }

    DlqEvent dlqEvent = new DlqEvent(originalTopic, key, event, errorMessage);
    ProducerRecord<String, Object> record = new ProducerRecord<>(dlqTopic, key, dlqEvent);

    return kafkaSender.send(
        Mono.just(SenderRecord.create(record, null)))
        .then()
        .doOnSuccess(v -> {
          logger.info("Event sent to DLQ for topic {}: key={}", originalTopic, key);
          dlqCounter.increment();
        })
        .doOnError(ex -> {
          logger.error("Critical: Failed to send to DLQ: {}", ex.getMessage());
          dlqFailureCounter.increment();
        })
        .onErrorResume(ex -> Mono.empty());
  }
}
