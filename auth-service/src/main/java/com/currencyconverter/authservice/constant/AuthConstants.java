package com.currencyconverter.authservice.constant;

/**
 * Constants used across the authentication service.
 */
public final class AuthConstants {

  /**
   * Constant keys and values used in authentication processes.
   */
  public static final String VALID = "valid";
  public static final String BEARER = "Bearer";
  public static final String RESULT = "result";
  public static final String SUBJECT = "subject";
  public static final String FAILURE = "failure";
  public static final String SUCCESS = "success";
  public static final String ISSUED_AT = "issuedAt";
  public static final String EXPIRES_AT = "expiresAt";
  public static final String CLIENT_TYPE = "clientType";

  private AuthConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
