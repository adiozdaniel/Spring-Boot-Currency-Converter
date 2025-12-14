package com.example.authservice.service;

import com.example.authservice.config.JwtConfig;
import com.example.authservice.exception.InvalidTokenException;
import com.example.authservice.exception.TokenRevokedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    private final JwtConfig jwtConfig;
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    public TokenService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public String generateAccessToken(String clientId, String clientType, Map<String, Object> additionalClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpiration());

        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(clientId)
                .claim("type", "access")
                .claim("clientType", clientType)
                .issuedAt(now)
                .expiration(expiry);

        if (additionalClaims != null) {
            additionalClaims.forEach(builder::claim);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    public String generateRefreshToken(String clientId, String clientType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getRefreshExpiration());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(clientId)
                .claim("type", "refresh")
                .claim("clientType", clientType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateToken(String token) {
        if (isRevoked(token)) {
            throw new TokenRevokedException("Token has been revoked");
        }

        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired: {}", e.getMessage());
            throw new InvalidTokenException("Token has expired");
        } catch (JwtException e) {
            logger.warn("Invalid token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token");
        }
    }

    public Claims validateRefreshToken(String token) {
        Claims claims = validateToken(token);

        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        return claims;
    }

    public void revokeToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            if (jti != null) {
                revokedTokens.add(jti);
                logger.info("Token revoked: {}", jti);
            }
        } catch (JwtException e) {
            logger.warn("Failed to revoke token: {}", e.getMessage());
        }
    }

    public boolean isRevoked(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            return jti != null && revokedTokens.contains(jti);
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return validateToken(token).getSubject();
    }

    public String extractClientType(String token) {
        return validateToken(token).get("clientType", String.class);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
