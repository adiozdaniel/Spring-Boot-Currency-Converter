package com.currencyconverter.authservice.exception;

public class InvalidApiKeyException extends RuntimeException {

    /**
     * Constructs a new {@link InvalidApiKeyException} with a default message.
     */
    public InvalidApiKeyException() {
        super("Invalid API key");
    }

    /**
     * Constructs a new {@link InvalidApiKeyException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public InvalidApiKeyException(String message) {
        super(message);
    }
}
