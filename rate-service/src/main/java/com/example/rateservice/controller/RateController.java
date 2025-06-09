package com.example.rateservice.controller;

import com.example.rateservice.service.RateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/rate")
public class RateController {

  private static final Logger logger = LoggerFactory.getLogger(RateController.class);
  private final RateService rateService;

  public RateController(RateService rateService) {
    this.rateService = rateService;
  }

  @GetMapping
  public Map<String, Object> getExchangeRate(@RequestParam String from, @RequestParam String to) {
    logger.info("Request received for exchange rate from {} to {}", from, to);
    return rateService.fetchRate(from.toUpperCase(), to.toUpperCase());
  }
}
