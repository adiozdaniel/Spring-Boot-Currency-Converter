package com.example.mainservice.repository;

import com.example.mainservice.model.Conversion;
import org.springframework.data.repository.CrudRepository;
import java.util.UUID;

public interface ConversionRepository extends CrudRepository<Conversion, UUID> {
}
