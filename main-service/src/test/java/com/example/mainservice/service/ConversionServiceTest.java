package com.example.mainservice.service;

import com.example.mainservice.exception.ServiceException;
import com.example.mainservice.model.Conversion;
import com.example.mainservice.repository.ConversionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        when(conversionRepository.save(any(Conversion.class))).thenReturn(savedConversion);

        // When
        Conversion result = conversionService.saveConversion(from, to, amount, rate, convertedAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFromCurrency()).isEqualTo(from);
        assertThat(result.getToCurrency()).isEqualTo(to);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getRate()).isEqualTo(rate);
        assertThat(result.getConvertedAmount()).isEqualTo(convertedAmount);

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
    @DisplayName("Should save conversion with different currencies")
    void shouldSaveConversionWithDifferentCurrencies() {
        // Given
        String from = "GBP";
        String to = "JPY";
        BigDecimal amount = new BigDecimal("50.00");
        BigDecimal rate = new BigDecimal("170.50");
        BigDecimal convertedAmount = new BigDecimal("8525.00");

        Conversion savedConversion = new Conversion(from, to, amount, rate, convertedAmount, LocalDateTime.now());

        when(conversionRepository.save(any(Conversion.class))).thenReturn(savedConversion);

        // When
        Conversion result = conversionService.saveConversion(from, to, amount, rate, convertedAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFromCurrency()).isEqualTo(from);
        assertThat(result.getToCurrency()).isEqualTo(to);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getRate()).isEqualByComparingTo(rate);
        assertThat(result.getConvertedAmount()).isEqualByComparingTo(convertedAmount);

        verify(conversionRepository).save(any(Conversion.class));
    }

    @Test
    @DisplayName("Should save conversion with decimal values")
    void shouldSaveConversionWithDecimalValues() {
        // Given
        String from = "EUR";
        String to = "USD";
        BigDecimal amount = new BigDecimal("123.45");
        BigDecimal rate = new BigDecimal("1.18");
        BigDecimal convertedAmount = new BigDecimal("145.671");

        Conversion savedConversion = new Conversion(from, to, amount, rate, convertedAmount, LocalDateTime.now());

        when(conversionRepository.save(any(Conversion.class))).thenReturn(savedConversion);

        // When
        Conversion result = conversionService.saveConversion(from, to, amount, rate, convertedAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getRate()).isEqualByComparingTo(rate);
        assertThat(result.getConvertedAmount()).isEqualByComparingTo(convertedAmount);

        verify(conversionRepository, times(1)).save(any(Conversion.class));
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
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThatThrownBy(() -> conversionService.saveConversion(from, to, amount, rate, convertedAmount))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Failed to save conversion")
                .hasMessageContaining("Database connection failed");

        verify(conversionRepository).save(any(Conversion.class));
    }

    @Test
    @DisplayName("Should throw ServiceException with proper message when save fails due to null pointer")
    void shouldThrowServiceExceptionWhenSaveFailsDueToNullPointer() {
        // Given
        String from = "USD";
        String to = "EUR";
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal rate = new BigDecimal("0.85");
        BigDecimal convertedAmount = new BigDecimal("85.00");

        when(conversionRepository.save(any(Conversion.class)))
                .thenThrow(new NullPointerException("Null value encountered"));

        // When & Then
        assertThatThrownBy(() -> conversionService.saveConversion(from, to, amount, rate, convertedAmount))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Failed to save conversion");

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

        when(conversionRepository.save(any(Conversion.class))).thenReturn(savedConversion);

        // When
        Conversion result = conversionService.saveConversion(from, to, amount, rate, convertedAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        verify(conversionRepository).save(any(Conversion.class));
    }

    @Test
    @DisplayName("Should save conversion with very small rates")
    void shouldSaveConversionWithVerySmallRates() {
        // Given
        String from = "BTC";
        String to = "USD";
        BigDecimal amount = new BigDecimal("1.00");
        BigDecimal rate = new BigDecimal("0.0000001");
        BigDecimal convertedAmount = new BigDecimal("0.0000001");

        Conversion savedConversion = new Conversion(from, to, amount, rate, convertedAmount, LocalDateTime.now());

        when(conversionRepository.save(any(Conversion.class))).thenReturn(savedConversion);

        // When
        Conversion result = conversionService.saveConversion(from, to, amount, rate, convertedAmount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRate()).isEqualByComparingTo(rate);
        verify(conversionRepository).save(any(Conversion.class));
    }
}
