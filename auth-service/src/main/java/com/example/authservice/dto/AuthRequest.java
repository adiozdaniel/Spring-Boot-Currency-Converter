package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthRequest {

    @NotBlank(message = "API key is required")
    private String apiKey;

    private String clientId;
    private String clientType;

    public AuthRequest() {
    }

    public AuthRequest(String apiKey, String clientId, String clientType) {
        this.apiKey = apiKey;
        this.clientId = clientId;
        this.clientType = clientType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}
