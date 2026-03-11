package com.currencyconverter.mainservice.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.currencyconverter.mainservice.model.Conversion;

import java.util.UUID;

public interface ConversionRepository extends ReactiveCrudRepository<Conversion, UUID> {
}
