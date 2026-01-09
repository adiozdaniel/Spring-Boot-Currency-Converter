package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for refresh token requests.
 * <p>
 * This class encapsulates the refresh token sent by a client to obtain a new
 * access token.
 * </p>
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    /**
     * Default constructor.
     */
    public RefreshTokenRequest() {
    }

    /**
     * Constructs a new {@link RefreshTokenRequest} with the specified refresh token.
     *
     * @param refreshToken the refresh token.
     */
    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
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
}
