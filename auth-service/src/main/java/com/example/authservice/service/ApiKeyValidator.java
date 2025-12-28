package com.example.authservice.service;

import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Service interface for validating API keys.
 * <p>
 * This interface defines the contract for operations related to API key validation,
 * including checking validity, retrieving client types, and fetching API key information.
 * </p>
 */
public interface ApiKeyValidator {

    /**
     * Checks if a given API key is valid.
     *
     * @param apiKey the API key to validate.
     * @return a {@link Mono} emitting {@code true} if the API key is valid, {@code false} otherwise.
     */
    Mono<Boolean> isValid(String apiKey);

    /**
     * Retrieves the client type associated with a given API key.
     *
     * @param apiKey the API key for which to retrieve the client type.
     * @return a {@link Mono} emitting the client type as a {@link String}, or an empty {@link Mono} if not found.
     */
    Mono<String> getClientType(String apiKey);

    /**
     * Retrieves all information associated with a given API key.
     *
     * @param apiKey the API key for which to retrieve information.
     * @return a {@link Mono} emitting a {@link Map} containing API key details, or an empty {@link Mono} if not found.
     */
    Mono<Map<String, String>> getApiKeyInfo(String apiKey);
}
