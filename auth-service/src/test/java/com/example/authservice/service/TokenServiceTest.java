package com.example.authservice.service;

import com.example.authservice.config.JwtConfig;
import com.example.authservice.exception.InvalidTokenException;
import com.example.authservice.exception.TokenRevokedException;
import com.example.authservice.service.impl.TokenServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService Tests")
class TokenServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    private TokenService tokenService;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-jwt-secret-key-minimum-32-characters-long");
        jwtConfig.setExpiration(3600000); // 1 hour
        jwtConfig.setRefreshExpiration(7200000); // 2 hours

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));

        tokenService = new TokenServiceImpl(jwtConfig, redisTemplate);
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        StepVerifier.create(tokenService.generateAccessToken("client123", "web", null))
                .assertNext(token -> {
                    assertNotNull(token);
                    assertFalse(token.isEmpty());

                    StepVerifier.create(tokenService.validateToken(token))
                            .assertNext(claims -> {
                                assertEquals("client123", claims.getSubject());
                                assertEquals("web", claims.get("clientType"));
                                assertEquals("access", claims.get("type"));
                            })
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should generate access token with additional claims")
    void shouldGenerateAccessTokenWithAdditionalClaims() {
        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put("ip", "192.168.1.1");
        additionalClaims.put("role", "user");

        StepVerifier.create(tokenService.generateAccessToken("client123", "mobile", additionalClaims))
                .assertNext(token ->
                        StepVerifier.create(tokenService.validateToken(token))
                                .assertNext(claims -> {
                                    assertEquals("192.168.1.1", claims.get("ip"));
                                    assertEquals("user", claims.get("role"));
                                })
                                .verifyComplete()
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        StepVerifier.create(tokenService.generateRefreshToken("client123", "web"))
                .assertNext(token -> {
                    assertNotNull(token);
                    StepVerifier.create(tokenService.validateRefreshToken(token))
                            .assertNext(claims -> {
                                assertEquals("client123", claims.getSubject());
                                assertEquals("refresh", claims.get("type"));
                            })
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should reject access token as refresh token")
    void shouldRejectAccessTokenAsRefreshToken() {
        Mono<Claims> result = tokenService.generateAccessToken("client123", "web", null)
                .flatMap(accessToken -> tokenService.validateRefreshToken(accessToken));

        StepVerifier.create(result)
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        StepVerifier.create(tokenService.validateToken("invalid-token"))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    @DisplayName("Should revoke token")
    void shouldRevokeToken() {
        String[] tokenHolder = new String[1];

        // Generate token
        StepVerifier.create(tokenService.generateAccessToken("client123", "web", null))
                .assertNext(token -> tokenHolder[0] = token)
                .verifyComplete();

        String token = tokenHolder[0];

        // Verify not revoked initially
        StepVerifier.create(tokenService.isRevoked(token))
                .expectNext(false)
                .verifyComplete();

        // Mock Redis operations for revocation
        when(valueOps.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(true));

        // Revoke token
        StepVerifier.create(tokenService.revokeToken(token))
                .verifyComplete();

        // Verify token is revoked
        StepVerifier.create(tokenService.isRevoked(token))
                .expectNext(true)
                .verifyComplete();

        // Verify validation fails for revoked token
        StepVerifier.create(tokenService.validateToken(token))
                .expectError(TokenRevokedException.class)
                .verify();
    }

    @Test
    @DisplayName("Should extract subject from token")
    void shouldExtractSubjectFromToken() {
        Mono<String> result = tokenService.generateAccessToken("client123", "web", null)
                .flatMap(token -> tokenService.extractSubject(token));

        StepVerifier.create(result)
                .expectNext("client123")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should extract client type from token")
    void shouldExtractClientTypeFromToken() {
        Mono<String> result = tokenService.generateAccessToken("client123", "mobile", null)
                .flatMap(token -> tokenService.extractClientType(token));

        StepVerifier.create(result)
                .expectNext("mobile")
                .verifyComplete();
    }
}
