package com.example.mainservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class StatusController {

  private static final Logger logger = LoggerFactory.getLogger(StatusController.class);

  @GetMapping("/status")
  public Map<String, String> getStatus() {
    logger.info("Health check requested for main-service");
    return Map.of("status", "UP");
  }
}
