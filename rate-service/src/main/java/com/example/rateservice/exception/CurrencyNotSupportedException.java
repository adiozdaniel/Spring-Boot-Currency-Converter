package com.example.rateservice.exception;

public class CurrencyNotSupportedException extends RuntimeException {
  public CurrencyNotSupportedException(String message) {
    super(message);
  }
}
