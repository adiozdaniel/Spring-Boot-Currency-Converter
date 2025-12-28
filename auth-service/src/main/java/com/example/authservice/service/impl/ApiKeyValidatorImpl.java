package com.example.authservice.service.impl;

import com.example.authservice.config.ApiKeyConfig;
import com.example.authservice.service.ApiKeyValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the {@link ApiKeyValidator} interface.
 * <p>
 * This service provides concrete logic for validating API keys against configured
 * keys for different client types (web, mobile, platform).
 * </p>
 */
@Service
public class ApiKeyValidatorImpl implements ApiKeyValidator {

  private static final Logger logger = LoggerFactory.getLogger(ApiKeyValidatorImpl.class);

  private final ApiKeyConfig apiKeyConfig;

  /**
   * Constructs an {@link ApiKeyValidatorImpl} with the provided API key configuration.
   *
   * @param apiKeyConfig the configuration containing valid API keys.
   */
  public ApiKeyValidatorImpl(ApiKeyConfig apiKeyConfig) {
    this.apiKeyConfig = apiKeyConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Boolean> isValid(String apiKey) {
    return Mono.fromSupplier(() -> {
      if (apiKey == null || apiKey.isBlank()) {
        logger.warn("API key validation failed: null or blank key provided");
        return false;
      }

      boolean valid = apiKey.equals(apiKeyConfig.getWeb()) ||
          apiKey.equals(apiKeyConfig.getMobile()) ||
          apiKey.equals(apiKeyConfig.getPlatform());

      if (!valid) {
        logger.warn("Invalid API key attempt detected");
      } else {
        logger.debug("API key validation successful");
      }

      return valid;

    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<String> getClientType(String apiKey) {

    return Mono.fromSupplier(() -> {
      if (apiKey == null || apiKey.isBlank()) {
        logger.debug("Cannot determine client type: null or blank key");
        return null;
      }

      return apiKey.equals(apiKeyConfig.getWeb()) ? "web"
          : apiKey.equals(apiKeyConfig.getMobile()) ? "mobile"
              : apiKey.equals(apiKeyConfig.getPlatform()) ? "platform" : null;
    }).flatMap(clientType -> clientType != null ? Mono.just(clientType) : Mono.empty());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Mono<Map<String, String>> getApiKeyInfo(String apiKey) {
    return getClientType(apiKey)
        .map(clientType -> {
          Map<String, String> info = new HashMap<>();
          info.put("valid", "true");
          info.put("clientType", clientType);
          return info;
        })
        .switchIfEmpty(Mono.fromSupplier(() -> {
          Map<String, String> info = new HashMap<>();
          info.put("valid", "false");
          return info;
        }));
  }
}
