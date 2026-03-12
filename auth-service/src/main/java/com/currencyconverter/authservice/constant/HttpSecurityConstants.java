package com.currencyconverter.authservice.constant;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Security and HTTP-related constants for authentication controller.
 * <p>
 * All patterns are pre-compiled for optimal performance and thread-safe.
 * All maps are immutable for safe concurrent access.
 * </p>
 */
public final class HttpSecurityConstants {

       /**
        * IPv4 octet pattern for standalone IPv4 validation (0-255, no leading zeros).
        * More explicit pattern that prevents leading zeros.
        */
       private static final String IPV4_OCTET_STRICT = "(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)";

       /**
        * IPv4 octet separator pattern for repeated groups.
        */
       private static final String IPV4_OCTET_SEP = "\\.){3}";

       /**
        * Comprehensive IPv4 pattern supporting:
        * - Standard format: 192.168.1.1
        * - Range validation: 0-255 for each octet
        * - No leading zeros
        */
       private static final String IPV4_PATTERN = "^(?:" + IPV4_OCTET_STRICT + IPV4_OCTET_SEP + IPV4_OCTET_STRICT + "$";

       /**
        * IPv4 octet pattern for use in IPv4-mapped IPv6 addresses.
        * Matches 0-255 range using \\d instead of [0-9] for conciseness.
        */
       private static final String IPV4_OCTET = "(?:25[0-5]|(?:2[0-4]|1?\\d)?\\d)";

       /**
        * IPv6 hexadecimal group pattern (1-4 hex digits).
        */
       private static final String IPV6_HEX = "[0-9a-fA-F]{1,4}";

       /** 
        * Grouping for IPv6 patterns.
        */
       private static final String IPV6_GROUP =  "(?:";

       /**
        * Standard and compressed IPv6 patterns (no IPv4 mapping).
        * Reduced complexity by extracting repeated patterns.
        */
       private static final String IPV6_STANDARD =
                     IPV6_GROUP + IPV6_HEX + ":){7}" + IPV6_HEX + "|" +
                     IPV6_GROUP + IPV6_HEX + ":){1,7}:|" +
                     IPV6_GROUP + IPV6_HEX + ":){1,6}:" + IPV6_HEX + "|" +
                     IPV6_GROUP + IPV6_HEX + ":){1,5}(?::" + IPV6_HEX + "){1,2}|" +
                     IPV6_GROUP + IPV6_HEX + ":){1,4}(?::" + IPV6_HEX + "){1,3}|" +
                     IPV6_GROUP + IPV6_HEX + ":){1,3}(?::" + IPV6_HEX + "){1,4}|" +
                     IPV6_GROUP + IPV6_HEX + ":){1,2}(?::" + IPV6_HEX + "){1,5}|" +
                     IPV6_HEX + ":(?::" + IPV6_HEX + "){1,6}|" +
                     ":(?:(?::" + IPV6_HEX + "){1,7}|:)";

       /**
        * IPv4-mapped IPv6 patterns (e.g., ::ffff:192.168.1.1).
        * Extracted to separate constant to reduce overall complexity.
        */
       private static final String IPV6_MAPPED =
                     "::(?:ffff(?::0{1,4})?:)?(?:" + IPV4_OCTET + IPV4_OCTET_SEP + IPV4_OCTET + "|" +
                     IPV6_GROUP + IPV6_HEX + ":){1,4}:(?:" + IPV4_OCTET + IPV4_OCTET_SEP + IPV4_OCTET;

       /**
        * Comprehensive IPv6 pattern supporting:
        * - Full format: 2001:0db8:0000:0000:0000:0000:0000:0001
        * - Compressed format: 2001:db8::1
        * - Localhost: ::1
        * - Link-local: fe80::
        * - IPv4-mapped: ::ffff:192.168.1.1
        */
       private static final String IPV6_PATTERN = "^(?:" + IPV6_STANDARD + "|" + IPV6_MAPPED + ")$";

       /**
        * Combined IPv4 and IPv6 validation pattern.
        * Pre-compiled for thread-safe reuse and maximum performance.
        */
       public static final Pattern IP_ADDRESS_PATTERN = Pattern.compile(
                     IPV4_PATTERN + "|" + IPV6_PATTERN);

       /**
        * Maximum length for IPv6 address in string format.
        * Full IPv6 with IPv4 suffix: xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:255.255.255.255
        */
       public static final int MAX_IP_ADDRESS_LENGTH = 45;

       /**
        * HTTP header for forwarded client IP (standard proxy header).
        * Contains comma-separated list of IPs if multiple proxies.
        */
       public static final String X_FORWARDED_FOR = "X-Forwarded-For";

       /**
        * HTTP header for real client IP (nginx-specific).
        * Contains single IP address.
        */
       public static final String X_REAL_IP = "X-Real-IP";

       /**
        * Authorization header name.
        */
       public static final String AUTHORIZATION_HEADER = "Authorization";

       /**
        * Bearer token prefix in Authorization header.
        */
       public static final String BEARER_PREFIX = "Bearer ";

       /**
        * Length of Bearer prefix for efficient substring operations.
        */
       public static final int BEARER_PREFIX_LENGTH = 7;

       /**
        * Maximum allowed length for HTTP headers to prevent header injection attacks.
        * Typical JWT tokens are 150-300 bytes; 256 provides safe margin.
        */
       public static final int MAX_HEADER_LENGTH = 256;

       /**
        * Maximum allowed length for JWT tokens specifically.
        * JWTs with extensive claims can reach 1KB; 2KB provides safe margin.
        */
       public static final int MAX_JWT_TOKEN_LENGTH = 2048;

       /**
        * Minimum reasonable length for a JWT token (header.payload.signature).
        * Shorter tokens are likely malformed or malicious.
        */
       public static final int MIN_JWT_TOKEN_LENGTH = 20;

       /**
        * Immutable success response for token revocation.
        * Reused across all revocation requests to reduce allocations.
        */
       public static final Map<String, String> REVOKE_SUCCESS_RESPONSE = Map.of("message",
                     "Token revoked successfully");

       /**
        * Immutable error message for unknown IP address.
        */
       public static final String UNKNOWN_IP_ERROR = "Unable to determine client IP address";

       /**
        * Immutable error message for invalid IP format.
        */
       public static final String INVALID_IP_ERROR = "Invalid IP address format";

       /**
        * Log message template for header length violations.
        */
       public static final String LOG_HEADER_LENGTH_EXCEEDED = "{} header exceeds maximum length ({}), rejecting";

       /**
        * Log message template for invalid IP format.
        */

       public static final String LOG_INVALID_IP_FORMAT = "Invalid IP in {} header: {}";

       /**
        * Log message template for authentication requests.
        */
       public static final String LOG_AUTH_REQUEST = "Authentication request from IP: {}";

       /**
        * Log message template for token refresh requests.
        */
       public static final String LOG_REFRESH_REQUEST = "Token refresh request from IP: {}";

       /**
        * Log message template for token revocation.
        */
       public static final String LOG_TOKEN_REVOKED = "Token successfully revoked";

       /**
        * Private constructor to prevent instantiation.
        * This is a utility class with only static members.
        */
       private HttpSecurityConstants() {
              throw new AssertionError("Cannot instantiate HttpSecurityConstants");
       }
}
