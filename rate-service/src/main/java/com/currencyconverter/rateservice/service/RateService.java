package com.currencyconverter.rateservice.service;

import com.currencyconverter.rateservice.config.ExchangeRateApiConfig;
import com.currencyconverter.rateservice.exception.CurrencyNotSupportedException;
import com.currencyconverter.rateservice.exception.ExchangeRateFetchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class RateService {

    private static final Logger logger = LoggerFactory.getLogger(RateService.class);

    private final WebClient webClient;
    private final ExchangeRateApiConfig apiConfig;

    public RateService(WebClient webClient, ExchangeRateApiConfig apiConfig) {
        this.webClient = webClient;
        this.apiConfig = apiConfig;
    }

    public Mono<Map<String, Object>> fetchRate(String from, String to) {
        logger.debug("Fetching exchange rate from {} to {}", from, to);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(apiConfig.getEndpoint())
                        .queryParam("access_key", apiConfig.getKey())
                        .queryParam("from", from)
                        .queryParam("to", to)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    Mono.error(new CurrencyNotSupportedException("Currency not supported: " + from + " or " + to)))
                .onStatus(HttpStatusCode::isError, response -> 
                    response.bodyToMono(String.class)
                        .flatMap(body -> Mono.error(new ExchangeRateFetchException("API call failed: " + body))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .switchIfEmpty(Mono.error(new ExchangeRateFetchException("API call failed: empty response")))
                .flatMap(response -> {
                    if (response == null || response.get("success") == null || !(boolean) response.get("success")) {
                        String errorInfo = response != null && response.containsKey("error") ?
                                response.get("error").toString() : "No error information provided.";
                        logger.error("API call was not successful. Error: {}", errorInfo);
                        return Mono.error(new ExchangeRateFetchException("API call failed: " + errorInfo));
                    }

                    if (!response.containsKey("conversion_rate")) {
                        return Mono.error(new ExchangeRateFetchException("Response did not contain 'conversion_rate'."));
                    }

                    double rate = ((Number) response.get("conversion_rate")).doubleValue();

                    Map<String, Object> result = new HashMap<>();
                    result.put("from", from);
                    result.put("to", to);
                    result.put("rate", rate);

                    return Mono.just(result);
                })
                .doOnError(e -> logger.error("An error occurred while fetching exchange rate for {}/{}: {}", from, to, e.getMessage()));
    }
}
