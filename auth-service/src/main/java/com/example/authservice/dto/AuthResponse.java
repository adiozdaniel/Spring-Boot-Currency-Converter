package com.example.authservice.dto;

/**
 * Data Transfer Object (DTO) for authentication responses.
 * <p>
 * This class encapsulates the data returned to a client after a successful
 * authentication, including the access token, refresh token, token type, and
 * expiration time.
 * </p>
 */
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;

    /**
     * Default constructor.
     */
    public AuthResponse() {
    }

    /**
     * Constructs a new {@link AuthResponse} with the specified token details.
     *
     * @param accessToken  the access token.
     * @param refreshToken the refresh token.
     * @param tokenType    the type of the token (e.g., "Bearer").
     * @param expiresIn    the expiration time of the access token in seconds.
     */
    public AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    /**
     * Gets the access token.
     *
     * @return the access token.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token.
     *
     * @param accessToken the access token.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Gets the refresh token.
     *
     * @return the refresh token.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token.
     *
     * @param refreshToken the refresh token.
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Gets the token type.
     *
     * @return the token type.
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the token type.
     *
     * @param tokenType the token type.
     */
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Gets the expiration time of the access token in seconds.
     *
     * @return the expiration time in seconds.
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the expiration time of the access token in seconds.
     *
     * @param expiresIn the expiration time in seconds.
     */
    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
