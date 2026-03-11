package com.currencyconverter.mainservice.controller;

import jakarta.validation.Valid;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import com.currencyconverter.mainservice.config.RateServiceConfig;
import com.currencyconverter.mainservice.dto.ConvertRequest;
import com.currencyconverter.mainservice.dto.ConvertResponse;
import com.currencyconverter.mainservice.exception.ServiceException;
import com.currencyconverter.mainservice.service.ConversionService;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/convert")
public class ConvertController {

  private final WebClient webClient;
  private final RateServiceConfig config;
  private final ConversionService conversionService;

  public ConvertController(WebClient webClient, RateServiceConfig config, ConversionService conversionService) {
    this.webClient = webClient;
    this.config = config;
    this.conversionService = conversionService;
  }

  @PostMapping
  public Mono<ResponseEntity<ConvertResponse>> convert(@Valid @RequestBody ConvertRequest request) {
    String from = request.getFrom().toUpperCase();
    String to = request.getTo().toUpperCase();
    double amount = request.getAmount();

    String url = String.format("%s?from=%s&to=%s", config.getUrl(), from, to);

    return webClient.get()
        .uri(url)
        .retrieve()
        .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
            .flatMap(body -> Mono.error(new ServiceException("Rate service returned error: " + body))))
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
        })
        .flatMap(body -> {
          if (body == null || !body.containsKey("rate")) {
            return Mono.error(new ServiceException("Rate service returned invalid response"));
          }

          BigDecimal rate = new BigDecimal(body.get("rate").toString());
          BigDecimal amountDecimal = BigDecimal.valueOf(amount);
          BigDecimal converted = rate.multiply(amountDecimal);

          return conversionService.saveConversion(from, to, amountDecimal, rate, converted)
              .map(saved -> ResponseEntity.ok(new ConvertResponse(
                  from, to, rate.doubleValue(), amount, converted.doubleValue())));
        });
  }
}
