package com.example.authservice.exception;

public class InvalidTokenException extends RuntimeException {

    /**
     * Constructs a new {@link InvalidTokenException} with a default message.
     */
    public InvalidTokenException() {
        super("Invalid or expired token");
    }

    /**
     * Constructs a new {@link InvalidTokenException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public InvalidTokenException(String message) {
        super(message);
    }
}
