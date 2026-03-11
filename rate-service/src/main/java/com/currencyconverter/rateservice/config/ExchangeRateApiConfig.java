package com.currencyconverter.rateservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "exchange-rate.api")
public class ExchangeRateApiConfig {

  private String key;
  private String url;
  private String endpoint;
  private int connectTimeout;
  private int readTimeout;
  private int cacheTtl;

  // Getters and setters
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }

  public int getCacheTtl() {
    return cacheTtl;
  }

  public void setCacheTtl(int cacheTtl) {
    this.cacheTtl = cacheTtl;
  }
}
