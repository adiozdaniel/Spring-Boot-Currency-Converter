package com.example.authservice.service;

import reactor.core.publisher.Mono;

/**
 * Service interface for producing authentication-related events.
 * <p>
 * This interface defines methods for publishing various authentication events
 * to a message broker, ensuring proper logging and metric collection.
 * </p>
 */
public interface AuthEventProducer {
    /**
     * Publishes an event for a successful user login.
     *
     * @param clientId   the ID of the client that logged in.
     * @param clientType the type of the client (e.g., "web", "mobile").
     * @param ipAddress  the IP address from which the login originated.
     * @return a {@link Mono<Void>} that completes when the event has been published.
     */
    Mono<Void> publishLoginSuccess(String clientId, String clientType, String ipAddress);

    /**
     * Publishes an event for a failed user login attempt.
     *
     * @param clientId   the ID of the client that attempted to log in.
     * @param clientType the type of the client.
     * @param ipAddress  the IP address from which the login attempt originated.
     * @param reason     the reason for the login failure.
     * @return a {@link Mono<Void>} that completes when the event has been published.
     */
    Mono<Void> publishLoginFailed(String clientId, String clientType, String ipAddress, String reason);

    /**
     * Publishes an event indicating an invalid API key usage.
     *
     * @param ipAddress the IP address from which the invalid API key usage originated.
     * @return a {@link Mono<Void>} that completes when the event has been published.
     */
    Mono<Void> publishInvalidApiKey(String ipAddress);

    /**
     * Publishes an event indicating that a rate limit has been exceeded.
     *
     * @param clientId  the ID of the client that exceeded the rate limit.
     * @param ipAddress the IP address from which the rate limit was exceeded.
     * @return a {@link Mono<Void>} that completes when the event has been published.
     */
    Mono<Void> publishRateLimitExceeded(String clientId, String ipAddress);

    /**
     * Publishes an event for a newly generated token.
     *
     * @param tokenId    the ID of the generated token.
     * @param clientId   the ID of the client for which the token was generated.
     * @param clientType the type of the client.
     * @param ipAddress  the IP address from which the token generation originated.
     * @return a {@link Mono<Void>} that completes when the event has been published.
     */
    Mono<Void> publishTokenGenerated(String tokenId, String clientId, String clientType, String ipAddress);

    /**
     * Publishes an event for a refreshed token.
     *
     * @param tokenId    the ID of the refreshed token.
     * @param clientId   the ID of the client for which the token was refreshed.
     * @param clientType the type of the client.
     * @param ipAddress  the IP address from which the token refresh originated.
     * @return a {@link Mono<Void>} that completes when the event has been published.
     */
    Mono<Void> publishTokenRefreshed(String tokenId, String clientId, String clientType, String ipAddress);

    /**
     * Publishes an event for a revoked token.
     *
     * @param tokenId  the ID of the revoked token.
     * @param clientId the ID of the client whose token was revoked.
     * @return a {@link Mono<Void>} that completes when the event has been published.
     */
    Mono<Void> publishTokenRevoked(String tokenId, String clientId);

    /**
     * Publishes an event for a validated token.
     *
     * @param tokenId  the ID of the validated token.
     * @param clientId the ID of the client whose token was validated.
     * @return a {@link Mono<Void>} that completes when the event has been published.
     */
    Mono<Void> publishTokenValidated(String tokenId, String clientId);
}
