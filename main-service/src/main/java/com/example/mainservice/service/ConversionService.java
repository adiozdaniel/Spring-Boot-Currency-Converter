package com.example.mainservice.service;

import com.example.mainservice.model.Conversion;
import com.example.mainservice.repository.ConversionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ConversionService {

  private final ConversionRepository repository;

  public ConversionService(ConversionRepository repository) {
    this.repository = repository;
  }

  public Conversion saveConversion(String from, String to, BigDecimal amount, BigDecimal rate) {
    BigDecimal result = amount.multiply(rate);
    Conversion conversion = new Conversion(from, to, amount, rate, result, LocalDateTime.now());
    return repository.save(conversion);
  }
}
