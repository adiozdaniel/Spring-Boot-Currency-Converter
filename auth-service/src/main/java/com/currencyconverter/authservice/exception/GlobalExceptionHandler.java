package com.currencyconverter.authservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.currencyconverter.authservice.constant.ExceptionConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * <p>
 * This class centralizes the handling of various exceptions that may occur
 * during request processing, providing consistent error responses.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@link InvalidApiKeyException} and returns a 401 Unauthorized
     * response.
     *
     * @param ex the {@link InvalidApiKeyException} that was thrown.
     * @return a {@link ResponseEntity} containing error details and HTTP status
     *         401.
     */
    @ExceptionHandler(InvalidApiKeyException.class)
    public ResponseEntity<Map<String, String>> handleInvalidApiKeyException(InvalidApiKeyException ex) {
        logger.warn("Invalid API key attempt: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put(ExceptionConstants.ERROR, ExceptionConstants.UNAUTHORIZED);
        error.put(ExceptionConstants.MESSAGE, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles {@link InvalidTokenException} and returns a 401 Unauthorized
     * response.
     *
     * @param ex the {@link InvalidTokenException} that was thrown.
     * @return a {@link ResponseEntity} containing error details and HTTP status
     *         401.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidTokenException(InvalidTokenException ex) {
        logger.warn("Invalid token: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put(ExceptionConstants.ERROR, ExceptionConstants.INVALID_TOKEN);
        error.put(ExceptionConstants.MESSAGE, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles {@link TokenRevokedException} and returns a 401 Unauthorized
     * response.
     *
     * @param ex the {@link TokenRevokedException} that was thrown.
     * @return a {@link ResponseEntity} containing error details and HTTP status
     *         401.
     */
    @ExceptionHandler(TokenRevokedException.class)
    public ResponseEntity<Map<String, String>> handleTokenRevokedException(TokenRevokedException ex) {
        logger.warn("Revoked token used: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put(ExceptionConstants.ERROR, ExceptionConstants.TOKEN_REVOKED);
        error.put(ExceptionConstants.MESSAGE, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles {@link RateLimitExceededException} and returns a 429 Too Many
     * Requests response.
     *
     * @param ex the {@link RateLimitExceededException} that was thrown.
     * @return a {@link ResponseEntity} containing error details and HTTP status
     *         429.
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleRateLimitExceededException(RateLimitExceededException ex) {
        logger.warn("Rate limit exceeded: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put(ExceptionConstants.ERROR, ExceptionConstants.RATE_LIMIT_EXCEEDED);
        error.put(ExceptionConstants.MESSAGE, ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Handles {@link MethodArgumentNotValidException} for validation errors and
     * returns a 400 Bad Request response.
     *
     * @param ex the {@link MethodArgumentNotValidException} that was thrown.
     * @return a {@link ResponseEntity} containing validation error details and HTTP
     *         status 400.
     */
    @ExceptionHandler(org.springframework.web.bind.support.WebExchangeBindException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(org.springframework.web.bind.support.WebExchangeBindException ex) {
        logger.warn("Validation failed: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put(ExceptionConstants.ERROR, ExceptionConstants.VALIDATION_FAILED);
        response.put(ExceptionConstants.ERRORS, errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles {@link UnknownIpAddressException} and returns a 403 Forbidden
     * response.
     *
     * @param ex the {@link UnknownIpAddressException} that was thrown.
     * 
     * @return a {@link ResponseEntity} containing error details and HTTP status
     *         403.
     */
    @ExceptionHandler(UnknownIpAddressException.class)
    public ResponseEntity<Map<String, String>> handleUnknownIpAddressException(UnknownIpAddressException ex) {
        logger.warn("Request rejected - unknown IP Address");

        Map<String, String> error = new HashMap<>();
        error.put(ExceptionConstants.ERROR, ExceptionConstants.FORBIDDEN);
        error.put(ExceptionConstants.MESSAGE, ExceptionConstants.UNKNOWN_IP_ADDRESS_MESSAGE);

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles generic {@link Exception} and returns a 500 Internal Server Error
     * response.
     *
     * @param ex the {@link Exception} that was thrown.
     * @return a {@link ResponseEntity} containing generic error details and HTTP
     *         status 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGlobalException(Exception ex) {
        logger.error("Unhandled exception occurred: ", ex);
        Map<String, String> error = new HashMap<>();
        error.put(ExceptionConstants.ERROR, ExceptionConstants.INTERNAL_SERVER_ERROR);
        error.put(ExceptionConstants.MESSAGE, ExceptionConstants.UNEXPECTED_ERROR_MESSAGE);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
