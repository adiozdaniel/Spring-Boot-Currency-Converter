package com.example.rateservice.service;

import com.example.rateservice.config.ExchangeRateApiConfig;
import com.example.rateservice.exception.CurrencyNotSupportedException;
import com.example.rateservice.exception.ExchangeRateFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class RateService {

    private static final Logger logger = LoggerFactory.getLogger(RateService.class);

    private final RestTemplate restTemplate;
    private final ExchangeRateApiConfig apiConfig;

    // TODO: Implement caching using the 'cacheTtl' property from ExchangeRateApiConfig

    public RateService(RestTemplate restTemplate, ExchangeRateApiConfig apiConfig) {
        this.restTemplate = restTemplate;
        this.apiConfig = apiConfig;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchRate(String from, String to) {
        String url = UriComponentsBuilder.fromHttpUrl(apiConfig.getUrl())
                .path(apiConfig.getEndpoint())
                .queryParam("access_key", apiConfig.getKey())
                .queryParam("from", from)
                .queryParam("to", to)
                .toUriString();

        try {
            logger.debug("Fetching exchange rate from URL: {}", url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // Basic validation on the response
            if (response == null || response.get("success") == null || !(boolean) response.get("success")) {
                String errorInfo = response != null && response.containsKey("error") ?
                        response.get("error").toString() : "No error information provided.";
                logger.error("API call was not successful. Error: {}", errorInfo);
                throw new ExchangeRateFetchException("API call failed: " + errorInfo);
            }

            if (!response.containsKey("conversion_rate")) {
                throw new ExchangeRateFetchException("Response did not contain 'conversion_rate'.");
            }

            // Extract the conversion rate
            double rate = ((Number) response.get("conversion_rate")).doubleValue();

            return Map.of(
                    "from", from,
                    "to", to,
                    "rate", rate);

        } catch (HttpClientErrorException e) {
            logger.error("Client error while fetching exchange rate for {}/{}: {} - {}", from, to, e.getStatusCode(), e.getResponseBodyAsString());
            throw new CurrencyNotSupportedException("Currency not supported: " + from + " or " + to);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while fetching exchange rate for {}/{}: {}", from, to, e.getMessage());
            throw new ExchangeRateFetchException("Failed to retrieve exchange rate. Reason: " + e.getMessage());
        }
    }
}
