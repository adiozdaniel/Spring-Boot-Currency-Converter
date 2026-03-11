package com.currencyconverter.authservice.exception;

/**
 * Exception thrown when a client's IP address cannot be determined.
 * <p>
 * This is a security measure to reject requests from sources where
 * the IP address cannot be reliably identified.
 * </p>
 */
public class UnknownIpAddressException extends RuntimeException {

  /**
   * Constructs a new {@link UnknownIpAddressException} with a default
   * message.
   */
  public UnknownIpAddressException() {
    super("Unable to determine client IP address");
  }

  /**
   * Constructs a new {@link UnknownIpAddressException} with a custom
   * message.
   *
   * @param message the detail message.
   */
  public UnknownIpAddressException(String message) {
    super(message);
  }
}
