package com.example.authservice.service;

import com.example.authservice.config.ApiKeyConfig;
import com.example.authservice.service.impl.ApiKeyValidatorImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;

@DisplayName("ApiKeyValidator Tests")
class ApiKeyValidatorTest {

        private ApiKeyValidator apiKeyValidator;
        private ApiKeyConfig apiKeyConfig;

        private static final String WEB_KEY = "test-web-api-key-12345";
        private static final String MOBILE_KEY = "test-mobile-api-key-67890";
        private static final String PLATFORM_KEY = "test-platform-api-key-abcde";
        private static final String INVALID_KEY = "invalid-api-key";

        @BeforeEach
        void setUp() {
                apiKeyConfig = new ApiKeyConfig();
                apiKeyConfig.setWeb(WEB_KEY);
                apiKeyConfig.setMobile(MOBILE_KEY);
                apiKeyConfig.setPlatform(PLATFORM_KEY);

                apiKeyValidator = new ApiKeyValidatorImpl(apiKeyConfig);
        }

        @Nested
        @DisplayName("isValid() Tests")
        class IsValidTests {

                @Test
                @DisplayName("Should return true for valid web API key")
                void shouldReturnTrueForValidWebApiKey() {
                        StepVerifier.create(apiKeyValidator.isValid(WEB_KEY))
                                        .expectNext(true)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return true for valid mobile API key")
                void shouldReturnTrueForValidMobileApiKey() {
                        StepVerifier.create(apiKeyValidator.isValid(MOBILE_KEY))
                                        .expectNext(true)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return true for valid platform API key")
                void shouldReturnTrueForValidPlatformApiKey() {
                        StepVerifier.create(apiKeyValidator.isValid(PLATFORM_KEY))
                                        .expectNext(true)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return false for invalid API key")
                void shouldReturnFalseForInvalidApiKey() {
                        StepVerifier.create(apiKeyValidator.isValid(INVALID_KEY))
                                        .expectNext(false)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return false for null API key")
                void shouldReturnFalseForNullApiKey() {
                        StepVerifier.create(apiKeyValidator.isValid(null))
                                        .expectNext(false)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return false for empty API key")
                void shouldReturnFalseForEmptyApiKey() {
                        StepVerifier.create(apiKeyValidator.isValid(""))
                                        .expectNext(false)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return false for blank API key")
                void shouldReturnFalseForBlankApiKey() {
                        StepVerifier.create(apiKeyValidator.isValid("   "))
                                        .expectNext(false)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return false for whitespace-only API key")
                void shouldReturnFalseForWhitespaceApiKey() {
                        StepVerifier.create(apiKeyValidator.isValid("  \t\n  "))
                                        .expectNext(false)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should be case-sensitive for API keys")
                void shouldBeCaseSensitiveForApiKeys() {
                        StepVerifier.create(apiKeyValidator.isValid(WEB_KEY.toUpperCase()))
                                        .expectNext(false)
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("getClientType() Tests")
        class GetClientTypeTests {

                @Test
                @DisplayName("Should return 'web' for web API key")
                void shouldReturnWebForWebApiKey() {
                        StepVerifier.create(apiKeyValidator.getClientType(WEB_KEY))
                                        .expectNext("web")
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return 'mobile' for mobile API key")
                void shouldReturnMobileForMobileApiKey() {
                        StepVerifier.create(apiKeyValidator.getClientType(MOBILE_KEY))
                                        .expectNext("mobile")
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return 'platform' for platform API key")
                void shouldReturnPlatformForPlatformApiKey() {
                        StepVerifier.create(apiKeyValidator.getClientType(PLATFORM_KEY))
                                        .expectNext("platform")
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return empty Mono for invalid API key")
                void shouldReturnEmptyMonoForInvalidApiKey() {
                        StepVerifier.create(apiKeyValidator.getClientType(INVALID_KEY))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return empty Mono for null API key")
                void shouldReturnEmptyMonoForNullApiKey() {
                        StepVerifier.create(apiKeyValidator.getClientType(null))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return empty Mono for empty API key")
                void shouldReturnEmptyMonoForEmptyApiKey() {
                        StepVerifier.create(apiKeyValidator.getClientType(""))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return empty Mono for blank API key")
                void shouldReturnEmptyMonoForBlankApiKey() {
                        StepVerifier.create(apiKeyValidator.getClientType("   "))
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("getApiKeyInfo() Tests")
        class GetApiKeyInfoTests {

                @Test
                @DisplayName("Should return info with valid=true and clientType=web for web API key")
                void shouldReturnInfoForWebApiKey() {
                        StepVerifier.create(apiKeyValidator.getApiKeyInfo(WEB_KEY))
                                        .expectNextMatches(info -> "true".equals(info.get("valid")) &&
                                                        "web".equals(info.get("clientType")) &&
                                                        info.size() == 2)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return info with valid=true and clientType=mobile for mobile API key")
                void shouldReturnInfoForMobileApiKey() {
                        StepVerifier.create(apiKeyValidator.getApiKeyInfo(MOBILE_KEY))
                                        .expectNextMatches(info -> "true".equals(info.get("valid")) &&
                                                        "mobile".equals(info.get("clientType")) &&
                                                        info.size() == 2)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return info with valid=true and clientType=platform for platform API key")
                void shouldReturnInfoForPlatformApiKey() {
                        StepVerifier.create(apiKeyValidator.getApiKeyInfo(PLATFORM_KEY))
                                        .expectNextMatches(info -> "true".equals(info.get("valid")) &&
                                                        "platform".equals(info.get("clientType")) &&
                                                        info.size() == 2)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return info with valid=false for invalid API key")
                void shouldReturnInvalidInfoForInvalidApiKey() {
                        StepVerifier.create(apiKeyValidator.getApiKeyInfo(INVALID_KEY))
                                        .expectNextMatches(info -> "false".equals(info.get("valid")) &&
                                                        !info.containsKey("clientType") &&
                                                        info.size() == 1)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return info with valid=false for null API key")
                void shouldReturnInvalidInfoForNullApiKey() {
                        StepVerifier.create(apiKeyValidator.getApiKeyInfo(null))
                                        .expectNextMatches(info -> "false".equals(info.get("valid")) &&
                                                        !info.containsKey("clientType"))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return info with valid=false for empty API key")
                void shouldReturnInvalidInfoForEmptyApiKey() {
                        StepVerifier.create(apiKeyValidator.getApiKeyInfo(""))
                                        .expectNextMatches(info -> "false".equals(info.get("valid")) &&
                                                        !info.containsKey("clientType"))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should return info with valid=false for blank API key")
                void shouldReturnInvalidInfoForBlankApiKey() {
                        StepVerifier.create(apiKeyValidator.getApiKeyInfo("   "))
                                        .expectNextMatches(info -> "false".equals(info.get("valid")) &&
                                                        !info.containsKey("clientType"))
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("Integration Tests")
        class IntegrationTests {

                @Test
                @DisplayName("Should consistently identify same API key across all methods")
                void shouldConsistentlyIdentifySameApiKey() {
                        // Verify web key is consistently recognized
                        StepVerifier.create(apiKeyValidator.isValid(WEB_KEY))
                                        .expectNext(true)
                                        .verifyComplete();

                        StepVerifier.create(apiKeyValidator.getClientType(WEB_KEY))
                                        .expectNext("web")
                                        .verifyComplete();

                        StepVerifier.create(apiKeyValidator.getApiKeyInfo(WEB_KEY))
                                        .expectNextMatches(info -> "true".equals(info.get("valid")) &&
                                                        "web".equals(info.get("clientType")))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should differentiate between different valid API keys")
                void shouldDifferentiateBetweenDifferentValidApiKeys() {
                        // Web key
                        StepVerifier.create(apiKeyValidator.getClientType(WEB_KEY))
                                        .expectNext("web")
                                        .verifyComplete();

                        // Mobile key
                        StepVerifier.create(apiKeyValidator.getClientType(MOBILE_KEY))
                                        .expectNext("mobile")
                                        .verifyComplete();

                        // Platform key
                        StepVerifier.create(apiKeyValidator.getClientType(PLATFORM_KEY))
                                        .expectNext("platform")
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should handle multiple validation requests reactively")
                void shouldHandleMultipleValidationRequestsReactively() {
                        StepVerifier.create(
                                        apiKeyValidator.isValid(WEB_KEY)
                                                        .zipWith(apiKeyValidator.isValid(MOBILE_KEY))
                                                        .zipWith(apiKeyValidator.isValid(PLATFORM_KEY)))
                                        .expectNextMatches(tuple -> {
                                                Boolean webValid = tuple.getT1().getT1();
                                                Boolean mobileValid = tuple.getT1().getT2();
                                                Boolean platformValid = tuple.getT2();
                                                return webValid && mobileValid && platformValid;
                                        })
                                        .verifyComplete();
                }
        }

        @Nested
        @DisplayName("Edge Cases")
        class EdgeCaseTests {

                @Test
                @DisplayName("Should handle very long API key")
                void shouldHandleVeryLongApiKey() {
                        String longKey = "a".repeat(1000);
                        StepVerifier.create(apiKeyValidator.isValid(longKey))
                                        .expectNext(false)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should handle API key with special characters")
                void shouldHandleApiKeyWithSpecialCharacters() {
                        StepVerifier.create(apiKeyValidator.isValid("key-with-!@#$%^&*()"))
                                        .expectNext(false)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should handle API key with unicode characters")
                void shouldHandleApiKeyWithUnicodeCharacters() {
                        StepVerifier.create(apiKeyValidator.isValid("key-with-émojis-🔑"))
                                        .expectNext(false)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should handle partial match of valid key")
                void shouldHandlePartialMatchOfValidKey() {
                        String partialKey = WEB_KEY.substring(0, WEB_KEY.length() - 1);
                        StepVerifier.create(apiKeyValidator.isValid(partialKey))
                                        .expectNext(false)
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Should handle key with extra characters")
                void shouldHandleKeyWithExtraCharacters() {
                        String keyWithExtra = WEB_KEY + "extra";
                        StepVerifier.create(apiKeyValidator.isValid(keyWithExtra))
                                        .expectNext(false)
                                        .verifyComplete();
                }
        }
}
