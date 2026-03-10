package com.example.authservice.service.impl;

import com.example.authservice.config.RateLimitConfig;
import com.example.authservice.service.RateLimiterService;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Implementation of the {@link RateLimiterService} interface using Resilience4j's RateLimiter.
 * <p>
 * This service provides methods to apply and manage rate limits for various operations
 * or clients based on a configured refill rate and capacity.
 * </p>
 */
@Service
public class RateLimiterServiceImpl implements RateLimiterService {

  private static final Logger logger = LoggerFactory.getLogger(RateLimiterServiceImpl.class);

  private final RateLimiterRegistry rateLimiterRegistry;

  /**
   * Constructs a new {@link RateLimiterServiceImpl} with the provided rate limit configuration.
   * <p>
   * Initializes the Resilience4j {@link RateLimiterRegistry} with a default configuration
   * based on the {@link RateLimitConfig} properties.
   * </p>
   * @param rateLimitConfig the configuration properties for rate limiting.
   */
  public RateLimiterServiceImpl(RateLimitConfig rateLimitConfig) {
    RateLimiterConfig config = RateLimiterConfig.custom()
        .limitRefreshPeriod(Duration.ofSeconds(rateLimitConfig.getRefillDurationSeconds()))
        .limitForPeriod(rateLimitConfig.getRefillTokens())
        .timeoutDuration(Duration.ZERO) // No waiting for permission
        .build();

    this.rateLimiterRegistry = RateLimiterRegistry.of(config);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Boolean> tryAcquire(String key) {
    return Mono.fromCallable(() -> {
      RateLimiter rateLimiter = getRateLimiter(key);
      boolean permission = rateLimiter.acquirePermission();

      if (!permission) {
        logger.warn("Rate limit exceeded for key: {}", key);
      }

      return permission;
    })
        .subscribeOn(Schedulers.boundedElastic()); // Use a bounded elastic scheduler for blocking operations if any
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Mono<T> executeWithRateLimit(String key, Mono<T> operation) {
    RateLimiter rateLimiter = getRateLimiter(key);

    return operation
        .transformDeferred(RateLimiterOperator.of(rateLimiter))
        .doOnError(RequestNotPermitted.class,
            ex -> logger.warn("Rate limit exceeded for key: {}", key));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> clearRateLimiter(String key) {
    return Mono.fromRunnable(() -> {
      rateLimiterRegistry.remove(key);
      logger.info("Cleared RateLimiter for key: {}", key);
    });
  }

  /**
   * Retrieves an existing {@link RateLimiter} instance for the given key from the
   * registry, or creates a new one with the default configuration if it doesn't exist.
   *
   * @param key the unique identifier for the rate limiter.
   * @return a {@link RateLimiter} instance.
   */
  private RateLimiter getRateLimiter(String key) {
    // RateLimiterRegistry already caches instances internally.
    return rateLimiterRegistry.rateLimiter(key);
  }
}