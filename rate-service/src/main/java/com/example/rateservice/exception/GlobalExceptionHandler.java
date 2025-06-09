package com.example.rateservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // Handle unsupported currency exception
  @ExceptionHandler(CurrencyNotSupportedException.class)
  public ResponseEntity<Map<String, String>> handleCurrencyException(CurrencyNotSupportedException ex) {
    logger.warn("Currency not supported: {}", ex.getMessage());
    Map<String, String> error = new HashMap<>();
    error.put("error", ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  // Handle exchange rate fetch failures
  @ExceptionHandler(ExchangeRateFetchException.class)
  public ResponseEntity<Map<String, String>> handleExchangeRateException(ExchangeRateFetchException ex) {
    logger.error("Exchange rate fetch failure: {}", ex.getMessage());
    Map<String, String> error = new HashMap<>();
    error.put("error", "Exchange rate retrieval failed: " + ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
  }

  // Handle all other exceptions
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex) {
    logger.error("Unhandled exception occurred: ", ex);
    Map<String, String> error = new HashMap<>();
    error.put("error", "Internal server error: " + ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
