package com.example.authservice.service;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Service interface for managing JSON Web Tokens (JWTs), including generation,
 * validation, revocation, and extraction of claims.
 * <p>
 * This interface defines the core operations for handling both access and
 * refresh tokens within the authentication service.
 * </p>
 */
public interface TokenService {

    /**
     * Generates a new access token for a given client.
     *
     * @param clientId         the unique identifier of the client.
     * @param clientType       the type of the client (e.g., "web", "mobile").
     * @param additionalClaims any additional claims to include in the token.
     * @return a {@link Mono} emitting the generated access token string.
     */
    Mono<String> generateAccessToken(String clientId, String clientType, Map<String, Object> additionalClaims);

    /**
     * Generates a new refresh token for a given client.
     *
     * @param clientId   the unique identifier of the client.
     * @param clientType the type of the client.
     * @return a {@link Mono} emitting the generated refresh token string.
     */
    Mono<String> generateRefreshToken(String clientId, String clientType);

    /**
     * Validates an access token and extracts its claims.
     *
     * @param token the access token string to validate.
     * @return a {@link Mono} emitting the {@link Claims} contained within the token if valid.
     *         Emits an error if the token is invalid or expired.
     */
    Mono<Claims> validateToken(String token);

    /**
     * Validates a refresh token and extracts its claims.
     *
     * @param token the refresh token string to validate.
     * @return a {@link Mono} emitting the {@link Claims} contained within the refresh token if valid.
     *         Emits an error if the token is invalid or expired.
     */
    Mono<Claims> validateRefreshToken(String token);

    /**
     * Revokes a given token, typically a refresh token, making it unusable for further authentication.
     *
     * @param token the token string to be revoked.
     * @return a {@link Mono<Void>} that completes when the token has been successfully revoked.
     */
    Mono<Void> revokeToken(String token);

    /**
     * Checks if a given token has been revoked.
     *
     * @param token the token string to check.
     * @return a {@link Mono} emitting {@code true} if the token is revoked, {@code false} otherwise.
     */
    Mono<Boolean> isRevoked(String token);

    /**
     * Extracts the subject (client ID) from a token.
     *
     * @param token the token string.
     * @return a {@link Mono} emitting the subject string.
     */
    Mono<String> extractSubject(String token);

    /**
     * Extracts the client type from a token.
     *
     * @param token the token string.
     * @return a {@link Mono} emitting the client type string.
     */
    Mono<String> extractClientType(String token);

    /**
     * Extracts the unique identifier (JTI) of the token.
     *
     * @param token the token string.
     * @return a {@link Mono} emitting the token ID string.
     */
    Mono<String> extractTokenId(String token);
}
