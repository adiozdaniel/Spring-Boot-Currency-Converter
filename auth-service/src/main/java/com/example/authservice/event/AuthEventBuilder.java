package com.example.authservice.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A builder class for creating instances of {@link AuthEvent}.
 * <p>
 * This class follows the builder pattern to allow for the easy construction
 * of {@link AuthEvent} objects. It initializes the event ID and timestamp
 * automatically.
 * </p>
 */
public class AuthEventBuilder {
  private String eventId = UUID.randomUUID().toString();
  private Instant timestamp = Instant.now();
  private AuthEventType eventType;
  private String clientId;
  private String clientType;
  private String ipAddress;
  private boolean success;
  private String failureReason;
  private Map<String, Object> metadata;

  /**
   * Sets the type of the authentication event.
   *
   * @param eventType the type of the event.
   * @return this builder instance.
   */
  public AuthEventBuilder eventType(AuthEventType eventType) {
    this.eventType = eventType;
    return this;
  }

  /**
   * Sets the client ID for the event.
   *
   * @param clientId the client's unique identifier.
   * @return this builder instance.
   */
  public AuthEventBuilder clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Sets the client type for the event.
   *
   * @param clientType the type of the client (e.g., "web", "mobile").
   * @return this builder instance.
   */
  public AuthEventBuilder clientType(String clientType) {
    this.clientType = clientType;
    return this;
  }

  /**
   * Sets the IP address from which the event originated.
   *
   * @param ipAddress the client's IP address.
   * @return this builder instance.
   */
  public AuthEventBuilder ipAddress(String ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }

  /**
   * Sets the timestamp of the event.
   *
   * @param timestamp the time at which the event occurred.
   * @return this builder instance.
   */
  public AuthEventBuilder timestamp(Instant timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * Sets the success status of the event.
   *
   * @param success true if the event was successful, false otherwise.
   * @return this builder instance.
   */
  public AuthEventBuilder success(boolean success) {
    this.success = success;
    return this;
  }

  /**
   * Sets the reason for failure, if the event was not successful.
   *
   * @param failureReason a string describing the reason for failure.
   * @return this builder instance.
   */
  public AuthEventBuilder failureReason(String failureReason) {
    this.failureReason = failureReason;
    return this;
  }

  /**
   * Sets additional metadata for the event.
   *
   * @param metadata a map of additional key-value data.
   * @return this builder instance.
   */
  public AuthEventBuilder metadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Builds and returns an {@link AuthEvent} instance with the configured properties.
   *
   * @return a new {@link AuthEvent} instance.
   */
  public AuthEvent build() {
    return new AuthEvent(
        eventId,
        eventType,
        clientId,
        clientType,
        ipAddress,
        timestamp,
        success,
        failureReason,
        metadata);
  }
}
