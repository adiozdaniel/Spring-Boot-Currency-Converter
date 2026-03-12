package com.currencyconverter.rateservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class StatusController {

  private static final Logger logger = LoggerFactory.getLogger(StatusController.class);

  @GetMapping("/status")
  public Mono<Map<String, String>> getStatus() {
    logger.info("Health check requested for rate-service");
    return Mono.just(Map.of("status", "UP"));
  }
}
