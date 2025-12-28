package com.example.authservice.service.impl;

import com.example.authservice.config.JwtConfig;
import com.example.authservice.dto.AuthRequest;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.exception.InvalidApiKeyException;
import com.example.authservice.exception.RateLimitExceededException;
import com.example.authservice.service.ApiKeyValidator;
import com.example.authservice.service.AuthEventProducer;
import com.example.authservice.service.AuthenticationService;
import com.example.authservice.service.RateLimiterService;
import com.example.authservice.service.TokenService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.jsonwebtoken.Claims;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the {@link AuthenticationService} interface.
 * <p>
 * This service handles core authentication logic, including API key validation,
 * token generation, refresh, revocation, and validation. It also integrates
 * with rate limiting and event production for auditing.
 * </p>
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {
  private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

  private final ApiKeyValidator apiKeyValidator;
  private final TokenService tokenService;
  private final RateLimiterService rateLimiterService;
  private final Optional<AuthEventProducer> authEventProducer;
  private final JwtConfig jwtConfig;
  private final Counter authSuccessCounter;
  private final Counter authFailureCounter;

  /**
   * Constructs an {@link AuthenticationServiceImpl} with the necessary dependencies.
   *
   * @param apiKeyValidator      service for validating API keys.
   * @param tokenService         service for managing JWT tokens.
   * @param rateLimiterService   service for applying rate limits.
   * @param authEventProducer    optional service for producing authentication events.
   * @param jwtConfig            JWT configuration properties.
   * @param meterRegistry        the registry for collecting and managing metrics.
   */
  public AuthenticationServiceImpl(
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<AuthResponse> authenticate(AuthRequest request, String clientIp) {
    String rateLimitKey = clientIp != null ? clientIp : "unknown";

    Mono<AuthResponse> authOperation = apiKeyValidator.isValid(request.getApiKey())
        .flatMap(valid -> {
          if (!valid) {
            authFailureCounter.increment();
            authEventProducer.ifPresent(producer -> producer.publishInvalidApiKey(clientIp));
            logger.warn("Authentication failed - invalid API key from IP: {}", clientIp);
            return Mono.error(new InvalidApiKeyException());
          }

          // Now get client type reactively
          return apiKeyValidator.getClientType(request.getApiKey())
              .flatMap(clientType -> {
                String clientId = request.getClientId() != null
                    ? request.getClientId()
                    : UUID.randomUUID().toString();

                Map<String, Object> additionalClaims = new HashMap<>();
                additionalClaims.put("ip", clientIp);

                return tokenService.generateAccessToken(clientId, clientType, additionalClaims)
                    .zipWith(tokenService.generateRefreshToken(clientId, clientType))
                    .flatMap(tuple -> {
                      String accessToken = tuple.getT1();
                      String refreshToken = tuple.getT2();

                      authSuccessCounter.increment();

                      return tokenService.extractTokenId(accessToken)
                          .doOnNext(tokenId -> {
                            authEventProducer.ifPresent(producer -> {
                              producer.publishLoginSuccess(clientId, clientType, clientIp);
                              producer.publishTokenGenerated(tokenId, clientId, clientType,
                                  clientIp);
                            });
                            logger.info("Authentication successful for client: {} (type: {})",
                                clientId, clientType);
                          })
                          .thenReturn(new AuthResponse(
                              accessToken,
                              refreshToken,
                              "Bearer",
                              jwtConfig.getExpiration() / 1000));
                    });
              });
        });

    return rateLimiterService.executeWithRateLimit(rateLimitKey, authOperation)
        .onErrorResume(RequestNotPermitted.class, ex -> {
          authFailureCounter.increment();
          authEventProducer.ifPresent(producer -> producer.publishRateLimitExceeded(request.getClientId(), clientIp));
          return Mono.error(new RateLimitExceededException());
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<AuthResponse> refreshToken(String refreshToken, String clientIp) {
    return tokenService.validateRefreshToken(refreshToken)
        .flatMap(claims -> {
          String clientId = claims.getSubject();
          String clientType = claims.get("clientType", String.class);

          // Revoke old refresh token
          return tokenService.revokeToken(refreshToken)
              .then(Mono.defer(() -> {
                // Generate new tokens
                return tokenService.generateAccessToken(clientId, clientType, null)
                    .zipWith(tokenService.generateRefreshToken(clientId, clientType))
                    .flatMap(tuple -> {
                      String newAccessToken = tuple.getT1();
                      String newRefreshToken = tuple.getT2();

                      // Publish token refreshed event
                      return tokenService.extractTokenId(newAccessToken)
                          .doOnNext(tokenId -> {
                            authEventProducer
                                .ifPresent(producer -> producer.publishTokenRefreshed(tokenId, clientId, clientType,
                                    clientIp));
                            logger.info("Token refreshed for client: {}", clientId);
                          })
                          .thenReturn(new AuthResponse(
                              newAccessToken,
                              newRefreshToken,
                              "Bearer",
                              jwtConfig.getExpiration() / 1000));
                    });
              }));
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Void> revokeToken(String token) {
    return tokenService.extractTokenId(token)
        .zipWith(tokenService.extractSubject(token))
        .flatMap(tuple -> {
          String tokenId = tuple.getT1();
          String clientId = tuple.getT2();

          return tokenService.revokeToken(token)
              .doOnSuccess(v -> {
                authEventProducer.ifPresent(producer -> producer.publishTokenRevoked(tokenId, clientId));
                logger.info("Token revoked successfully");
              });
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, Object>> validateToken(String token) {
    return tokenService.validateToken(token)
        .zipWith(tokenService.extractTokenId(token))
        .flatMap(tuple -> {
          Claims claims = tuple.getT1();
          String tokenId = tuple.getT2();
          String clientId = claims.getSubject();

          authEventProducer.ifPresent(producer -> producer.publishTokenValidated(tokenId, clientId));

          Map<String, Object> result = new HashMap<>();
          result.put("valid", true);
          result.put("subject", clientId);
          result.put("clientType", claims.get("clientType"));
          result.put("expiresAt", claims.getExpiration());
          result.put("issuedAt", claims.getIssuedAt());

          return Mono.just(result);
        });
  }
}