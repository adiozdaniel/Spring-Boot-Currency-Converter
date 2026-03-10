package com.example.authservice.constant;

/**
 * Exception-related constants for authentication controller.
 */
public final class ExceptionConstants {

  /**
   * Constant for exception keys and messages.
   */
  public static final String ERROR = "error";
  public static final String ERRORS = "errors";
  public static final String MESSAGE = "message";
  public static final String FORBIDDEN = "forbidden";
  public static final String UNAUTHORIZED = "unauthorized";
  public static final String INVALID_TOKEN = "invalid_token";
  public static final String TOKEN_REVOKED = "token_revoked";
  public static final String VALIDATION_FAILED = "validation_failed";
  public static final String RATE_LIMIT_EXCEEDED = "rate_limit_exceeded";
  public static final String INTERNAL_SERVER_ERROR = "internal_server_error";
  public static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";
  public static final String UNKNOWN_IP_ADDRESS_MESSAGE = "Unable to determine client IP address";

  private ExceptionConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
