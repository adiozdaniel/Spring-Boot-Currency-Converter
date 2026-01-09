package com.example.authservice.exception;

public class TokenRevokedException extends RuntimeException {

    /**
     * Constructs a new {@link TokenRevokedException} with a default message.
     */
    public TokenRevokedException() {
        super("Token has been revoked");
    }

    /**
     * Constructs a new {@link TokenRevokedException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public TokenRevokedException(String message) {
        super(message);
    }
}
