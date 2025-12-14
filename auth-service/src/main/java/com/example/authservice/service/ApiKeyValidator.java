package com.example.authservice.service;

import com.example.authservice.config.ApiKeyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ApiKeyValidator {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyValidator.class);

    private final ApiKeyConfig apiKeyConfig;

    public ApiKeyValidator(ApiKeyConfig apiKeyConfig) {
        this.apiKeyConfig = apiKeyConfig;
    }

    public boolean isValid(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        return apiKey.equals(apiKeyConfig.getWeb()) ||
               apiKey.equals(apiKeyConfig.getMobile()) ||
               apiKey.equals(apiKeyConfig.getPlatform());
    }

    public String getClientType(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        if (apiKey.equals(apiKeyConfig.getWeb())) {
            return "web";
        } else if (apiKey.equals(apiKeyConfig.getMobile())) {
            return "mobile";
        } else if (apiKey.equals(apiKeyConfig.getPlatform())) {
            return "platform";
        }

        return null;
    }

    public Map<String, String> getApiKeyInfo(String apiKey) {
        Map<String, String> info = new HashMap<>();
        String clientType = getClientType(apiKey);

        if (clientType != null) {
            info.put("valid", "true");
            info.put("clientType", clientType);
        } else {
            info.put("valid", "false");
        }

        return info;
    }
}
