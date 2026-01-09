package com.example.authservice.controller;

import com.example.authservice.dto.AuthRequest;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.utility.HttpUtilities;
import com.example.authservice.dto.RevokeTokenRequest;
import com.example.authservice.dto.RefreshTokenRequest;
import com.example.authservice.service.AuthenticationService;
import com.example.authservice.constant.HttpSecurityConstants;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.server.reactive.ServerHttpRequest;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

/**
 * REST controller for handling authentication-related requests.
 * <p>
 * This controller provides endpoints for token issuance, refreshment,
 * revocation, and validation.
 * Optimized for performance and security.
 * </p>
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "JWT authentication and token management endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Authenticates a user and issues a new access and refresh token.
     *
     * @param request     the authentication request containing client credentials.
     * @param httpRequest the incoming server HTTP request.
     * @return a {@link Mono} containing a {@link ResponseEntity} with the
     *         {@link AuthResponse}.
     */
    @PostMapping("/token")
    @Operation(summary = "Authenticate and get tokens", description = "Validates API key and returns JWT access and refresh tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid API key"),
            @ApiResponse(responseCode = "403", description = "Unable to determine client IP"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public Mono<ResponseEntity<AuthResponse>> authenticate(
            @Valid @RequestBody AuthRequest request,
            ServerHttpRequest httpRequest) {

        // Extract IP synchronously (no need for Mono.fromSupplier overhead)
        String clientIp = HttpUtilities.getClientIp(httpRequest);

        if (logger.isInfoEnabled()) {
            logger.info("Authentication request from IP: {}", clientIp);
        }

        return authenticationService.authenticate(request, clientIp)
                .map(ResponseEntity::ok);
    }

    /**
     * Refreshes an existing access token using a refresh token.
     *
     * @param request     the refresh token request.
     * @param httpRequest the incoming server HTTP request.
     * @return a {@link Mono} containing a {@link ResponseEntity} with the new
     *         {@link AuthResponse}.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Uses a valid refresh token to obtain a new access token and refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully refreshed", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @ApiResponse(responseCode = "403", description = "Unable to determine client IP")
    })
    public Mono<ResponseEntity<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            ServerHttpRequest httpRequest) {

        String clientIp = HttpUtilities.getClientIp(httpRequest);

        if (logger.isInfoEnabled()) {
            logger.info("Token refresh request from IP: {}", clientIp);
        }

        return authenticationService.refreshToken(request.getRefreshToken(), clientIp)
                .map(ResponseEntity::ok);
    }

    /**
     * Revokes a refresh token.
     *
     * @param request the revoke token request.
     * @return a {@link Mono} containing a {@link ResponseEntity} with a
     *         confirmation message.
     */
    @PostMapping("/revoke")
    @Operation(summary = "Revoke token", description = "Revokes a refresh token, making it unusable for future authentication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token successfully revoked"),
            @ApiResponse(responseCode = "401", description = "Invalid token")
    })
    public Mono<ResponseEntity<Map<String, String>>> revokeToken(
            @Valid @RequestBody RevokeTokenRequest request) {

        return authenticationService.revokeToken(request.getToken())
                .doOnSuccess(v -> {
                    if (logger.isInfoEnabled()) {
                        logger.info("Token successfully revoked");
                    }
                })
                .thenReturn(ResponseEntity.ok(HttpSecurityConstants.REVOKE_SUCCESS_RESPONSE));
    }

    /**
     * Validates an access token.
     *
     * @param authHeader the Authorization header containing the Bearer token.
     * @return a {@link Mono} containing a {@link ResponseEntity} with the
     *         validation result.
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate access token", description = "Validates an access token and returns its claims", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public Mono<ResponseEntity<Map<String, Object>>> validateToken(
            @RequestHeader("Authorization") String authHeader) {

        String token = HttpUtilities.extractToken(authHeader);

        return authenticationService.validateToken(token)
                .map(ResponseEntity::ok);
    }
}
