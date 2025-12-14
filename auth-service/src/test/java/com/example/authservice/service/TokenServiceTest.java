package com.example.authservice.service;

import com.example.authservice.config.JwtConfig;
import com.example.authservice.exception.InvalidTokenException;
import com.example.authservice.exception.TokenRevokedException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenService Tests")
class TokenServiceTest {

    private TokenService tokenService;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("test-jwt-secret-key-minimum-32-characters-long");
        jwtConfig.setExpiration(3600000); // 1 hour
        jwtConfig.setRefreshExpiration(7200000); // 2 hours

        tokenService = new TokenService(jwtConfig);
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        String token = tokenService.generateAccessToken("client123", "web", null);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = tokenService.validateToken(token);
        assertEquals("client123", claims.getSubject());
        assertEquals("web", claims.get("clientType"));
        assertEquals("access", claims.get("type"));
    }

    @Test
    @DisplayName("Should generate access token with additional claims")
    void shouldGenerateAccessTokenWithAdditionalClaims() {
        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put("ip", "192.168.1.1");
        additionalClaims.put("role", "user");

        String token = tokenService.generateAccessToken("client123", "mobile", additionalClaims);
        Claims claims = tokenService.validateToken(token);

        assertEquals("192.168.1.1", claims.get("ip"));
        assertEquals("user", claims.get("role"));
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        String token = tokenService.generateRefreshToken("client123", "web");

        assertNotNull(token);
        Claims claims = tokenService.validateRefreshToken(token);
        assertEquals("client123", claims.getSubject());
        assertEquals("refresh", claims.get("type"));
    }

    @Test
    @DisplayName("Should reject access token as refresh token")
    void shouldRejectAccessTokenAsRefreshToken() {
        String accessToken = tokenService.generateAccessToken("client123", "web", null);

        assertThrows(InvalidTokenException.class, () ->
                tokenService.validateRefreshToken(accessToken));
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        assertThrows(InvalidTokenException.class, () ->
                tokenService.validateToken("invalid-token"));
    }

    @Test
    @DisplayName("Should revoke token")
    void shouldRevokeToken() {
        String token = tokenService.generateAccessToken("client123", "web", null);

        assertFalse(tokenService.isRevoked(token));

        tokenService.revokeToken(token);

        assertTrue(tokenService.isRevoked(token));
        assertThrows(TokenRevokedException.class, () ->
                tokenService.validateToken(token));
    }

    @Test
    @DisplayName("Should extract subject from token")
    void shouldExtractSubjectFromToken() {
        String token = tokenService.generateAccessToken("client123", "web", null);
        assertEquals("client123", tokenService.extractSubject(token));
    }

    @Test
    @DisplayName("Should extract client type from token")
    void shouldExtractClientTypeFromToken() {
        String token = tokenService.generateAccessToken("client123", "mobile", null);
        assertEquals("mobile", tokenService.extractClientType(token));
    }
}
