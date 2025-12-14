package com.example.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    private int capacity;
    private int refillTokens;
    private long refillDurationSeconds;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(int refillTokens) {
        this.refillTokens = refillTokens;
    }

    public long getRefillDurationSeconds() {
        return refillDurationSeconds;
    }

    public void setRefillDurationSeconds(long refillDurationSeconds) {
        this.refillDurationSeconds = refillDurationSeconds;
    }
}
