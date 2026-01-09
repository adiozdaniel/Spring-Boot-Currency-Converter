package com.example.authservice.event;

/**
 * Enumeration of authentication event types.
 * <p>
 * This enum defines the different types of events that can be recorded by the
 * authentication service.
 * </p>
 */
public enum AuthEventType {
    /**
     * A successful login event.
     */
    LOGIN_SUCCESS,
    /**
     * A failed login attempt.
     */
    LOGIN_FAILED,
    /**
     * An event indicating that the rate limit was exceeded.
     */
    RATE_LIMIT_EXCEEDED,
    /**
     * An event indicating that an invalid API key was used.
     */
    INVALID_API_KEY
}
