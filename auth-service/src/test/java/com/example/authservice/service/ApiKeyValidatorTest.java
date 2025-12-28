package com.example.authservice.service;

import com.example.authservice.config.ApiKeyConfig;
import com.example.authservice.service.impl.ApiKeyValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

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

        apiKeyValidator = new ApiKeyValidatorImpl(apiKeyConfig);
    }

    @Test
    @DisplayName("Should validate web API key")
    void shouldValidateWebApiKey() {
        StepVerifier.create(apiKeyValidator.isValid("test-web-key"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(apiKeyValidator.getClientType("test-web-key"))
                .expectNext("web")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should validate mobile API key")
    void shouldValidateMobileApiKey() {
        StepVerifier.create(apiKeyValidator.isValid("test-mobile-key"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(apiKeyValidator.getClientType("test-mobile-key"))
                .expectNext("mobile")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should validate platform API key")
    void shouldValidatePlatformApiKey() {
        StepVerifier.create(apiKeyValidator.isValid("test-platform-key"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(apiKeyValidator.getClientType("test-platform-key"))
                .expectNext("platform")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should reject invalid API key")
    void shouldRejectInvalidApiKey() {
        StepVerifier.create(apiKeyValidator.isValid("invalid-key"))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(apiKeyValidator.getClientType("invalid-key"))
                .verifyComplete(); // Empty Mono
    }

    @Test
    @DisplayName("Should reject null API key")
    void shouldRejectNullApiKey() {
        StepVerifier.create(apiKeyValidator.isValid(null))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(apiKeyValidator.getClientType(null))
                .verifyComplete(); // Empty Mono
    }

    @Test
    @DisplayName("Should reject blank API key")
    void shouldRejectBlankApiKey() {
        StepVerifier.create(apiKeyValidator.isValid(""))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(apiKeyValidator.isValid("   "))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return correct API key info for valid key")
    void shouldReturnCorrectApiKeyInfo() {
        StepVerifier.create(apiKeyValidator.getApiKeyInfo("test-web-key"))
                .expectNextMatches(info ->
                        "true".equals(info.get("valid")) &&
                                "web".equals(info.get("clientType"))
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return invalid info for invalid key")
    void shouldReturnInvalidInfoForInvalidKey() {
        StepVerifier.create(apiKeyValidator.getApiKeyInfo("invalid-key"))
                .expectNextMatches(info ->
                        "false".equals(info.get("valid")) &&
                                !info.containsKey("clientType")
                )
                .verifyComplete();
    }
}
