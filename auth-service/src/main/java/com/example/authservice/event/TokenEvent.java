package com.example.authservice.event;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a token-related event.
 * <p>
 * This record encapsulates details about a token event, such as its type,
 * the token and client involved, and the outcome. It is designed to be immutable.
 * </p>
 *
 * @param eventId       the unique identifier for the event.
 * @param eventType     the type of token event.
 * @param tokenId       the identifier of the token.
 * @param clientId      the identifier of the client.
 * @param clientType    the type of the client (e.g., "web", "mobile").
 * @param timestamp     the time at which the event occurred.
 * @param tokenExpiry   the expiration time of the token.
 * @param ipAddress     the IP address from which the event originated.
 * @param success       a flag indicating whether the operation was successful.
 * @param failureReason the reason for failure, if applicable.
 * @param metadata      additional metadata associated with the event.
 */
public record TokenEvent(
        String eventId,
        TokenEventType eventType,
        String tokenId,
        String clientId,
        String clientType,
        Instant timestamp,
        Instant tokenExpiry,
        String ipAddress,
        boolean success,
        String failureReason,
        Map<String, Object> metadata) {

    /**
     * Creates a new {@link TokenEventBuilder} for constructing a {@link TokenEvent}.
     *
     * @return a new instance of {@link TokenEventBuilder}.
     */
    public static TokenEventBuilder builder() {
        return new TokenEventBuilder();
    }

    /**
     * Enumeration of token event types.
     */
    public enum TokenEventType {
        /**
         * A new token was generated.
         */
        GENERATED,
        /**
         * A token was refreshed.
         */
        REFRESHED,
        /**
         * A token was revoked.
         */
        REVOKED,
        /**
         * A token was successfully validated.
         */
        VALIDATED,
        /**
         * A token has expired.
         */
        EXPIRED
    }
}
