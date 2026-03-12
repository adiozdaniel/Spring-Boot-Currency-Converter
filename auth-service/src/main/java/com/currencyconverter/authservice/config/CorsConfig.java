package com.currencyconverter.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration class for mapping Cross-Origin Resource Sharing (CORS) properties
 * from the application's configuration file.
 * <p>
 * This class is annotated with {@link ConfigurationProperties} to bind
 * properties prefixed with "cors". The {@link RefreshScope} annotation allows
 * these properties to be refreshed dynamically without restarting the application.
 * </p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "cors")
public class CorsConfig {

    private List<String> allowedOrigins;
    private List<String> allowedMethods;
    private List<String> allowedHeaders;
    private boolean allowCredentials;
    private long maxAge;

    /**
     * Gets the list of allowed origins for CORS requests.
     *
     * @return a list of allowed origins.
     */
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Sets the list of allowed origins for CORS requests.
     *
     * @param allowedOrigins a list of allowed origins.
     */
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Gets the list of allowed HTTP methods for CORS requests.
     *
     * @return a list of allowed HTTP methods.
     */
    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    /**
     * Sets the list of allowed HTTP methods for CORS requests.
     *
     * @param allowedMethods a list of allowed HTTP methods.
     */
    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    /**
     * Gets the list of allowed headers for CORS requests.
     *
     * @return a list of allowed headers.
     */
    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    /**
     * Sets the list of allowed headers for CORS requests.
     *
     * @param allowedHeaders a list of allowed headers.
     */
    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    /**
     * Checks if credentials are to be allowed in CORS requests.
     *
     * @return true if credentials are allowed, false otherwise.
     */
    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    /**
     * Sets whether credentials are to be allowed in CORS requests.
     *
     * @param allowCredentials true to allow credentials, false otherwise.
     */
    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    /**
     * Gets the maximum age (in seconds) for which the CORS pre-flight response
     * should be cached.
     *
     * @return the maximum age in seconds.
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * Sets the maximum age (in seconds) for which the CORS pre-flight response
     * should be cached.
     *
     * @param maxAge the maximum age in seconds.
     */
    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}
