package com.example.authservice.service;

import com.example.authservice.config.JwtConfig;
import com.example.authservice.dto.AuthRequest;
import com.example.authservice.exception.InvalidApiKeyException;
import com.example.authservice.exception.RateLimitExceededException;
import com.example.authservice.service.impl.AuthenticationServiceImpl;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
@SuppressWarnings("unchecked")
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

                authenticationService = new AuthenticationServiceImpl(
                                apiKeyValidator,
                                tokenService,
                                rateLimiterService,
                                null, // AuthEventProducer not needed for tests
                                jwtConfig,
                                meterRegistry);
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

                when(apiKeyValidator.isValid(apiKey)).thenReturn(Mono.just(true));
                when(apiKeyValidator.getClientType(apiKey)).thenReturn(Mono.just(clientType));
                when(tokenService.generateAccessToken(eq(clientId), eq(clientType), anyMap()))
                                .thenReturn(Mono.just("access-token-123"));
                when(tokenService.generateRefreshToken(clientId, clientType))
                                .thenReturn(Mono.just("refresh-token-123"));
                when(tokenService.extractTokenId(anyString())).thenReturn(Mono.just("token-id-123"));
                when(rateLimiterService.executeWithRateLimit(eq(clientIp), any()))
                                .thenAnswer(invocation -> invocation.getArgument(1));

                // When & Then
                StepVerifier.create(authenticationService.authenticate(request, clientIp))
                                .assertNext(response -> {
                                        assertThat(response).isNotNull();
                                        assertThat(response.getAccessToken()).isEqualTo("access-token-123");
                                        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-123");
                                        assertThat(response.getTokenType()).isEqualTo("Bearer");
                                        assertThat(response.getExpiresIn()).isEqualTo(3600L);
                                })
                                .verifyComplete();

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

                when(apiKeyValidator.isValid(apiKey)).thenReturn(Mono.just(true));
                when(apiKeyValidator.getClientType(apiKey)).thenReturn(Mono.just(clientType));
                when(tokenService.generateAccessToken(anyString(), eq(clientType), anyMap()))
                                .thenReturn(Mono.just("access-token-456"));
                when(tokenService.generateRefreshToken(anyString(), eq(clientType)))
                                .thenReturn(Mono.just("refresh-token-456"));
                when(tokenService.extractTokenId(anyString())).thenReturn(Mono.just("token-id-456"));
                when(rateLimiterService.executeWithRateLimit(eq(clientIp), any()))
                                .thenAnswer(invocation -> invocation.getArgument(1));

                // When & Then
                StepVerifier.create(authenticationService.authenticate(request, clientIp))
                                .assertNext(response -> {
                                        assertThat(response).isNotNull();
                                        assertThat(response.getAccessToken()).isEqualTo("access-token-456");
                                        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
                                        assertThat(response.getTokenType()).isEqualTo("Bearer");
                                })
                                .verifyComplete();

                ArgumentCaptor<String> clientIdCaptor = ArgumentCaptor.forClass(String.class);
                verify(tokenService).generateAccessToken(clientIdCaptor.capture(), eq(clientType), anyMap());

                // Verify that a UUID was generated
                String capturedClientId = clientIdCaptor.getValue();
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

                when(apiKeyValidator.isValid(apiKey)).thenReturn(Mono.just(true));
                when(apiKeyValidator.getClientType(apiKey)).thenReturn(Mono.just(clientType));
                when(tokenService.generateAccessToken(eq(clientId), eq(clientType), anyMap()))
                                .thenReturn(Mono.just("access-token-789"));
                when(tokenService.generateRefreshToken(clientId, clientType))
                                .thenReturn(Mono.just("refresh-token-789"));
                when(tokenService.extractTokenId(anyString())).thenReturn(Mono.just("token-id-789"));
                when(rateLimiterService.executeWithRateLimit(eq("unknown"), any()))
                                .thenAnswer(invocation -> invocation.getArgument(1));

                // When & Then
                StepVerifier.create(authenticationService.authenticate(request, null))
                                .assertNext(response -> assertThat(response).isNotNull())
                                .verifyComplete();

                verify(rateLimiterService).executeWithRateLimit(eq("unknown"), any());

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

                // Mock API key validation (needed for operation construction, but not executed
                // due to rate limit)
                lenient().when(apiKeyValidator.isValid(apiKey)).thenReturn(Mono.just(true));
                lenient().when(apiKeyValidator.getClientType(apiKey)).thenReturn(Mono.just("web"));

                RequestNotPermitted requestNotPermitted = mock(RequestNotPermitted.class);
                when(rateLimiterService.executeWithRateLimit(eq(clientIp), any()))
                                .thenReturn(Mono.error(requestNotPermitted));

                // When & Then
                StepVerifier.create(authenticationService.authenticate(request, clientIp))
                                .expectError(RateLimitExceededException.class)
                                .verify();

                verify(rateLimiterService).executeWithRateLimit(eq(clientIp), any());
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

                when(apiKeyValidator.isValid(apiKey)).thenReturn(Mono.just(false));
                when(rateLimiterService.executeWithRateLimit(eq(clientIp), any()))
                                .thenAnswer(invocation -> invocation.getArgument(1));

                // When & Then
                StepVerifier.create(authenticationService.authenticate(request, clientIp))
                                .expectError(InvalidApiKeyException.class)
                                .verify();

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

                when(apiKeyValidator.isValid(apiKey)).thenReturn(Mono.just(true));
                when(apiKeyValidator.getClientType(apiKey)).thenReturn(Mono.just(clientType));
                when(tokenService.generateAccessToken(eq(clientId), eq(clientType), anyMap()))
                                .thenReturn(Mono.just("access-token"));
                when(tokenService.generateRefreshToken(clientId, clientType))
                                .thenReturn(Mono.just("refresh-token"));
                when(tokenService.extractTokenId(anyString())).thenReturn(Mono.just("token-id"));
                when(rateLimiterService.executeWithRateLimit(eq(clientIp), any()))
                                .thenAnswer(invocation -> invocation.getArgument(1));

                // When
                StepVerifier.create(authenticationService.authenticate(request, clientIp))
                                .assertNext(response -> assertThat(response).isNotNull())
                                .verifyComplete();

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

                when(apiKeyValidator.isValid(apiKey)).thenReturn(Mono.just(true));
                when(apiKeyValidator.getClientType(apiKey)).thenReturn(Mono.just(clientType));
                when(tokenService.generateAccessToken(eq(clientId), eq(clientType), anyMap()))
                                .thenReturn(Mono.just("access-token"));
                when(tokenService.generateRefreshToken(clientId, clientType))
                                .thenReturn(Mono.just("refresh-token"));
                when(tokenService.extractTokenId(anyString())).thenReturn(Mono.just("token-id"));
                when(rateLimiterService.executeWithRateLimit(eq(clientIp), any()))
                                .thenAnswer(invocation -> invocation.getArgument(1));

                // When
                StepVerifier.create(authenticationService.authenticate(request, clientIp))
                                .assertNext(response -> assertThat(response).isNotNull())
                                .verifyComplete();

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
                String clientIp = "192.168.1.1";

                Claims claims = mock(Claims.class);
                when(claims.getSubject()).thenReturn(clientId);
                when(claims.get("clientType", String.class)).thenReturn(clientType);

                when(tokenService.validateRefreshToken(oldRefreshToken)).thenReturn(Mono.just(claims));
                when(tokenService.revokeToken(oldRefreshToken)).thenReturn(Mono.empty());
                when(tokenService.generateAccessToken(clientId, clientType, null))
                                .thenReturn(Mono.just("new-access-token"));
                when(tokenService.generateRefreshToken(clientId, clientType))
                                .thenReturn(Mono.just("new-refresh-token"));
                when(tokenService.extractTokenId(anyString())).thenReturn(Mono.just("new-token-id"));

                // When & Then
                StepVerifier.create(authenticationService.refreshToken(oldRefreshToken, clientIp))
                                .assertNext(response -> {
                                        assertThat(response).isNotNull();
                                        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
                                        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
                                        assertThat(response.getTokenType()).isEqualTo("Bearer");
                                        assertThat(response.getExpiresIn()).isEqualTo(3600L);
                                })
                                .verifyComplete();

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
                String clientIp = "192.168.1.1";

                Claims claims = mock(Claims.class);
                when(claims.getSubject()).thenReturn(clientId);
                when(claims.get("clientType", String.class)).thenReturn(clientType);

                when(tokenService.validateRefreshToken(oldRefreshToken)).thenReturn(Mono.just(claims));
                when(tokenService.revokeToken(oldRefreshToken)).thenReturn(Mono.empty());
                when(tokenService.generateAccessToken(clientId, clientType, null))
                                .thenReturn(Mono.just("new-access-token"));
                when(tokenService.generateRefreshToken(clientId, clientType))
                                .thenReturn(Mono.just("new-refresh-token"));
                when(tokenService.extractTokenId(anyString())).thenReturn(Mono.just("new-token-id"));

                // When
                StepVerifier.create(authenticationService.refreshToken(oldRefreshToken, clientIp))
                                .assertNext(response -> assertThat(response).isNotNull())
                                .verifyComplete();

                // Then
                verify(tokenService).revokeToken(oldRefreshToken);
        }

        @Test
        @DisplayName("Should revoke token successfully")
        void shouldRevokeTokenSuccessfully() {
                // Given
                String token = "token-to-revoke";
                String tokenId = "token-id-123";
                String clientId = "client-123";

                when(tokenService.extractTokenId(token)).thenReturn(Mono.just(tokenId));
                when(tokenService.extractSubject(token)).thenReturn(Mono.just(clientId));
                when(tokenService.revokeToken(token)).thenReturn(Mono.empty());

                // When & Then
                StepVerifier.create(authenticationService.revokeToken(token))
                                .verifyComplete();

                verify(tokenService).revokeToken(token);
        }

        @Test
        @DisplayName("Should validate token and return claims")
        void shouldValidateTokenAndReturnClaims() {
                // Given
                String token = "valid-token";
                String tokenId = "token-id-123";
                String clientId = "client-123";
                String clientType = "web";
                Date issuedAt = new Date(System.currentTimeMillis());
                Date expiresAt = new Date(System.currentTimeMillis() + 3600000);

                Claims claims = mock(Claims.class);
                when(claims.getSubject()).thenReturn(clientId);
                when(claims.get("clientType")).thenReturn(clientType);
                when(claims.getIssuedAt()).thenReturn(issuedAt);
                when(claims.getExpiration()).thenReturn(expiresAt);

                when(tokenService.validateToken(token)).thenReturn(Mono.just(claims));
                when(tokenService.extractTokenId(token)).thenReturn(Mono.just(tokenId));

                // When & Then
                StepVerifier.create(authenticationService.validateToken(token))
                                .assertNext(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).containsEntry("valid", true);
                                        assertThat(result).containsEntry("subject", clientId);
                                        assertThat(result).containsEntry("clientType", clientType);
                                        assertThat(result).containsEntry("expiresAt", expiresAt);
                                        assertThat(result).containsEntry("issuedAt", issuedAt);
                                })
                                .verifyComplete();

                verify(tokenService).validateToken(token);
                verify(tokenService).extractTokenId(token);
        }

        @Test
        @DisplayName("Should validate token and return all required fields")
        void shouldValidateTokenAndReturnAllRequiredFields() {
                // Given
                String token = "valid-token";
                String tokenId = "token-id-456";
                String clientId = "client-456";
                String clientType = "platform";
                Date issuedAt = new Date(System.currentTimeMillis() - 1000000);
                Date expiresAt = new Date(System.currentTimeMillis() + 2000000);

                Claims claims = mock(Claims.class);
                when(claims.getSubject()).thenReturn(clientId);
                when(claims.get("clientType")).thenReturn(clientType);
                when(claims.getIssuedAt()).thenReturn(issuedAt);
                when(claims.getExpiration()).thenReturn(expiresAt);

                when(tokenService.validateToken(token)).thenReturn(Mono.just(claims));
                when(tokenService.extractTokenId(token)).thenReturn(Mono.just(tokenId));

                // When & Then
                StepVerifier.create(authenticationService.validateToken(token))
                                .assertNext(result -> {
                                        assertThat(result).isNotNull();
                                        assertThat(result).containsEntry("valid", true);
                                        assertThat(result).containsEntry("subject", clientId);
                                        assertThat(result).containsEntry("clientType", clientType);
                                        assertThat(result).containsEntry("expiresAt", expiresAt);
                                        assertThat(result).containsEntry("issuedAt", issuedAt);
                                })
                                .verifyComplete();
        }
}
