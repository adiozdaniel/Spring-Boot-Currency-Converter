package com.currencyconverter.rateservice.exception;

public class ExchangeRateFetchException extends RuntimeException {
  public ExchangeRateFetchException(String message) {
    super(message);
  }
}
