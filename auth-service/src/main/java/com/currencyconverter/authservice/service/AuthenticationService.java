package com.currencyconverter.authservice.service;

import reactor.core.publisher.Mono;
import java.util.Map;

import com.currencyconverter.authservice.dto.AuthRequest;
import com.currencyconverter.authservice.dto.AuthResponse;

/**
 * Service interface for handling authentication-related operations.
 * <p>
 * This interface defines the core business logic for user authentication,
 * token management (refresh, revoke), and token validation.
 * </p>
 */
public interface AuthenticationService {

    /**
     * Authenticates a client based on the provided request and client IP.
     *
     * @param request  the {@link AuthRequest} containing authentication credentials.
     * @param clientIp the IP address of the client making the request.
     * @return a {@link Mono} emitting an {@link AuthResponse} containing access and refresh tokens.
     */
    Mono<AuthResponse> authenticate(AuthRequest request, String clientIp);

    /**
     * Refreshes an expired or about-to-expire access token using a valid refresh token.
     *
     * @param refreshToken the refresh token provided by the client.
     * @param clientIp     the IP address of the client making the request.
     * @return a {@link Mono} emitting a new {@link AuthResponse} with updated tokens.
     */
    Mono<AuthResponse> refreshToken(String refreshToken, String clientIp);

    /**
     * Revokes a given token, typically a refresh token, making it unusable for further authentication.
     *
     * @param token the token string to be revoked.
     * @return a {@link Mono<Void>} that completes when the token has been successfully revoked.
     */
    Mono<Void> revokeToken(String token);

    /**
     * Validates an access token and returns information about its validity and claims.
     *
     * @param token the access token string to validate.
     * @return a {@link Mono} emitting a {@link Map} containing token validation details and claims.
     */
    Mono<Map<String, Object>> validateToken(String token);
}
