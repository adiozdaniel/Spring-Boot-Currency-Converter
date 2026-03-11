package com.example.mainservice.repository;

import com.example.mainservice.model.Conversion;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import java.util.UUID;

public interface ConversionRepository extends ReactiveCrudRepository<Conversion, UUID> {
}
