package com.example.authservice.kafka;

public record DlqEvent(
    String originalTopic,
    String key,
    Object event,
    String errorMessage,
    long timestamp) {

  /**
   * Constructs a new {@link DlqEvent} with the specified details and the current timestamp.
   *
   * @param originalTopic the name of the Kafka topic where the event originally failed.
   * @param key           the key of the Kafka message.
   * @param event         the original event object that failed to be processed.
   * @param errorMessage  a description of the error that caused the event to be sent to the DLQ.
   */
  public DlqEvent(
      String originalTopic, String key,
      Object event, String errorMessage) {

    this(originalTopic, key, event, errorMessage, System.currentTimeMillis());
  }
}
