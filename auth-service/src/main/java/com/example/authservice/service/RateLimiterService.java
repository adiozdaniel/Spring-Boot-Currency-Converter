package com.example.authservice.service;

import com.example.authservice.config.RateLimitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    private final RateLimitConfig rateLimitConfig;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    public boolean tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);
        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            logger.warn("Rate limit exceeded for key: {}", key);
        }

        return consumed;
    }

    public long getAvailableTokens(String key) {
        Bucket bucket = buckets.get(key);
        return bucket != null ? bucket.getAvailableTokens() : rateLimitConfig.getCapacity();
    }

    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimitConfig.getCapacity())
                .refillGreedy(rateLimitConfig.getRefillTokens(),
                        Duration.ofSeconds(rateLimitConfig.getRefillDurationSeconds()))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public void clearBucket(String key) {
        buckets.remove(key);
    }
}
