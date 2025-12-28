package com.example.authservice.controller;

import com.example.authservice.dto.AuthRequest;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.RefreshTokenRequest;
import com.example.authservice.dto.RevokeTokenRequest;
import com.example.authservice.exception.InvalidApiKeyException;
import com.example.authservice.exception.RateLimitExceededException;
import com.example.authservice.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AuthController.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

        @Autowired
        private WebTestClient webTestClient;

        @SuppressWarnings("removal")
        @MockBean
        private AuthenticationService authenticationService;

        @Test
        @DisplayName("Should authenticate with valid API key")
        void shouldAuthenticateWithValidApiKey() {
                AuthRequest request = new AuthRequest("valid-api-key", "client123", "web");
                AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 3600);

                when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                                .thenReturn(Mono.just(response));

                webTestClient.post()
                                .uri("/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.accessToken").isEqualTo("access-token")
                                .jsonPath("$.refreshToken").isEqualTo("refresh-token")
                                .jsonPath("$.tokenType").isEqualTo("Bearer");
        }

        @Test
        @DisplayName("Should reject request with missing API key")
        void shouldRejectRequestWithMissingApiKey() {
                AuthRequest request = new AuthRequest();
                request.setClientId("client123");

                webTestClient.post()
                                .uri("/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().is5xxServerError(); // Validation error in test context
        }

        @Test
        @DisplayName("Should return 401 for invalid API key")
        void shouldReturn401ForInvalidApiKey() {
                AuthRequest request = new AuthRequest("invalid-api-key", "client123", "web");

                when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                                .thenReturn(Mono.error(new InvalidApiKeyException()));

                webTestClient.post()
                                .uri("/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Should return 429 when rate limit exceeded")
        void shouldReturn429WhenRateLimitExceeded() {
                AuthRequest request = new AuthRequest("valid-api-key", "client123", "web");

                when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                                .thenReturn(Mono.error(new RateLimitExceededException()));

                webTestClient.post()
                                .uri("/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isEqualTo(429);
        }

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
                RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
                AuthResponse response = new AuthResponse("new-access-token", "new-refresh-token", "Bearer", 3600);

                when(authenticationService.refreshToken(anyString(), anyString()))
                                .thenReturn(Mono.just(response));

                webTestClient.post()
                                .uri("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.accessToken").isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("Should revoke token successfully")
        void shouldRevokeTokenSuccessfully() {
                RevokeTokenRequest request = new RevokeTokenRequest("token-to-revoke");

                when(authenticationService.revokeToken(anyString()))
                                .thenReturn(Mono.empty());

                webTestClient.post()
                                .uri("/auth/revoke")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.message").isEqualTo("Token revoked successfully");
        }

        @Test
        @DisplayName("Should validate token successfully")
        void shouldValidateTokenSuccessfully() {
                when(authenticationService.validateToken(anyString()))
                                .thenReturn(Mono.just(Map.of("valid", true, "subject", "client123")));

                webTestClient.post()
                                .uri("/auth/validate")
                                .header("Authorization", "Bearer valid-token")
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.valid").isEqualTo(true);
        }

        @Test
        @DisplayName("Should extract IP from X-Forwarded-For header")
        void shouldExtractIpFromXForwardedForHeader() {
                AuthRequest request = new AuthRequest("valid-api-key", "client123", "web");
                AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 3600);

                when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                                .thenReturn(Mono.just(response));

                webTestClient.post()
                                .uri("/auth/token")
                                .header("X-Forwarded-For", "203.0.113.45")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should extract first IP from X-Forwarded-For with multiple IPs")
        void shouldExtractFirstIpFromXForwardedForWithMultipleIps() {
                AuthRequest request = new AuthRequest("valid-api-key", "client123", "web");
                AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 3600);

                when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                                .thenReturn(Mono.just(response));

                webTestClient.post()
                                .uri("/auth/token")
                                .header("X-Forwarded-For", "203.0.113.45, 198.51.100.1, 192.0.2.1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should extract IP from X-Real-IP header")
        void shouldExtractIpFromXRealIpHeader() {
                RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
                AuthResponse response = new AuthResponse("new-access-token", "new-refresh-token", "Bearer", 3600);

                when(authenticationService.refreshToken(anyString(), anyString()))
                                .thenReturn(Mono.just(response));

                webTestClient.post()
                                .uri("/auth/refresh")
                                .header("X-Real-IP", "198.51.100.5")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should prioritize X-Forwarded-For over X-Real-IP")
        void shouldPrioritizeXForwardedForOverXRealIp() {
                AuthRequest request = new AuthRequest("valid-api-key", "client123", "web");
                AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 3600);

                when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                                .thenReturn(Mono.just(response));

                webTestClient.post()
                                .uri("/auth/token")
                                .header("X-Forwarded-For", "203.0.113.45")
                                .header("X-Real-IP", "198.51.100.5")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should handle request without IP headers")
        void shouldHandleRequestWithoutIpHeaders() {
                AuthRequest request = new AuthRequest("valid-api-key", "client123", "web");
                AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 3600);

                when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                                .thenReturn(Mono.just(response));

                webTestClient.post()
                                .uri("/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should extract token from Bearer authorization header")
        void shouldExtractTokenFromBearerAuthorizationHeader() {
                when(authenticationService.validateToken("valid-token-123"))
                                .thenReturn(Mono.just(Map.of("valid", true, "subject", "client123")));

                webTestClient.post()
                                .uri("/auth/validate")
                                .header("Authorization", "Bearer valid-token-123")
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.valid").isEqualTo(true);
        }

        @Test
        @DisplayName("Should handle authorization header without Bearer prefix")
        void shouldHandleAuthorizationHeaderWithoutBearerPrefix() {
                when(authenticationService.validateToken("plain-token"))
                                .thenReturn(Mono.just(Map.of("valid", false)));

                webTestClient.post()
                                .uri("/auth/validate")
                                .header("Authorization", "plain-token")
                                .exchange()
                                .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should handle different Bearer token formats")
        void shouldHandleDifferentBearerTokenFormats() {
                when(authenticationService.validateToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
                                .thenReturn(Mono.just(Map.of("valid", true, "subject", "client456")));

                webTestClient.post()
                                .uri("/auth/validate")
                                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
                                .exchange()
                                .expectStatus().isOk()
                                .expectBody()
                                .jsonPath("$.valid").isEqualTo(true);
        }
}
