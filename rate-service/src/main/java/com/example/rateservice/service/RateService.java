package com.example.rateservice.service;

import com.example.rateservice.config.ExchangeRateApiConfig;
import com.example.rateservice.exception.CurrencyNotSupportedException;
import com.example.rateservice.exception.ExchangeRateFetchException;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RateService {

  private final ExchangeRateApiConfig apiConfig;

  public RateService(ExchangeRateApiConfig apiConfig) {
    this.apiConfig = apiConfig;
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> fetchRate(String from, String to) {
    try {
      String url = String.format("%s/%s%s/%s/%s",
          apiConfig.getUrl(), apiConfig.getKey(), apiConfig.getEndpoint(), from, to);
      RestTemplate restTemplate = new RestTemplate();
      Map<String, Object> response = (Map<String, Object>) restTemplate.getForObject(url, Map.class);

      // Validate response
      if (response == null || !"success".equals(response.get("result"))) {
        throw new CurrencyNotSupportedException("Currency not supported: " + from + " or " + to);
      }

      // Extract the conversion rate
      double rate = (double) response.get("conversion_rate");
      return Map.of(
          "from", from,
          "to", to,
          "rate", rate);

    } catch (HttpClientErrorException e) {
      throw new CurrencyNotSupportedException("Currency not supported: " + from + " or " + to);
    } catch (Exception e) {
      throw new ExchangeRateFetchException("Failed to retrieve exchange rate. Reason: " + e.getMessage());
    }
  }
}
