package com.example.mainservice.service;

import com.example.mainservice.exception.ServiceException;
import com.example.mainservice.model.Conversion;
import com.example.mainservice.repository.ConversionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversionService Tests")
class ConversionServiceTest {

    @Mock
    private ConversionRepository conversionRepository;

    @InjectMocks
    private ConversionService conversionService;

    @Test
    @DisplayName("Should successfully save conversion")
    void shouldSaveConversion() {
        // Given
        String from = "USD";
        String to = "EUR";
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal rate = new BigDecimal("0.85");
        BigDecimal convertedAmount = new BigDecimal("85.00");

        Conversion savedConversion = new Conversion(from, to, amount, rate, convertedAmount, LocalDateTime.now());

        when(conversionRepository.save(any(Conversion.class))).thenReturn(Mono.just(savedConversion));

        // When
        Mono<Conversion> resultMono = conversionService.saveConversion(from, to, amount, rate, convertedAmount);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.getFromCurrency()).isEqualTo(from);
                    assertThat(result.getToCurrency()).isEqualTo(to);
                    assertThat(result.getAmount()).isEqualTo(amount);
                    assertThat(result.getRate()).isEqualTo(rate);
                    assertThat(result.getConvertedAmount()).isEqualTo(convertedAmount);
                })
                .verifyComplete();

        ArgumentCaptor<Conversion> captor = ArgumentCaptor.forClass(Conversion.class);
        verify(conversionRepository).save(captor.capture());

        Conversion captured = captor.getValue();
        assertThat(captured.getFromCurrency()).isEqualTo(from);
        assertThat(captured.getToCurrency()).isEqualTo(to);
        assertThat(captured.getAmount()).isEqualTo(amount);
        assertThat(captured.getRate()).isEqualTo(rate);
        assertThat(captured.getConvertedAmount()).isEqualTo(convertedAmount);
        assertThat(captured.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should throw ServiceException when repository save fails")
    void shouldThrowServiceExceptionWhenRepositorySaveFails() {
        // Given
        String from = "USD";
        String to = "EUR";
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal rate = new BigDecimal("0.85");
        BigDecimal convertedAmount = new BigDecimal("85.00");

        when(conversionRepository.save(any(Conversion.class)))
                .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

        // When
        Mono<Conversion> resultMono = conversionService.saveConversion(from, to, amount, rate, convertedAmount);

        // Then
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof ServiceException &&
                        throwable.getMessage().contains("Failed to save conversion") &&
                        throwable.getMessage().contains("Database connection failed"))
                .verify();

        verify(conversionRepository).save(any(Conversion.class));
    }

    @Test
    @DisplayName("Should save conversion with large amounts")
    void shouldSaveConversionWithLargeAmounts() {
        // Given
        String from = "USD";
        String to = "EUR";
        BigDecimal amount = new BigDecimal("999999999.99");
        BigDecimal rate = new BigDecimal("0.85");
        BigDecimal convertedAmount = amount.multiply(rate);

        Conversion savedConversion = new Conversion(from, to, amount, rate, convertedAmount, LocalDateTime.now());

        when(conversionRepository.save(any(Conversion.class))).thenReturn(Mono.just(savedConversion));

        // When
        Mono<Conversion> resultMono = conversionService.saveConversion(from, to, amount, rate, convertedAmount);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> assertThat(result.getAmount()).isEqualByComparingTo(amount))
                .verifyComplete();
        verify(conversionRepository).save(any(Conversion.class));
    }
}
