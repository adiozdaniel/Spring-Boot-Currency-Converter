package com.currencyconverter.authservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for token revocation requests.
 * <p>
 * This class encapsulates the token that a client wishes to revoke.
 * </p>
 */
public class RevokeTokenRequest {

    @NotBlank(message = "Token is required")
    private String token;

    /**
     * Default constructor.
     */
    public RevokeTokenRequest() {
    }

    /**
     * Constructs a new {@link RevokeTokenRequest} with the specified token.
     *
     * @param token the token to be revoked.
     */
    public RevokeTokenRequest(String token) {
        this.token = token;
    }

    /**
     * Gets the token to be revoked.
     *
     * @return the token.
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the token to be revoked.
     *
     * @param token the token.
     */
    public void setToken(String token) {
        this.token = token;
    }
}
