package com.example.rateservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "exchange-rate.api")
public class ExchangeRateApiConfig {
  @Value("${exchange-rate.api.key}")
  private String apiKey;

  @Value("${exchange-rate.api.url}")
  private String apiUrl;

  @Value("${exchange-rate.api.endpoint:/pair}")
  private String apiEndpoint;

  private String key;
  private String url;
  private String endpoint = "/pair";

  // Getters and setters (required)
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = apiKey;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = apiUrl;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
}
