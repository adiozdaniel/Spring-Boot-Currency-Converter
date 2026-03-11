package com.example.mainservice.service;

import com.example.mainservice.exception.ServiceException;
import com.example.mainservice.model.Conversion;
import com.example.mainservice.repository.ConversionRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ConversionService {

  private final ConversionRepository repository;

  public ConversionService(ConversionRepository repository) {
    this.repository = repository;
  }

  public Mono<Conversion> saveConversion(String from, String to, BigDecimal amount, BigDecimal rate,
      BigDecimal convertedAmount) {
    Conversion conversion = new Conversion(from, to, amount, rate, convertedAmount, LocalDateTime.now());
    return repository.save(conversion)
        .onErrorMap(e -> new ServiceException("Failed to save conversion: " + e.getMessage()));
  }
}
