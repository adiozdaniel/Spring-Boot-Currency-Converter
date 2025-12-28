package com.example.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Configuration class for mapping JSON Web Token (JWT) properties from the
 * application's configuration file.
 * <p>
 * This class is annotated with {@link ConfigurationProperties} to bind
 * properties prefixed with "jwt". The {@link RefreshScope} annotation allows
 * these properties to be refreshed dynamically without restarting the application.
 * </p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret;
    private long expiration;
    private long refreshExpiration;

    /**
     * Gets the secret key used for signing JWTs.
     *
     * @return the JWT secret key.
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Sets the secret key used for signing JWTs.
     *
     * @param secret the JWT secret key.
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Gets the expiration time for access tokens in milliseconds.
     *
     * @return the access token expiration time.
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * Sets the expiration time for access tokens in milliseconds.
     *
     * @param expiration the access token expiration time.
     */
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    /**
     * Gets the expiration time for refresh tokens in milliseconds.
     *
     * @return the refresh token expiration time.
     */
    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    /**
     * Sets the expiration time for refresh tokens in milliseconds.
     *
     * @param refreshExpiration the refresh token expiration time.
     */
    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
}
