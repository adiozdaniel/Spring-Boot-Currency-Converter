package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for authentication requests.
 * <p>
 * This class encapsulates the data required to authenticate a client, including
 * the API key, client ID, and client type.
 * </p>
 */
public class AuthRequest {

    @NotBlank(message = "API key is required")
    private String apiKey;

    private String clientId;
    private String clientType;

    /**
     * Default constructor.
     */
    public AuthRequest() {
    }

    /**
     * Constructs a new {@link AuthRequest} with the specified details.
     *
     * @param apiKey     the client's API key.
     * @param clientId   the unique identifier for the client.
     * @param clientType the type of the client (e.g., "web", "mobile").
     */
    public AuthRequest(String apiKey, String clientId, String clientType) {
        this.apiKey = apiKey;
        this.clientId = clientId;
        this.clientType = clientType;
    }

    /**
     * Gets the API key.
     *
     * @return the API key.
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets the API key.
     *
     * @param apiKey the API key.
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Gets the client ID.
     *
     * @return the client ID.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the client ID.
     *
     * @param clientId the client ID.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets the client type.
     *
     * @return the client type.
     */
    public String getClientType() {
        return clientType;
    }

    /**
     * Sets the client type.
     *
     * @param clientType the client type.
     */
    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}
