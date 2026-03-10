package com.example.authservice.service;

import com.example.authservice.config.RateLimitConfig;
import com.example.authservice.service.impl.RateLimiterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("RateLimiterService Tests")
class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;
    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setRefillTokens(5);
        rateLimitConfig.setRefillDurationSeconds(60);

        rateLimiterService = new RateLimiterServiceImpl(rateLimitConfig);
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinRateLimit() {
        String key = "test-client";

        for (int i = 0; i < 5; i++) {
            StepVerifier.create(rateLimiterService.tryAcquire(key))
                    .expectNext(true)
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("Should reject requests exceeding rate limit")
    void shouldRejectRequestsExceedingRateLimit() {
        String key = "test-client-2";

        // Consume all tokens
        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryAcquire(key).block();
        }

        // Next request should be rejected
        StepVerifier.create(rateLimiterService.tryAcquire(key))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should isolate rate limits per key")
    void shouldIsolateRateLimitsPerKey() {
        String key1 = "client-1";
        String key2 = "client-2";

        // Exhaust key1's tokens
        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryAcquire(key1).block();
        }

        // key2 should still have tokens
        StepVerifier.create(rateLimiterService.tryAcquire(key2))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(rateLimiterService.tryAcquire(key1))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should clear rate limiter")
    void shouldClearRateLimiter() {
        String key = "test-client-4";

        // Consume all tokens
        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryAcquire(key).block();
        }

        // Verify no more tokens
        StepVerifier.create(rateLimiterService.tryAcquire(key))
                .expectNext(false)
                .verifyComplete();

        // Clear the rate limiter
        StepVerifier.create(rateLimiterService.clearRateLimiter(key))
                .verifyComplete();

        // Should have full capacity again
        StepVerifier.create(rateLimiterService.tryAcquire(key))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should execute operation with rate limit")
    void shouldExecuteOperationWithRateLimit() {
        String key = "test-client-5";
        Mono<String> operation = Mono.just("success");

        StepVerifier.create(rateLimiterService.executeWithRateLimit(key, operation))
                .expectNext("success")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should allow multiple operations within rate limit")
    void shouldAllowMultipleOperationsWithinRateLimit() {
        String key = "test-client-6";

        for (int i = 0; i < 5; i++) {
            Mono<String> operation = Mono.just("success-" + i);
            StepVerifier.create(rateLimiterService.executeWithRateLimit(key, operation))
                    .expectNext("success-" + i)
                    .verifyComplete();
        }
    }
}
