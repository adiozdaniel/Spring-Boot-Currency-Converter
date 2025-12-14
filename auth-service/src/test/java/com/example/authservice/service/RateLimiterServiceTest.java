package com.example.authservice.service;

import com.example.authservice.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimiterService Tests")
class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;
    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setCapacity(5);
        rateLimitConfig.setRefillTokens(5);
        rateLimitConfig.setRefillDurationSeconds(60);

        rateLimiterService = new RateLimiterService(rateLimitConfig);
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinRateLimit() {
        String key = "test-client";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.tryConsume(key),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Should reject requests exceeding rate limit")
    void shouldRejectRequestsExceedingRateLimit() {
        String key = "test-client-2";

        // Consume all tokens
        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryConsume(key);
        }

        // Next request should be rejected
        assertFalse(rateLimiterService.tryConsume(key));
    }

    @Test
    @DisplayName("Should track available tokens")
    void shouldTrackAvailableTokens() {
        String key = "test-client-3";

        assertEquals(5, rateLimiterService.getAvailableTokens(key));

        rateLimiterService.tryConsume(key);
        rateLimiterService.tryConsume(key);

        assertEquals(3, rateLimiterService.getAvailableTokens(key));
    }

    @Test
    @DisplayName("Should isolate rate limits per key")
    void shouldIsolateRateLimitsPerKey() {
        String key1 = "client-1";
        String key2 = "client-2";

        // Exhaust key1's tokens
        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryConsume(key1);
        }

        // key2 should still have tokens
        assertTrue(rateLimiterService.tryConsume(key2));
        assertFalse(rateLimiterService.tryConsume(key1));
    }

    @Test
    @DisplayName("Should clear bucket")
    void shouldClearBucket() {
        String key = "test-client-4";

        // Consume all tokens
        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryConsume(key);
        }

        // Clear the bucket
        rateLimiterService.clearBucket(key);

        // Should have full capacity again
        assertEquals(5, rateLimiterService.getAvailableTokens(key));
    }
}
