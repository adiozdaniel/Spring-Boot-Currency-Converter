package com.example.mainservice.controller;

import com.example.mainservice.dto.ConvertRequest;
import com.example.mainservice.dto.ConvertResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/convert")
public class ConvertController {

  private static final Logger logger = LoggerFactory.getLogger(ConvertController.class);
  private final RestTemplate restTemplate;

  public ConvertController(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
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
      String url = String.format("http://localhost:8080/rate?from=%s&to=%s", from, to);
      ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
          url,
          HttpMethod.GET,
          null,
          new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
      );
      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new RuntimeException("Rate service returned invalid response");
      }

      Map<String, Object> body = response.getBody();
      double rate = ((Number) body.get("rate")).doubleValue();
      double converted = rate * amount;

      return ResponseEntity.ok(new ConvertResponse(from.toUpperCase(), to.toUpperCase(), rate, amount, converted));
    } catch (Exception e) {
      logger.error("Conversion failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
          "error", "Conversion failed: " + e.getMessage()));
    }
  }
}
