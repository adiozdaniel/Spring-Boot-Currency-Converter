package com.example.authservice.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Rate limit exceeded. Please try again later.");
    }

    public RateLimitExceededException(String message) {
        super(message);
    }
}
