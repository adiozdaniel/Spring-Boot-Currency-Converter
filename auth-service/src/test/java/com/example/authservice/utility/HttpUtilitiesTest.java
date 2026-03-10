package com.example.authservice.utility;

import com.example.authservice.constant.HttpSecurityConstants;
import com.example.authservice.exception.UnknownIpAddressException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@DisplayName("HttpUtilities Tests")
class HttpUtilitiesTest {

  @Nested
  @DisplayName("getClientIp() Tests")
  class GetClientIpTests {

    @Test
    @DisplayName("Should fallback to remote address when headers are absent")
    void shouldFallbackToRemoteAddressWhenHeadersAbsent() throws UnknownHostException {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      when(request.getHeaders()).thenReturn(headers);

      InetAddress inetAddress = InetAddress.getByName("172.16.0.1");
      InetSocketAddress socketAddress = new InetSocketAddress(inetAddress, 8080);
      when(request.getRemoteAddress()).thenReturn(socketAddress);

      String result = HttpUtilities.getClientIp(request);

      assertEquals("172.16.0.1", result);
    }

    @ParameterizedTest
    @CsvSource({
        "X-Forwarded-For, '192.168.1.100', '192.168.1.100'",
        "X-Forwarded-For, '203.0.113.1, 198.51.100.2, 192.0.2.3', '203.0.113.1'",
        "X-Forwarded-For, '  192.168.1.100  ', '192.168.1.100'",
        "X-Real-IP, '10.0.0.5', '10.0.0.5'",
        "X-Real-IP, '  10.0.0.5  ', '10.0.0.5'"
    })
    @DisplayName("Should extract IP from headers")
    void shouldExtractIpFromHeaders(String headerName, String headerValue, String expectedIp) {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(headerName, headerValue);
      when(request.getHeaders()).thenReturn(headers);

      String result = HttpUtilities.getClientIp(request);

      assertEquals(expectedIp, result);
    }

    @Test
    @DisplayName("Should throw UnknownIpAddressException when no IP found")
    void shouldThrowExceptionWhenNoIpFound() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      when(request.getHeaders()).thenReturn(headers);
      when(request.getRemoteAddress()).thenReturn(null);

      assertThrows(UnknownIpAddressException.class, () -> HttpUtilities.getClientIp(request));
    }

    @Test
    @DisplayName("Should throw UnknownIpAddressException for invalid IP in X-Forwarded-For")
    void shouldThrowExceptionForInvalidIpInXForwardedFor() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, "invalid-ip");
      when(request.getHeaders()).thenReturn(headers);
      when(request.getRemoteAddress()).thenReturn(null);

      assertThrows(UnknownIpAddressException.class, () -> HttpUtilities.getClientIp(request));
    }

    @Test
    @DisplayName("Should throw UnknownIpAddressException for invalid IP in X-Real-IP")
    void shouldThrowExceptionForInvalidIpInXRealIp() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_REAL_IP, "999.999.999.999");
      when(request.getHeaders()).thenReturn(headers);
      when(request.getRemoteAddress()).thenReturn(null);

      assertThrows(UnknownIpAddressException.class, () -> HttpUtilities.getClientIp(request));
    }

    @Test
    @DisplayName("Should throw UnknownIpAddressException when X-Forwarded-For exceeds max length")
    void shouldThrowExceptionWhenXForwardedForExceedsMaxLength() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      String longHeader = "a".repeat(HttpSecurityConstants.MAX_HEADER_LENGTH + 1);
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, longHeader);
      when(request.getHeaders()).thenReturn(headers);

      assertThrows(UnknownIpAddressException.class, () -> HttpUtilities.getClientIp(request));
    }

    @Test
    @DisplayName("Should throw UnknownIpAddressException when X-Real-IP exceeds max length")
    void shouldThrowExceptionWhenXRealIpExceedsMaxLength() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      String longHeader = "a".repeat(HttpSecurityConstants.MAX_HEADER_LENGTH + 1);
      headers.add(HttpSecurityConstants.X_REAL_IP, longHeader);
      when(request.getHeaders()).thenReturn(headers);

      assertThrows(UnknownIpAddressException.class, () -> HttpUtilities.getClientIp(request));
    }

    @Test
    @DisplayName("Should fallback to X-Real-IP when X-Forwarded-For has invalid IP")
    void shouldFallbackToXRealIpWhenXForwardedForInvalid() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, "invalid");
      headers.add(HttpSecurityConstants.X_REAL_IP, "10.0.0.1");
      when(request.getHeaders()).thenReturn(headers);

      String result = HttpUtilities.getClientIp(request);

      assertEquals("10.0.0.1", result);
    }

    @Test
    @DisplayName("Should handle empty X-Forwarded-For and fallback to X-Real-IP")
    void shouldHandleEmptyXForwardedForAndFallback() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, "");
      headers.add(HttpSecurityConstants.X_REAL_IP, "192.168.1.1");
      when(request.getHeaders()).thenReturn(headers);

      String result = HttpUtilities.getClientIp(request);

      assertEquals("192.168.1.1", result);
    }
  }

  @Nested
  @DisplayName("extractToken() Tests")
  class ExtractTokenTests {

    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @ParameterizedTest
    @CsvSource(value = {
        "'Bearer '",
        "'bearer '",
        "'BeArEr '",
        "'BEARER '"
    })
    @DisplayName("Should extract token from Bearer header (case insensitive)")
    void shouldExtractTokenFromBearerHeaderCaseInsensitive(String bearerPrefix) {
      String authHeader = bearerPrefix + VALID_TOKEN;

      String result = HttpUtilities.extractToken(authHeader);

      assertEquals(VALID_TOKEN, result);
    }

    @Test
    @DisplayName("Should return original header when exceeds max length")
    void shouldReturnOriginalHeaderWhenExceedsMaxLength() {
      String longHeader = "Bearer " + "a".repeat(HttpSecurityConstants.MAX_HEADER_LENGTH);

      String result = HttpUtilities.extractToken(longHeader);

      assertEquals(longHeader, result);
    }

    @Test
    @DisplayName("Should extract token and validate length within acceptable range")
    void shouldExtractTokenAndValidateLength() {
      String validToken = "a".repeat(50); // Within MIN and MAX
      String authHeader = "Bearer " + validToken;

      String result = HttpUtilities.extractToken(authHeader);

      assertEquals(validToken, result);
    }

    @Test
    @DisplayName("Should return token even when below minimum length")
    void shouldReturnTokenEvenWhenBelowMinimumLength() {
      String shortToken = "short"; // Below MIN_JWT_TOKEN_LENGTH (20)
      String authHeader = "Bearer " + shortToken;

      String result = HttpUtilities.extractToken(authHeader);

      assertEquals(shortToken, result);
    }

    @Test
    @DisplayName("Should return original header when token causes header to exceed MAX_HEADER_LENGTH")
    void shouldReturnOriginalHeaderWhenTokenTooLong() {
      String longToken = "a".repeat(HttpSecurityConstants.MAX_JWT_TOKEN_LENGTH + 100);
      String authHeader = "Bearer " + longToken;

      String result = HttpUtilities.extractToken(authHeader);

      assertEquals(authHeader, result);
    }

    @Test
    @DisplayName("Should return original header when only Bearer prefix (length = BEARER_PREFIX_LENGTH)")
    void shouldReturnOriginalHeaderWhenOnlyBearerPrefix() {
      String authHeader = "Bearer ";

      String result = HttpUtilities.extractToken(authHeader);

      assertEquals(authHeader, result);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "'Basic dXNlcjpwYXNz', 'Basic dXNlcjpwYXNz'",
        "null, null",
        "'', ''",
        "'Bearer', 'Bearer'"
    }, nullValues = "null")
    @DisplayName("Should return original input for non-Bearer tokens")
    void shouldReturnOriginalInputForNonBearerTokens(String authHeader, String expected) {
      String result = HttpUtilities.extractToken(authHeader);

      assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle token at exact minimum length")
    void shouldHandleTokenAtExactMinimumLength() {
      String exactMinToken = "a".repeat(HttpSecurityConstants.MIN_JWT_TOKEN_LENGTH);
      String authHeader = "Bearer " + exactMinToken;

      String result = HttpUtilities.extractToken(authHeader);

      assertEquals(exactMinToken, result);
    }

    @Test
    @DisplayName("Should return original header when token at MAX_JWT_TOKEN_LENGTH causes header to exceed MAX_HEADER_LENGTH")
    void shouldReturnOriginalHeaderWhenTokenAtMaxLength() {
      String exactMaxToken = "a".repeat(HttpSecurityConstants.MAX_JWT_TOKEN_LENGTH);
      String authHeader = "Bearer " + exactMaxToken;

      String result = HttpUtilities.extractToken(authHeader);

      assertEquals(authHeader, result);
    }

    @Test
    @DisplayName("Should handle whitespace in Bearer prefix")
    void shouldNotExtractWhenBearerHasExtraWhitespace() {
      String authHeader = "Bearer  " + VALID_TOKEN; // Double space

      String result = HttpUtilities.extractToken(authHeader);

      // Should still extract because we only check prefix length
      assertEquals(" " + VALID_TOKEN, result);
    }

    @Test
    @DisplayName("Should handle token with special characters")
    void shouldHandleTokenWithSpecialCharacters() {
      String tokenWithSpecialChars = "token.with-special_chars";
      String authHeader = "Bearer " + tokenWithSpecialChars;

      String result = HttpUtilities.extractToken(authHeader);

      assertEquals(tokenWithSpecialChars, result);
    }
  }

  @Nested
  @DisplayName("IPv4 Validation Tests")
  class Ipv4ValidationTests {

    @ParameterizedTest
    @CsvSource({
        "256.1.1.1",
        "192.168.1",
        "192.168.a.1"
    })
    @DisplayName("Should reject invalid IPv4 addresses")
    void shouldRejectInvalidIpv4Addresses(String invalidIp) {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, invalidIp);
      when(request.getHeaders()).thenReturn(headers);
      when(request.getRemoteAddress()).thenReturn(null);
      assertThrows(UnknownIpAddressException.class, () -> HttpUtilities.getClientIp(request));
    }

    @ParameterizedTest
    @CsvSource({
        "192.168.1.1",
        "10.0.0.1",
        "255.255.255.255",
        "127.0.0.1",
        "10.20.30.40",
        "172.16.0.1",
        "192.168.100.200"
    })
    @DisplayName("Should validate and return valid IPv4 addresses")
    void shouldValidateValidIpv4Addresses(String ipv4Address) {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, ipv4Address);
      when(request.getHeaders()).thenReturn(headers);

      String result = HttpUtilities.getClientIp(request);

      assertEquals(ipv4Address, result);
    }

    @Nested
    @DisplayName("IPv6 Validation Tests")
    class Ipv6ValidationTests {

      @ParameterizedTest
      @CsvSource({
          "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
          "2001:db8::1",
          "::1",
          "::",
          "fe80::1",
          "::ffff:192.168.1.1",
          "2001:0DB8:Ac10:FE01::1"
      })
      @DisplayName("Should validate valid IPv6 addresses")
      void shouldValidateValidIpv6Addresses(String ipv6Address) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpSecurityConstants.X_FORWARDED_FOR, ipv6Address);
        when(request.getHeaders()).thenReturn(headers);
        String result = HttpUtilities.getClientIp(request);
        assertEquals(ipv6Address, result);
      }
    }
  }

  @Nested
  @DisplayName("Edge Cases and Security Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should reject IP address longer than max length")
    void shouldRejectIpLongerThanMaxLength() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      String tooLongIp = "a".repeat(HttpSecurityConstants.MAX_IP_ADDRESS_LENGTH + 1);
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, tooLongIp);
      when(request.getHeaders()).thenReturn(headers);
      when(request.getRemoteAddress()).thenReturn(null);
      assertThrows(UnknownIpAddressException.class, () -> HttpUtilities.getClientIp(request));
    }

    @Test
    @DisplayName("Should handle IP at exact max length")
    void shouldHandleIpAtExactMaxLength() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, "2001:0db8:85a3:0000:0000:8a2e:0370:7334");

      when(request.getHeaders()).thenReturn(headers);
      assertDoesNotThrow(() -> HttpUtilities.getClientIp(request));
    }

    @Test
    @DisplayName("Should handle multiple commas in X-Forwarded-For")
    void shouldHandleMultipleCommasInXForwardedFor() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, "192.168.1.1,,,10.0.0.1");
      when(request.getHeaders()).thenReturn(headers);
      String result = HttpUtilities.getClientIp(request);
      assertEquals("192.168.1.1", result);
    }

    @Test
    @DisplayName("Should handle X-Forwarded-For with only comma")
    void shouldHandleXForwardedForWithOnlyComma() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, ",");
      headers.add(HttpSecurityConstants.X_REAL_IP, "10.0.0.1");
      when(request.getHeaders()).thenReturn(headers);
      String result = HttpUtilities.getClientIp(request);
      assertEquals("10.0.0.1", result);
    }

    @Test
    @DisplayName("Should reject SQL injection attempt in IP")
    void shouldRejectSqlInjectionInIp() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, "'; DROP TABLE users; --");
      when(request.getHeaders()).thenReturn(headers);
      when(request.getRemoteAddress()).thenReturn(null);
      assertThrows(UnknownIpAddressException.class, () -> HttpUtilities.getClientIp(request));
    }

    @Test
    @DisplayName("Should reject XSS attempt in IP")
    void shouldRejectXssAttemptInIp() {
      ServerHttpRequest request = mock(ServerHttpRequest.class);
      HttpHeaders headers = new HttpHeaders();
      headers.add(HttpSecurityConstants.X_FORWARDED_FOR, "<script>alert('xss')</script>");
      when(request.getHeaders()).thenReturn(headers);
      when(request.getRemoteAddress()).thenReturn(null);
      assertThrows(UnknownIpAddressException.class, () -> HttpUtilities.getClientIp(request));
    }

    @Test
    @DisplayName("Should handle concurrent requests safely")
    void shouldHandleConcurrentRequestsSafely() throws InterruptedException {
      // Test thread safety of static method with pre-compiled patterns
      Runnable task = () -> {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpSecurityConstants.X_FORWARDED_FOR, "192.168.1.1");
        when(request.getHeaders()).thenReturn(headers);
        assertDoesNotThrow(() -> HttpUtilities.getClientIp(request));
      };
      Thread[] threads = new Thread[10];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(task);
        threads[i].start();
      }
      for (Thread thread : threads) {
        thread.join();
      }
    }

    @Test
    @DisplayName("Should not instantiate HttpUtilities class")
    void shouldNotInstantiateHttpUtilities() throws Exception {
      var constructor = HttpUtilities.class.getDeclaredConstructor();

      constructor.setAccessible(true);
      var exception = assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);

      assertTrue(exception.getCause() instanceof AssertionError);
      assertEquals("Cannot instantiate HttpUtilities", exception.getCause().getMessage());
    }
  }
}
