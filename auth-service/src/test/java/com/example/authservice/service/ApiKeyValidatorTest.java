package com.example.authservice.service;

import com.example.authservice.config.ApiKeyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiKeyValidator Tests")
class ApiKeyValidatorTest {

    private ApiKeyValidator apiKeyValidator;
    private ApiKeyConfig apiKeyConfig;

    @BeforeEach
    void setUp() {
        apiKeyConfig = new ApiKeyConfig();
        apiKeyConfig.setWeb("test-web-key");
        apiKeyConfig.setMobile("test-mobile-key");
        apiKeyConfig.setPlatform("test-platform-key");

        apiKeyValidator = new ApiKeyValidator(apiKeyConfig);
    }

    @Test
    @DisplayName("Should validate web API key")
    void shouldValidateWebApiKey() {
        assertTrue(apiKeyValidator.isValid("test-web-key"));
        assertEquals("web", apiKeyValidator.getClientType("test-web-key"));
    }

    @Test
    @DisplayName("Should validate mobile API key")
    void shouldValidateMobileApiKey() {
        assertTrue(apiKeyValidator.isValid("test-mobile-key"));
        assertEquals("mobile", apiKeyValidator.getClientType("test-mobile-key"));
    }

    @Test
    @DisplayName("Should validate platform API key")
    void shouldValidatePlatformApiKey() {
        assertTrue(apiKeyValidator.isValid("test-platform-key"));
        assertEquals("platform", apiKeyValidator.getClientType("test-platform-key"));
    }

    @Test
    @DisplayName("Should reject invalid API key")
    void shouldRejectInvalidApiKey() {
        assertFalse(apiKeyValidator.isValid("invalid-key"));
        assertNull(apiKeyValidator.getClientType("invalid-key"));
    }

    @Test
    @DisplayName("Should reject null API key")
    void shouldRejectNullApiKey() {
        assertFalse(apiKeyValidator.isValid(null));
        assertNull(apiKeyValidator.getClientType(null));
    }

    @Test
    @DisplayName("Should reject blank API key")
    void shouldRejectBlankApiKey() {
        assertFalse(apiKeyValidator.isValid(""));
        assertFalse(apiKeyValidator.isValid("   "));
    }

    @Test
    @DisplayName("Should return correct API key info for valid key")
    void shouldReturnCorrectApiKeyInfo() {
        var info = apiKeyValidator.getApiKeyInfo("test-web-key");
        assertEquals("true", info.get("valid"));
        assertEquals("web", info.get("clientType"));
    }

    @Test
    @DisplayName("Should return invalid info for invalid key")
    void shouldReturnInvalidInfoForInvalidKey() {
        var info = apiKeyValidator.getApiKeyInfo("invalid-key");
        assertEquals("false", info.get("valid"));
        assertNull(info.get("clientType"));
    }
}
