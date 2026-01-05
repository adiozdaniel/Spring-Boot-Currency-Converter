package com.example.authservice.controller;

import com.example.authservice.dto.AuthRequest;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.RevokeTokenRequest;
import com.example.authservice.dto.RefreshTokenRequest;
import com.example.authservice.service.AuthenticationService;
import com.example.authservice.exception.UnknownIpAddressException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import java.util.Map;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

/**
 * REST controller for handling authentication-related requests.
 * <p>
 * This controller provides endpoints for token issuance, refreshment,
 * revocation,
 * and validation.
 * </p>
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "JWT authentication and token management endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;

    /**
     * Constructs a new {@link AuthController} with the specified authentication
     * service.
     *
     * @param authenticationService the service for handling authentication logic.
     */
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

        return Mono.fromSupplier(() -> getClientIp(httpRequest))
                .doOnNext(ip -> logger.info("Authentication request from IP: {}", ip))
                .flatMap(clientIp -> authenticationService.authenticate(request, clientIp))
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

        return Mono.fromSupplier(() -> getClientIp(httpRequest))
                .doOnNext(ip -> logger.info("Token refresh request from IP: {}", ip))
                .flatMap(clientIp -> authenticationService.refreshToken(request.getRefreshToken(), clientIp))
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
                .doOnSuccess(v -> logger.info("Token successfully revoked"))
                .thenReturn(ResponseEntity.ok(Map.of("message", "Token revoked successfully")));
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

        String token = extractToken(authHeader);

        return authenticationService.validateToken(token)
                .map(ResponseEntity::ok);
    }

    /**
     * Extracts the client's IP address from the request headers.
     * <p>
     * It checks for "X-Forwarded-For" and "X-Real-IP" headers before falling
     * back to the remote address of the request.
     * </p>
     *
     * @param request the server HTTP request.
     * @return the client's IP address.
     * @throws UnknownIpAddressException if IP address cannot be determined.
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        throw new UnknownIpAddressException();
    }

    /**
     * Extracts the JWT from the Authorization header.
     *
     * @param authHeader the Authorization header string.
     * @return the token string or the original header if not a Bearer token.
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
