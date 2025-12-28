package com.example.authservice.exception;

public class RateLimitExceededException extends RuntimeException {

    /**
     * Constructs a new {@link RateLimitExceededException} with a default message.
     */
    public RateLimitExceededException() {
        super("Rate limit exceeded. Please try again later.");
    }

    /**
     * Constructs a new {@link RateLimitExceededException} with the specified detail message.
     *
     * @param message the detail message.
     */
    public RateLimitExceededException(String message) {
        super(message);
    }
}
