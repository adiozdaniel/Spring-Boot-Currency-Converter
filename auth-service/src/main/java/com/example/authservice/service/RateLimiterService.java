package com.example.authservice.service;

import reactor.core.publisher.Mono;

/**
 * Service interface for managing rate limiting operations.
 * <p>
 * This interface provides methods to control access to resources based on predefined
 * rate limits, preventing abuse and ensuring fair usage.
 * </p>
 */
public interface RateLimiterService {

    /**
     * Attempts to acquire permission for a rate-limited operation.
     *
     * @param key a unique identifier for the resource or client to be rate-limited.
     * @return a {@link Mono} emitting {@code true} if permission is granted (rate limit not exceeded),
     *         {@code false} if the rate limit is exceeded.
     */
    Mono<Boolean> tryAcquire(String key);

    /**
     * Wraps a reactive operation with rate limiting logic.
     * <p>
     * If the rate limit for the given key is exceeded, a {@link RateLimitExceededException}
     * will be thrown. Otherwise, the provided operation will be executed.
     * </p>
     * @param <T> the type of the result from the operation.
     * @param key a unique identifier for the resource or client to be rate-limited.
     * @param operation the {@link Mono} representing the operation to be rate-limited.
     * @return a {@link Mono} emitting the result of the operation if the rate limit is not exceeded.
     * @throws com.example.authservice.exception.RateLimitExceededException if the rate limit is exceeded.
     */
    <T> Mono<T> executeWithRateLimit(String key, Mono<T> operation);

    /**
     * Clears or removes the rate limiter for a specific key.
     * <p>
     * This can be used to reset the rate limit for a client or resource,
     * e.g., after a period of inactivity or successful payment.
     * </p>
     * @param key the unique identifier of the rate limiter to clear.
     * @return a {@link Mono<Void>} that completes when the rate limiter has been cleared.
     */
    Mono<Void> clearRateLimiter(String key);
}
