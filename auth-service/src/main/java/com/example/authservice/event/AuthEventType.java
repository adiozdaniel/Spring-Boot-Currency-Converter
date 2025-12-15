package com.example.authservice.event;

public enum AuthEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    TOKEN_GENERATED,
    TOKEN_REFRESHED,
    TOKEN_REVOKED,
    TOKEN_VALIDATED,
    TOKEN_EXPIRED,
    RATE_LIMIT_EXCEEDED,
    INVALID_API_KEY,
    INVALID_TOKEN
}
