package com.example.rateservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.rateservice.config.ExchangeRateApiConfig;

import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@Service
public class RateService {
  private final ExchangeRateApiConfig apiConfig;

  public RateService(ExchangeRateApiConfig apiConfig) {
    this.apiConfig = apiConfig;
  }

  private static final Logger logger = LoggerFactory.getLogger(RateService.class);

  @SuppressWarnings("unchecked")
  public Map<String, Object> fetchRate(String from, String to) {
    try {
      String url = String.format("%s/%s%s/%s/%s",
          apiConfig.getUrl(), apiConfig.getKey(), apiConfig.getEndpoint(), from, to);
      RestTemplate restTemplate = new RestTemplate();
      Map<String, Object> response = (Map<String, Object>) restTemplate.getForObject(url, Map.class);

      // Validate response
      if (response == null || !"success".equals(response.get("result"))) {
        throw new RuntimeException("Invalid API response: " + response);
      }

      // Extract the conversion rate
      double rate = (double) response.get("conversion_rate");
      return Map.of(
          "from", from,
          "to", to,
          "rate", rate);

    } catch (HttpClientErrorException e) {
      logger.error("API error: {}", e.getMessage());
      throw new RuntimeException("Currency not supported: " + from + " or " + to);
    } catch (Exception e) {
      logger.error("Unexpected error: {}", e.getMessage());
      throw new RuntimeException("Failed to retrieve exchange rate. Reason: " + e.getMessage());
    }
  }
}
