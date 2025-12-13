package com.example.rateservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ExchangeRateApiConfig apiConfig) {
        return builder
                .setConnectTimeout(Duration.ofMillis(apiConfig.getTimeout()))
                .setReadTimeout(Duration.ofMillis(apiConfig.getTimeout()))
                .build();
    }
}
