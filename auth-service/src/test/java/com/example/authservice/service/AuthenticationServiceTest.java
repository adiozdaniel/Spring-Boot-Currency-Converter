package com.example.authservice.service;

import com.example.authservice.config.JwtConfig;
import com.example.authservice.dto.AuthRequest;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.exception.InvalidApiKeyException;
import com.example.authservice.exception.RateLimitExceededException;
import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock
    private ApiKeyValidator apiKeyValidator;

    @Mock
    private TokenService tokenService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private JwtConfig jwtConfig;

    private MeterRegistry meterRegistry;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        lenient().when(jwtConfig.getExpiration()).thenReturn(3600000L);

        authenticationService = new AuthenticationService(
                apiKeyValidator,
                tokenService,
                rateLimiterService,
                jwtConfig,
                meterRegistry
        );
    }

    @Test
    @DisplayName("Should authenticate successfully with provided clientId")
    void shouldAuthenticateSuccessfullyWithProvidedClientId() {
        // Given
        String apiKey = "valid-api-key";
        String clientId = "client-123";
        String clientIp = "192.168.1.1";
        String clientType = "web";

        AuthRequest request = new AuthRequest(apiKey, clientId, null);

        when(rateLimiterService.tryConsume(clientIp)).thenReturn(true);
        when(apiKeyValidator.isValid(apiKey)).thenReturn(true);
        when(apiKeyValidator.getClientType(apiKey)).thenReturn(clientType);
        when(tokenService.generateAccessToken(eq(clientId), eq(clientType), anyMap()))
                .thenReturn("access-token-123");
        when(tokenService.generateRefreshToken(clientId, clientType))
                .thenReturn("refresh-token-123");

        // When
        AuthResponse response = authenticationService.authenticate(request, clientIp);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-123");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);

        verify(rateLimiterService).tryConsume(clientIp);
        verify(apiKeyValidator).isValid(apiKey);
        verify(apiKeyValidator).getClientType(apiKey);
        verify(tokenService).generateAccessToken(eq(clientId), eq(clientType), anyMap());
        verify(tokenService).generateRefreshToken(clientId, clientType);
    }

    @Test
    @DisplayName("Should authenticate successfully without clientId and generate UUID")
    void shouldAuthenticateSuccessfullyWithoutClientId() {
        // Given
        String apiKey = "valid-api-key";
        String clientIp = "192.168.1.1";
        String clientType = "mobile";

        AuthRequest request = new AuthRequest(apiKey, null, null);

        when(rateLimiterService.tryConsume(clientIp)).thenReturn(true);
        when(apiKeyValidator.isValid(apiKey)).thenReturn(true);
        when(apiKeyValidator.getClientType(apiKey)).thenReturn(clientType);
        when(tokenService.generateAccessToken(anyString(), eq(clientType), anyMap()))
                .thenReturn("access-token-456");
        when(tokenService.generateRefreshToken(anyString(), eq(clientType)))
                .thenReturn("refresh-token-456");

        // When
        AuthResponse response = authenticationService.authenticate(request, clientIp);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-456");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        ArgumentCaptor<String> clientIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(tokenService).generateAccessToken(clientIdCaptor.capture(), eq(clientType), anyMap());

        // Verify that a UUID was generated (should match UUID pattern)
        String capturedClientId = clientIdCaptor.getValue();
        assertThat(capturedClientId).isNotNull();
        assertThat(capturedClientId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Should authenticate successfully with null clientIp")
    void shouldAuthenticateSuccessfullyWithNullClientIp() {
        // Given
        String apiKey = "valid-api-key";
        String clientId = "client-123";
        String clientType = "web";

        AuthRequest request = new AuthRequest(apiKey, clientId, null);

        when(rateLimiterService.tryConsume("unknown")).thenReturn(true);
        when(apiKeyValidator.isValid(apiKey)).thenReturn(true);
        when(apiKeyValidator.getClientType(apiKey)).thenReturn(clientType);
        when(tokenService.generateAccessToken(eq(clientId), eq(clientType), anyMap()))
                .thenReturn("access-token-789");
        when(tokenService.generateRefreshToken(clientId, clientType))
                .thenReturn("refresh-token-789");

        // When
        AuthResponse response = authenticationService.authenticate(request, null);

        // Then
        assertThat(response).isNotNull();
        verify(rateLimiterService).tryConsume("unknown");

        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(tokenService).generateAccessToken(eq(clientId), eq(clientType), claimsCaptor.capture());

        Map<String, Object> capturedClaims = claimsCaptor.getValue();
        assertThat(capturedClaims).containsEntry("ip", null);
    }

    @Test
    @DisplayName("Should throw RateLimitExceededException when rate limit exceeded")
    void shouldThrowRateLimitExceededExceptionWhenRateLimitExceeded() {
        // Given
        String apiKey = "valid-api-key";
        String clientId = "client-123";
        String clientIp = "192.168.1.1";

        AuthRequest request = new AuthRequest(apiKey, clientId, null);

        when(rateLimiterService.tryConsume(clientIp)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(request, clientIp))
                .isInstanceOf(RateLimitExceededException.class);

        verify(rateLimiterService).tryConsume(clientIp);
        verify(apiKeyValidator, never()).isValid(anyString());
        verify(tokenService, never()).generateAccessToken(anyString(), anyString(), anyMap());

        // Verify failure counter was incremented
        assertThat(meterRegistry.counter("auth.attempts", "result", "failure").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should throw InvalidApiKeyException when API key is invalid")
    void shouldThrowInvalidApiKeyExceptionWhenApiKeyIsInvalid() {
        // Given
        String apiKey = "invalid-api-key";
        String clientId = "client-123";
        String clientIp = "192.168.1.1";

        AuthRequest request = new AuthRequest(apiKey, clientId, null);

        when(rateLimiterService.tryConsume(clientIp)).thenReturn(true);
        when(apiKeyValidator.isValid(apiKey)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(request, clientIp))
                .isInstanceOf(InvalidApiKeyException.class);

        verify(rateLimiterService).tryConsume(clientIp);
        verify(apiKeyValidator).isValid(apiKey);
        verify(tokenService, never()).generateAccessToken(anyString(), anyString(), anyMap());

        // Verify failure counter was incremented
        assertThat(meterRegistry.counter("auth.attempts", "result", "failure").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should pass clientIp in additional claims")
    void shouldPassClientIpInAdditionalClaims() {
        // Given
        String apiKey = "valid-api-key";
        String clientId = "client-123";
        String clientIp = "192.168.1.1";
        String clientType = "web";

        AuthRequest request = new AuthRequest(apiKey, clientId, null);

        when(rateLimiterService.tryConsume(clientIp)).thenReturn(true);
        when(apiKeyValidator.isValid(apiKey)).thenReturn(true);
        when(apiKeyValidator.getClientType(apiKey)).thenReturn(clientType);
        when(tokenService.generateAccessToken(eq(clientId), eq(clientType), anyMap()))
                .thenReturn("access-token");
        when(tokenService.generateRefreshToken(clientId, clientType))
                .thenReturn("refresh-token");

        // When
        authenticationService.authenticate(request, clientIp);

        // Then
        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(tokenService).generateAccessToken(eq(clientId), eq(clientType), claimsCaptor.capture());

        Map<String, Object> additionalClaims = claimsCaptor.getValue();
        assertThat(additionalClaims).containsEntry("ip", clientIp);
    }

    @Test
    @DisplayName("Should increment success counter on successful authentication")
    void shouldIncrementSuccessCounterOnSuccessfulAuthentication() {
        // Given
        String apiKey = "valid-api-key";
        String clientId = "client-123";
        String clientIp = "192.168.1.1";
        String clientType = "web";

        AuthRequest request = new AuthRequest(apiKey, clientId, null);

        when(rateLimiterService.tryConsume(clientIp)).thenReturn(true);
        when(apiKeyValidator.isValid(apiKey)).thenReturn(true);
        when(apiKeyValidator.getClientType(apiKey)).thenReturn(clientType);
        when(tokenService.generateAccessToken(eq(clientId), eq(clientType), anyMap()))
                .thenReturn("access-token");
        when(tokenService.generateRefreshToken(clientId, clientType))
                .thenReturn("refresh-token");

        // When
        authenticationService.authenticate(request, clientIp);

        // Then
        assertThat(meterRegistry.counter("auth.attempts", "result", "success").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() {
        // Given
        String oldRefreshToken = "old-refresh-token";
        String clientId = "client-123";
        String clientType = "web";

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(clientId);
        when(claims.get("clientType", String.class)).thenReturn(clientType);

        when(tokenService.validateRefreshToken(oldRefreshToken)).thenReturn(claims);
        when(tokenService.generateAccessToken(clientId, clientType, null))
                .thenReturn("new-access-token");
        when(tokenService.generateRefreshToken(clientId, clientType))
                .thenReturn("new-refresh-token");

        // When
        AuthResponse response = authenticationService.refreshToken(oldRefreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);

        verify(tokenService).validateRefreshToken(oldRefreshToken);
        verify(tokenService).revokeToken(oldRefreshToken);
        verify(tokenService).generateAccessToken(clientId, clientType, null);
        verify(tokenService).generateRefreshToken(clientId, clientType);
    }

    @Test
    @DisplayName("Should revoke old refresh token when creating new tokens")
    void shouldRevokeOldRefreshTokenWhenCreatingNewTokens() {
        // Given
        String oldRefreshToken = "old-refresh-token";
        String clientId = "client-123";
        String clientType = "mobile";

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(clientId);
        when(claims.get("clientType", String.class)).thenReturn(clientType);

        when(tokenService.validateRefreshToken(oldRefreshToken)).thenReturn(claims);
        when(tokenService.generateAccessToken(clientId, clientType, null))
                .thenReturn("new-access-token");
        when(tokenService.generateRefreshToken(clientId, clientType))
                .thenReturn("new-refresh-token");

        // When
        authenticationService.refreshToken(oldRefreshToken);

        // Then
        verify(tokenService).revokeToken(oldRefreshToken);
    }

    @Test
    @DisplayName("Should revoke token successfully")
    void shouldRevokeTokenSuccessfully() {
        // Given
        String token = "token-to-revoke";

        // When
        authenticationService.revokeToken(token);

        // Then
        verify(tokenService).revokeToken(token);
    }

    @Test
    @DisplayName("Should validate token and return claims")
    void shouldValidateTokenAndReturnClaims() {
        // Given
        String token = "valid-token";
        String clientId = "client-123";
        String clientType = "web";
        Date issuedAt = new Date(System.currentTimeMillis());
        Date expiresAt = new Date(System.currentTimeMillis() + 3600000);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(clientId);
        when(claims.get("clientType")).thenReturn(clientType);
        when(claims.getIssuedAt()).thenReturn(issuedAt);
        when(claims.getExpiration()).thenReturn(expiresAt);

        when(tokenService.validateToken(token)).thenReturn(claims);

        // When
        Map<String, Object> result = authenticationService.validateToken(token);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsEntry("valid", true);
        assertThat(result).containsEntry("subject", clientId);
        assertThat(result).containsEntry("clientType", clientType);
        assertThat(result).containsEntry("expiresAt", expiresAt);
        assertThat(result).containsEntry("issuedAt", issuedAt);

        verify(tokenService).validateToken(token);
    }

    @Test
    @DisplayName("Should validate token and return all required fields")
    void shouldValidateTokenAndReturnAllRequiredFields() {
        // Given
        String token = "valid-token";
        String clientId = "client-456";
        String clientType = "platform";
        Date issuedAt = new Date(System.currentTimeMillis() - 1000000);
        Date expiresAt = new Date(System.currentTimeMillis() + 2000000);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(clientId);
        when(claims.get("clientType")).thenReturn(clientType);
        when(claims.getIssuedAt()).thenReturn(issuedAt);
        when(claims.getExpiration()).thenReturn(expiresAt);

        when(tokenService.validateToken(token)).thenReturn(claims);

        // When
        Map<String, Object> result = authenticationService.validateToken(token);

        // Then
        assertThat(result).hasSize(5);
        assertThat(result.get("valid")).isEqualTo(true);
        assertThat(result.get("subject")).isEqualTo(clientId);
        assertThat(result.get("clientType")).isEqualTo(clientType);
        assertThat(result.get("expiresAt")).isEqualTo(expiresAt);
        assertThat(result.get("issuedAt")).isEqualTo(issuedAt);
    }
}
