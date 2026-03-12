package com.currencyconverter.authservice.event;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an authentication-related event.
 * <p>
 * This record encapsulates details about an authentication event, such as its type,
 * the client involved, and the outcome. It is designed to be immutable.
 * </p>
 *
 * @param eventId       the unique identifier for the event.
 * @param eventType     the type of authentication event.
 * @param clientId      the identifier of the client.
 * @param clientType    the type of the client (e.g., "web", "mobile").
 * @param ipAddress     the IP address from which the event originated.
 * @param timestamp     the time at which the event occurred.
 * @param success       a flag indicating whether the authentication was successful.
 * @param failureReason the reason for failure, if applicable.
 * @param metadata      additional metadata associated with the event.
 */
public record AuthEvent(
        String eventId,
        AuthEventType eventType,
        String clientId,
        String clientType,
        String ipAddress,
        Instant timestamp,
        boolean success,
        String failureReason,
        Map<String, Object> metadata
    ) {

    /**
     * Creates a new {@link AuthEventBuilder} for constructing an {@link AuthEvent}.
     *
     * @return a new instance of {@link AuthEventBuilder}.
     */
    public static AuthEventBuilder builder() {
        return new AuthEventBuilder();
    }
}
