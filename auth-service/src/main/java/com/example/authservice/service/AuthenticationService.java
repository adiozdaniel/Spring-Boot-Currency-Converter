package com.example.authservice.service;

import com.example.authservice.config.JwtConfig;
import com.example.authservice.dto.AuthRequest;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.exception.InvalidApiKeyException;
import com.example.authservice.exception.InvalidTokenException;
import com.example.authservice.exception.RateLimitExceededException;
import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final ApiKeyValidator apiKeyValidator;
    private final TokenService tokenService;
    private final RateLimiterService rateLimiterService;
    private final Optional<AuthEventProducer> authEventProducer;
    private final JwtConfig jwtConfig;
    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;

    public AuthenticationService(
            ApiKeyValidator apiKeyValidator,
            TokenService tokenService,
            RateLimiterService rateLimiterService,
            @Autowired(required = false) AuthEventProducer authEventProducer,
            JwtConfig jwtConfig,
            MeterRegistry meterRegistry) {
        this.apiKeyValidator = apiKeyValidator;
        this.tokenService = tokenService;
        this.rateLimiterService = rateLimiterService;
        this.authEventProducer = Optional.ofNullable(authEventProducer);
        this.jwtConfig = jwtConfig;

        this.authSuccessCounter = Counter.builder("auth.attempts")
                .tag("result", "success")
                .description("Number of successful authentication attempts")
                .register(meterRegistry);

        this.authFailureCounter = Counter.builder("auth.attempts")
                .tag("result", "failure")
                .description("Number of failed authentication attempts")
                .register(meterRegistry);
    }

    public AuthResponse authenticate(AuthRequest request, String clientIp) {
        String rateLimitKey = clientIp != null ? clientIp : "unknown";

        if (!rateLimiterService.tryConsume(rateLimitKey)) {
            authFailureCounter.increment();
            authEventProducer.ifPresent(producer -> producer.publishRateLimitExceeded(request.getClientId(), clientIp));
            throw new RateLimitExceededException();
        }

        if (!apiKeyValidator.isValid(request.getApiKey())) {
            authFailureCounter.increment();
            authEventProducer.ifPresent(producer -> producer.publishInvalidApiKey(clientIp));
            logger.warn("Authentication failed - invalid API key from IP: {}", clientIp);
            throw new InvalidApiKeyException();
        }

        String clientType = apiKeyValidator.getClientType(request.getApiKey());
        String clientId = request.getClientId() != null ? request.getClientId() : UUID.randomUUID().toString();

        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put("ip", clientIp);

        String accessToken = tokenService.generateAccessToken(clientId, clientType, additionalClaims);
        String refreshToken = tokenService.generateRefreshToken(clientId, clientType);

        // Publish success events
        authSuccessCounter.increment();
        authEventProducer.ifPresent(producer -> {
            producer.publishLoginSuccess(clientId, clientType, clientIp);
            producer.publishTokenGenerated(tokenService.extractTokenId(accessToken), clientId, clientType, clientIp);
        });

        logger.info("Authentication successful for client: {} (type: {})", clientId, clientType);

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtConfig.getExpiration() / 1000
        );
    }

    public AuthResponse refreshToken(String refreshToken, String clientIp) {
        Claims claims = tokenService.validateRefreshToken(refreshToken);

        String clientId = claims.getSubject();
        String clientType = claims.get("clientType", String.class);

        tokenService.revokeToken(refreshToken);

        String newAccessToken = tokenService.generateAccessToken(clientId, clientType, null);
        String newRefreshToken = tokenService.generateRefreshToken(clientId, clientType);

        // Publish token events
        authEventProducer.ifPresent(producer ->
            producer.publishTokenRefreshed(tokenService.extractTokenId(newAccessToken), clientId, clientType, clientIp));

        logger.info("Token refreshed for client: {}", clientId);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtConfig.getExpiration() / 1000
        );
    }

    public void revokeToken(String token) {
        String tokenId = tokenService.extractTokenId(token);
        String clientId = tokenService.extractSubject(token);

        tokenService.revokeToken(token);
        authEventProducer.ifPresent(producer -> producer.publishTokenRevoked(tokenId, clientId));

        logger.info("Token revoked successfully");
    }

    public Map<String, Object> validateToken(String token) {
        Claims claims = tokenService.validateToken(token);

        String tokenId = tokenService.extractTokenId(token);
        String clientId = claims.getSubject();
        authEventProducer.ifPresent(producer -> producer.publishTokenValidated(tokenId, clientId));

        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("subject", clientId);
        result.put("clientType", claims.get("clientType"));
        result.put("expiresAt", claims.getExpiration());
        result.put("issuedAt", claims.getIssuedAt());

        return result;
    }
}
