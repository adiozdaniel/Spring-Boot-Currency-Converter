package com.example.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Configuration class for mapping rate limiting properties from the application's
 * configuration file.
 * <p>
 * This class is annotated with {@link ConfigurationProperties} to bind
 * properties prefixed with "rate-limit". The {@link RefreshScope} annotation
 * allows these properties to be refreshed dynamically without restarting the
 * application.
 * </p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    /**
     * Maximum number of requests allowed per period.
     */
    private int refillTokens = 10;

    /**
     * Duration of the rate limit period in seconds.
     */
    private long refillDurationSeconds = 1;

    /**
     * Gets the maximum number of requests allowed per period.
     *
     * @return the number of refill tokens.
     */
    public int getRefillTokens() {
        return refillTokens;
    }

    /**
     * Sets the maximum number of requests allowed per period.
     *
     * @param refillTokens the number of refill tokens.
     */
    public void setRefillTokens(int refillTokens) {
        this.refillTokens = refillTokens;
    }

    /**
     * Gets the duration of the rate limit period in seconds.
     *
     * @return the refill duration in seconds.
     */
    public long getRefillDurationSeconds() {
        return refillDurationSeconds;
    }

    /**
     * Sets the duration of the rate limit period in seconds.
     *
     * @param refillDurationSeconds the refill duration in seconds.
     */
    public void setRefillDurationSeconds(long refillDurationSeconds) {
        this.refillDurationSeconds = refillDurationSeconds;
    }
}
