package com.currencyconverter.mainservice.exception;

public class ServiceException extends RuntimeException {
  public ServiceException(String message) {
    super(message);
  }
}
