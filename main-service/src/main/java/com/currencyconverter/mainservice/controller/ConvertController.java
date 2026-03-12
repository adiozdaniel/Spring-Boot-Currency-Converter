package com.currencyconverter.mainservice.controller;

import com.currencyconverter.mainservice.dto.ConvertRequest;
import com.currencyconverter.mainservice.dto.ConvertResponse;
import com.currencyconverter.mainservice.grpc.RateGrpcClient;
import com.currencyconverter.mainservice.service.ConversionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/convert")
public class ConvertController {

  private final RateGrpcClient rateGrpcClient;
  private final ConversionService conversionService;

  public ConvertController(RateGrpcClient rateGrpcClient, ConversionService conversionService) {
    this.rateGrpcClient = rateGrpcClient;
    this.conversionService = conversionService;
  }

  @PostMapping
  public Mono<ResponseEntity<ConvertResponse>> convert(@Valid @RequestBody ConvertRequest request) {
    String from = request.getFrom().toUpperCase();
    String to = request.getTo().toUpperCase();
    double amount = request.getAmount();
    BigDecimal amountDecimal = BigDecimal.valueOf(amount);

    return rateGrpcClient.getExchangeRate(from, to)
        .flatMap(rate -> {
          BigDecimal converted = amountDecimal.multiply(rate);
          return conversionService.saveConversion(from, to, amountDecimal, rate, converted)
              .map(saved -> ResponseEntity.ok(new ConvertResponse(
                  from, to, rate.doubleValue(), amount, converted.doubleValue())));
        });
  }
}
