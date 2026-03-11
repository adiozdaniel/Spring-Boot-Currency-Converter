package com.currencyconverter.mainservice.controller;

import com.currencyconverter.mainservice.dto.ConvertRequest;
import com.currencyconverter.mainservice.dto.ConvertResponse;
import com.currencyconverter.mainservice.grpc.RateGrpcClient;
import com.currencyconverter.mainservice.model.Conversion;
import com.currencyconverter.mainservice.service.ConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConvertController Tests")
class ConvertControllerTest {

    @Mock
    private RateGrpcClient rateGrpcClient;

    @Mock
    private ConversionService conversionService;

    private ConvertController convertController;

    @BeforeEach
    void setUp() {
        convertController = new ConvertController(rateGrpcClient, conversionService);
    }

    @Test
    @DisplayName("Should convert currency successfully via gRPC")
    void shouldConvertCurrencySuccessfully() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo("EUR");
        request.setAmount(100.0);

        when(rateGrpcClient.getExchangeRate("USD", "EUR")).thenReturn(Mono.just(BigDecimal.valueOf(0.85)));

        Conversion savedConversion = new Conversion("USD", "EUR",
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(85.0), LocalDateTime.now());

        when(conversionService.saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class))).thenReturn(Mono.just(savedConversion));

        // When
        Mono<ResponseEntity<ConvertResponse>> responseMono = convertController.convert(request);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    ConvertResponse convertResponse = response.getBody();
                    assertThat(convertResponse).isNotNull();
                    assertThat(convertResponse.getFrom()).isEqualTo("USD");
                    assertThat(convertResponse.getTo()).isEqualTo("EUR");
                    assertThat(convertResponse.getRate()).isEqualTo(0.85);
                    assertThat(convertResponse.getAmount()).isEqualTo(100.0);
                    assertThat(convertResponse.getConverted()).isEqualTo(85.0);
                })
                .verifyComplete();

        verify(rateGrpcClient).getExchangeRate("USD", "EUR");
        verify(conversionService).saveConversion(
                eq("USD"),
                eq("EUR"),
                eq(BigDecimal.valueOf(100.0)),
                eq(BigDecimal.valueOf(0.85)),
                any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should convert with lowercase currencies to uppercase")
    void shouldConvertWithLowercaseCurrenciesToUppercase() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("usd");
        request.setTo("eur");
        request.setAmount(100.0);

        when(rateGrpcClient.getExchangeRate("USD", "EUR")).thenReturn(Mono.just(BigDecimal.valueOf(0.85)));

        Conversion savedConversion = new Conversion("USD", "EUR",
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(85.0), LocalDateTime.now());

        when(conversionService.saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class))).thenReturn(Mono.just(savedConversion));

        // When
        Mono<ResponseEntity<ConvertResponse>> responseMono = convertController.convert(request);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    ConvertResponse convertResponse = response.getBody();
                    assertThat(convertResponse).isNotNull();
                    assertThat(convertResponse.getFrom()).isEqualTo("USD");
                    assertThat(convertResponse.getTo()).isEqualTo("EUR");
                })
                .verifyComplete();

        verify(rateGrpcClient).getExchangeRate("USD", "EUR");
    }
}
