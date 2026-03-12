package com.currencyconverter.authservice.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A builder class for creating instances of {@link TokenEvent}.
 * <p>
 * This class follows the builder pattern to allow for the easy construction
 * of {@link TokenEvent} objects. It initializes the event ID and timestamp
 * automatically.
 * </p>
 */
public class TokenEventBuilder {
  private String eventId = UUID.randomUUID().toString();
  private Instant timestamp = Instant.now();
  private TokenEvent.TokenEventType eventType;
  private String tokenId;
  private String clientId;
  private String clientType;
  private Instant tokenExpiry;
  private String ipAddress;
  private boolean success = true;
  private String failureReason;
  private Map<String, Object> metadata;

  /**
   * Sets the type of the token event.
   *
   * @param eventType the type of the event.
   * @return this builder instance.
   */
  public TokenEventBuilder eventType(TokenEvent.TokenEventType eventType) {
    this.eventType = eventType;
    return this;
  }

  /**
   * Sets the token ID for the event.
   *
   * @param tokenId the token's unique identifier.
   * @return this builder instance.
   */
  public TokenEventBuilder tokenId(String tokenId) {
    this.tokenId = tokenId;
    return this;
  }

  /**
   * Sets the client ID for the event.
   *
   * @param clientId the client's unique identifier.
   * @return this builder instance.
   */
  public TokenEventBuilder clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Sets the client type for the event.
   *
   * @param clientType the type of the client (e.g., "web", "mobile").
   * @return this builder instance.
   */
  public TokenEventBuilder clientType(String clientType) {
    this.clientType = clientType;
    return this;
  }

  /**
   * Sets the timestamp of the event.
   *
   * @param timestamp the time at which the event occurred.
   * @return this builder instance.
   */
  public TokenEventBuilder timestamp(Instant timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  /**
   * Sets the expiration timestamp of the token.
   *
   * @param tokenExpiry the token's expiration time.
   * @return this builder instance.
   */
  public TokenEventBuilder tokenExpiry(Instant tokenExpiry) {
    this.tokenExpiry = tokenExpiry;
    return this;
  }

  /**
   * Sets the IP address from which the event originated.
   *
   * @param ipAddress the client's IP address.
   * @return this builder instance.
   */
  public TokenEventBuilder ipAddress(String ipAddress) {
    this.ipAddress = ipAddress;
    return this;
  }

  /**
   * Sets the success status of the event.
   *
   * @param success true if the event was successful, false otherwise.
   * @return this builder instance.
   */
  public TokenEventBuilder success(boolean success) {
    this.success = success;
    return this;
  }

  /**
   * Sets the reason for failure, if the event was not successful.
   *
   * @param failureReason a string describing the reason for failure.
   * @return this builder instance.
   */
  public TokenEventBuilder failureReason(String failureReason) {
    this.failureReason = failureReason;
    return this;
  }

  /**
   * Sets additional metadata for the event.
   *
   * @param metadata a map of additional key-value data.
   * @return this builder instance.
   */
  public TokenEventBuilder metadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Builds and returns a {@link TokenEvent} instance with the configured properties.
   *
   * @return a new {@link TokenEvent} instance.
   */
  public TokenEvent build() {
    return new TokenEvent(
        eventId,
        eventType,
        tokenId,
        clientId,
        clientType,
        timestamp,
        tokenExpiry,
        ipAddress,
        success,
        failureReason,
        metadata);
  }
}
