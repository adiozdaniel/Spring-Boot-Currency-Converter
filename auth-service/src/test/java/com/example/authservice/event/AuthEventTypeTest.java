package com.example.authservice.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthEventType Tests")
class AuthEventTypeTest {

    @Test
    @DisplayName("Should have all expected event types")
    void shouldHaveAllExpectedEventTypes() {
        AuthEventType[] types = AuthEventType.values();

        assertEquals(10, types.length);
    }

    @Test
    @DisplayName("Should contain login success type")
    void shouldContainLoginSuccessType() {
        assertNotNull(AuthEventType.valueOf("LOGIN_SUCCESS"));
    }

    @Test
    @DisplayName("Should contain login failed type")
    void shouldContainLoginFailedType() {
        assertNotNull(AuthEventType.valueOf("LOGIN_FAILED"));
    }

    @Test
    @DisplayName("Should contain token generated type")
    void shouldContainTokenGeneratedType() {
        assertNotNull(AuthEventType.valueOf("TOKEN_GENERATED"));
    }

    @Test
    @DisplayName("Should contain token refreshed type")
    void shouldContainTokenRefreshedType() {
        assertNotNull(AuthEventType.valueOf("TOKEN_REFRESHED"));
    }

    @Test
    @DisplayName("Should contain token revoked type")
    void shouldContainTokenRevokedType() {
        assertNotNull(AuthEventType.valueOf("TOKEN_REVOKED"));
    }

    @Test
    @DisplayName("Should contain token validated type")
    void shouldContainTokenValidatedType() {
        assertNotNull(AuthEventType.valueOf("TOKEN_VALIDATED"));
    }

    @Test
    @DisplayName("Should contain token expired type")
    void shouldContainTokenExpiredType() {
        assertNotNull(AuthEventType.valueOf("TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("Should contain rate limit exceeded type")
    void shouldContainRateLimitExceededType() {
        assertNotNull(AuthEventType.valueOf("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Should contain invalid API key type")
    void shouldContainInvalidApiKeyType() {
        assertNotNull(AuthEventType.valueOf("INVALID_API_KEY"));
    }

    @Test
    @DisplayName("Should contain invalid token type")
    void shouldContainInvalidTokenType() {
        assertNotNull(AuthEventType.valueOf("INVALID_TOKEN"));
    }
}
