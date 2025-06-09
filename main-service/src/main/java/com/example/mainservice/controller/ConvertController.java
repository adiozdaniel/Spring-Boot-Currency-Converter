package com.example.mainservice.controller;

import com.example.mainservice.config.RateServiceConfig;
import com.example.mainservice.dto.ConvertRequest;
import com.example.mainservice.dto.ConvertResponse;
import com.example.mainservice.service.ConversionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/convert")
public class ConvertController {

  private static final Logger logger = LoggerFactory.getLogger(ConvertController.class);
  private final RestTemplate restTemplate;
  private final RateServiceConfig config;
  private final ConversionService conversionService;

  public ConvertController(RestTemplate restTemplate, RateServiceConfig config, ConversionService conversionService) {
    this.restTemplate = restTemplate;
    this.config = config;
    this.conversionService = conversionService;
  }

  @PostMapping
  public ResponseEntity<?> convert(@RequestBody ConvertRequest request) {
    String from = request.getFrom();
    String to = request.getTo();
    double amount = request.getAmount();

    if (from == null || to == null || amount <= 0) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "Invalid input: `from`, `to`, and positive `amount` are required."));
    }

    try {
      String url = String.format("%s?from=%s&to=%s", config.getUrl(), from, to);
      ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
          url,
          HttpMethod.GET,
          null,
          new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
          });
      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new RuntimeException("Rate service returned invalid response");
      }

      Map<String, Object> body = response.getBody();
      double rate = ((Number) body.get("rate")).doubleValue();
      double converted = rate * amount;

      conversionService.saveConversion(
          from.toUpperCase(), to.toUpperCase(),
          BigDecimal.valueOf(amount), BigDecimal.valueOf(rate), BigDecimal.valueOf(converted));

      return ResponseEntity.ok(new ConvertResponse(from.toUpperCase(), to.toUpperCase(), rate, amount, converted));
    } catch (Exception e) {
      logger.error("Conversion failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "error", "Conversion failed: " + e.getMessage()));
    }
  }
}
