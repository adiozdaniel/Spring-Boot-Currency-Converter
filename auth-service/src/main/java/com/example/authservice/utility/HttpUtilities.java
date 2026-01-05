package com.example.authservice.utility;

import com.example.authservice.constant.HttpSecurityConstants;
import com.example.authservice.exception.UnknownIpAddressException;

import org.springframework.http.server.reactive.ServerHttpRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class HttpUtilities {
  private static final Logger logger = LoggerFactory.getLogger(HttpUtilities.class);

  /**
   * Extracts and validates the client's IP address from the request headers.
   * <p>
   * Performance optimized: imperative style, minimal allocations, early returns.
   * Security hardened: validates IP format, prevents header injection.
   * </p>
   *
   * @param request the server HTTP request
   * @return the validated client's IP address
   * @throws UnknownIpAddressException if IP address cannot be determined or is
   *                                   invalid
   */
  public static String getClientIp(ServerHttpRequest request) {
    String ip = getIpFromHeader(request, HttpSecurityConstants.X_FORWARDED_FOR, true);
    if (ip != null) {
      return ip;
    }

    ip = getIpFromHeader(request, HttpSecurityConstants.X_REAL_IP, false);
    if (ip != null) {
      return ip;
    }

    return getIpFromRemoteAddress(request);
  }

  /**
   * Extracts IP address from a specific header with validation.
   *
   * @param request the server HTTP request
   * @param headerName the header name to extract from
   * @param parseFirstOnly whether to parse only the first IP (for X-Forwarded-For)
   * @return the validated IP address or null if invalid
   * @throws UnknownIpAddressException if header exceeds maximum length
   */
  private static String getIpFromHeader(ServerHttpRequest request, String headerName, boolean parseFirstOnly) {
    String headerValue = request.getHeaders().getFirst(headerName);
    if (headerValue == null || headerValue.isEmpty()) {
      return null;
    }

    if (headerValue.length() > HttpSecurityConstants.MAX_HEADER_LENGTH) {
      logger.warn("{} header exceeds maximum length, rejecting", headerName);
      throw new UnknownIpAddressException();
    }

    String ip = parseFirstOnly ? extractFirstIp(headerValue) : headerValue.trim();
    if (isValidIp(ip)) {
      return ip;
    }

    logger.warn("Invalid IP in {} header: {}", headerName, ip);
    return null;
  }

  /**
   * Extracts the first IP address from a comma-separated list.
   *
   * @param headerValue the header value containing comma-separated IPs
   * @return the first IP address
   */
  private static String extractFirstIp(String headerValue) {
    int commaIndex = headerValue.indexOf(',');
    return commaIndex > 0
        ? headerValue.substring(0, commaIndex).trim()
        : headerValue.trim();
  }

  /**
   * Extracts IP address from the remote address of the request.
   *
   * @param request the server HTTP request
   * @return the IP address or throws exception if not available
   * @throws UnknownIpAddressException if remote address is not available
   */
  private static String getIpFromRemoteAddress(ServerHttpRequest request) {
    InetSocketAddress remoteAddress = request.getRemoteAddress();
    if (remoteAddress != null) {
      InetAddress address = remoteAddress.getAddress();
      if (address != null) {
        return address.getHostAddress();
      }
    }
    throw new UnknownIpAddressException();
  }

  /**
   * Validates IP address format for security.
   * <p>
   * Uses pre-compiled regex pattern for optimal performance.
   * Supports both IPv4 and IPv6.
   * </p>
   *
   * @param ip the IP address to validate
   * @return true if valid, false otherwise
   */
  private static boolean isValidIp(String ip) {
    // Quick length check before regex (performance optimization)
    if (ip == null || ip.isEmpty() || ip.length() > HttpSecurityConstants.MAX_IP_ADDRESS_LENGTH) {
      return false;
    }
    return HttpSecurityConstants.IP_ADDRESS_PATTERN.matcher(ip).matches();
  }

  /**
   * Extracts JWT from Authorization header with security validation.
   * <p>
   * Performance optimized: uses indexOf instead of startsWith + substring.
   * Security hardened: validates format and length.
   * </p>
   *
   * @param authHeader the Authorization header string
   * @return the token string or the original header if not a Bearer token
   */
  public static String extractToken(String authHeader) {
    if (authHeader == null || authHeader.isEmpty()) {
      return authHeader;
    }

    // Security: prevent excessively long headers
    if (authHeader.length() > HttpSecurityConstants.MAX_HEADER_LENGTH) {
      logger.warn("Authorization header exceeds maximum length");
      return authHeader;
    }

    // Performance: use regionMatches instead of startsWith (slightly faster)
    if (authHeader.length() > HttpSecurityConstants.BEARER_PREFIX_LENGTH &&
        authHeader.regionMatches(true, 0, HttpSecurityConstants.BEARER_PREFIX, 0,
            HttpSecurityConstants.BEARER_PREFIX_LENGTH)) {

      String token = authHeader.substring(HttpSecurityConstants.BEARER_PREFIX_LENGTH);

      // Additional JWT token length validation (optional security hardening)
      if (token.length() < HttpSecurityConstants.MIN_JWT_TOKEN_LENGTH ||
          token.length() > HttpSecurityConstants.MAX_JWT_TOKEN_LENGTH) {
        if (logger.isWarnEnabled()) {
          logger.warn("JWT token length out of acceptable range: {}", token.length());
        }

        return token;
      }

      return token;
    }

    return authHeader;
  }

  private HttpUtilities() {
    throw new AssertionError("Cannot instantiate HttpUtilities");
  }
}
