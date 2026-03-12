package com.currencyconverter.authservice.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import com.currencyconverter.authservice.config.JwtConfig;
import com.currencyconverter.authservice.constant.AuthConstants;
import com.currencyconverter.authservice.exception.InvalidTokenException;
import com.currencyconverter.authservice.exception.TokenRevokedException;
import com.currencyconverter.authservice.service.TokenService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of the {@link TokenService} interface for JWT token management.
 * <p>
 * This service handles the generation, validation, revocation, and parsing of
 * both access and refresh JSON Web Tokens (JWTs). It integrates with Redis
 * to manage revoked tokens.
 * </p>
 */
@Service
public class TokenServiceImpl implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);
    private static final String REVOKED_TOKEN_PREFIX = "revoked:token:";

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Constructs a new {@link TokenServiceImpl}.
     *
     * @param jwtConfig     the JWT configuration properties.
     * @param redisTemplate the reactive Redis template for data access.
     */
    public TokenServiceImpl(
            JwtConfig jwtConfig,
            ReactiveRedisTemplate<String, String> redisTemplate) {
        this.jwtConfig = jwtConfig;
        this.redisTemplate = redisTemplate;
        this.signingKey = Keys.hmacShaKeyFor(
                jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> generateAccessToken(
            String clientId, String clientType,
            Map<String, Object> additionalClaims) {

        return Mono.defer(() -> {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + jwtConfig.getExpiration());

            var builder = Jwts.builder()
                    .id(UUID.randomUUID().toString()) // JTI - Unique token ID
                    .subject(clientId)
                    .claim("type", "access")
                    .claim(AuthConstants.CLIENT_TYPE, clientType)
                    .issuedAt(now)
                    .expiration(expiry);

            if (additionalClaims != null) {
                additionalClaims.forEach(builder::claim);
            }

            String token = builder.signWith(signingKey).compact();
            return Mono.just(token);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> generateRefreshToken(String clientId, String clientType) {
        return Mono.defer(() -> {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + jwtConfig.getRefreshExpiration());

            String token = Jwts.builder()
                    .id(UUID.randomUUID().toString()) // JTI - Unique token ID
                    .subject(clientId)
                    .claim("type", "refresh")
                    .claim(AuthConstants.CLIENT_TYPE, clientType)
                    .issuedAt(now)
                    .expiration(expiry)
                    .signWith(signingKey)
                    .compact();

            return Mono.just(token);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Claims> validateToken(String token) {
        return parseClaims(token)
                .flatMap(claims -> {
                    String jti = claims.getId();
                    if (jti == null) {
                        return Mono.just(claims); // No JTI, cannot check for revocation
                    }

                    // Check if token is revoked in Redis
                    return checkRevokedInRedis(jti)
                            .flatMap(revoked -> {
                                if (revoked.booleanValue()) {
                                    return Mono.error(new TokenRevokedException("Token has been revoked"));
                                }
                                return Mono.just(claims);
                            });
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Claims> validateRefreshToken(String token) {
        return validateToken(token) // First validate general token properties
                .flatMap(claims -> {
                    String type = claims.get("type", String.class);
                    if (!"refresh".equals(type)) {
                        return Mono.error(new InvalidTokenException("Token is not a refresh token"));
                    }
                    return Mono.just(claims);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Void> revokeToken(String token) {
        return parseClaims(token)
                .flatMap(claims -> {
                    String jti = claims.getId();
                    if (jti == null) {
                        logger.warn("Token has no JTI, cannot revoke");
                        return Mono.empty();
                    }

                    // Calculate TTL: time until token expires
                    Date expiration = claims.getExpiration();
                    long ttlSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;

                    if (ttlSeconds <= 0) {
                        logger.debug("Token already expired, no need to revoke");
                        return Mono.empty();
                    }

                    // Store in Redis with TTL matching token expiration
                    String redisKey = REVOKED_TOKEN_PREFIX + jti;
                    return redisTemplate.opsForValue()
                            .set(redisKey, "true", Duration.ofSeconds(ttlSeconds))
                            .doOnSuccess(success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    logger.info("Token revoked: {}", jti);
                                }
                            })
                            .then(); // Convert Boolean Mono to Void Mono
                })
                .onErrorResume(ex -> {
                    logger.error("Failed to revoke token: {}", ex.getMessage());
                    return Mono.empty(); // Continue without error if revocation fails
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<Boolean> isRevoked(String token) {
        return parseClaims(token)
                .flatMap(claims -> {
                    String jti = claims.getId();
                    if (jti == null) {
                        return Mono.just(false); // Token without JTI cannot be explicitly revoked
                    }
                    return checkRevokedInRedis(jti);
                })
                .onErrorReturn(false); // If parsing fails, consider it not revoked
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> extractSubject(String token) {
        return validateToken(token).map(Claims::getSubject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> extractClientType(String token) {
        return validateToken(token).map(claims -> claims.get("clientType", String.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mono<String> extractTokenId(String token) {
        return validateToken(token).map(Claims::getId);
    }

    /**
     * Parses a JWT token and returns its {@link Claims}.
     * <p>
     * This method handles JWT parsing and signature verification. It translates
     * specific JWT exceptions (e.g., expired, invalid signature) into custom
     * application exceptions.
     * </p>
     * @param token the JWT string to parse.
     * @return a {@link Mono} emitting the {@link Claims} if parsing is successful.
     * @throws InvalidTokenException if the token is expired or otherwise invalid.
     */
    private Mono<Claims> parseClaims(String token) {
        return Mono.fromCallable(() -> {
            try {
                return Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (ExpiredJwtException ex) {
                logger.warn("Token expired: {}", ex.getMessage());
                throw new InvalidTokenException("Token has expired");
            } catch (JwtException ex) {
                logger.warn("Invalid token: {}", ex.getMessage());
                throw new InvalidTokenException("Invalid token");
            }
        });
    }

    /**
     * Checks if a token, identified by its JTI (JWT ID), is present in the Redis
     * store of revoked tokens.
     *
     * @param jti the JWT ID of the token to check.
     * @return a {@link Mono} emitting {@code true} if the token is revoked, {@code false} otherwise.
     */
    private Mono<Boolean> checkRevokedInRedis(String jti) {
        String redisKey = REVOKED_TOKEN_PREFIX + jti;
        return redisTemplate.hasKey(redisKey)
                .defaultIfEmpty(false); // Default to false if key not found (e.g., Redis connection issues)
    }
}