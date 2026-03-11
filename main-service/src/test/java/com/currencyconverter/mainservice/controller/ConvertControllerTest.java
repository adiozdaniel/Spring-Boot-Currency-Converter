package com.currencyconverter.mainservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import com.currencyconverter.mainservice.config.RateServiceConfig;
import com.currencyconverter.mainservice.dto.ConvertRequest;
import com.currencyconverter.mainservice.dto.ConvertResponse;
import com.currencyconverter.mainservice.model.Conversion;
import com.currencyconverter.mainservice.service.ConversionService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConvertController Tests")
class ConvertControllerTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private RateServiceConfig rateServiceConfig;

    @Mock
    private ConversionService conversionService;

    private ConvertController convertController;

    @BeforeEach
    void setUp() {
        convertController = new ConvertController(webClient, rateServiceConfig, conversionService);
        when(rateServiceConfig.getUrl()).thenReturn("http://localhost:8082/rates");

        // Mock WebClient fluent API
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("Should convert currency successfully")
    void shouldConvertCurrencySuccessfully() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo("EUR");
        request.setAmount(100.0);

        Map<String, Object> rateResponse = Map.of("rate", 0.85);
        doReturn(Mono.just(rateResponse)).when(responseSpec).bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        Conversion savedConversion = new Conversion("USD", "EUR",
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(85.0), LocalDateTime.now());

        doReturn(Mono.just(savedConversion)).when(conversionService).saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class));

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

        verify(conversionService).saveConversion(
                eq("USD"),
                eq("EUR"),
                eq(BigDecimal.valueOf(100.0)),
                any(BigDecimal.class),
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

        Map<String, Object> rateResponse = Map.of("rate", 0.85);
        doReturn(Mono.just(rateResponse)).when(responseSpec).bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        Conversion savedConversion = new Conversion("USD", "EUR",
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(85.0), LocalDateTime.now());

        doReturn(Mono.just(savedConversion)).when(conversionService).saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class));

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

        verify(conversionService).saveConversion(
                eq("USD"),
                eq("EUR"),
                any(BigDecimal.class),
                any(BigDecimal.class),
                any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should handle large amounts correctly")
    void shouldHandleLargeAmountsCorrectly() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo("EUR");
        request.setAmount(1000000.0);

        Map<String, Object> rateResponse = Map.of("rate", 0.85);
        doReturn(Mono.just(rateResponse)).when(responseSpec).bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        Conversion savedConversion = new Conversion("USD", "EUR",
                BigDecimal.valueOf(1000000.0), BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(850000.0), LocalDateTime.now());

        doReturn(Mono.just(savedConversion)).when(conversionService).saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class));

        // When
        Mono<ResponseEntity<ConvertResponse>> responseMono = convertController.convert(request);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    ConvertResponse convertResponse = response.getBody();
                    assertThat(convertResponse).isNotNull();
                    assertThat(convertResponse.getAmount()).isEqualTo(1000000.0);
                    assertThat(convertResponse.getConverted()).isEqualTo(850000.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle rate as Integer type")
    void shouldHandleRateAsIntegerType() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo("JPY");
        request.setAmount(100.0);

        Map<String, Object> rateResponse = Map.of("rate", 150);
        doReturn(Mono.just(rateResponse)).when(responseSpec).bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        doReturn(Mono.just(new Conversion("USD", "JPY", BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(150), BigDecimal.valueOf(15000.0), LocalDateTime.now())))
                .when(conversionService).saveConversion(anyString(), anyString(), any(BigDecimal.class),
                        any(BigDecimal.class), any(BigDecimal.class));

        // When
        Mono<ResponseEntity<ConvertResponse>> responseMono = convertController.convert(request);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    ConvertResponse convertResponse = response.getBody();
                    assertThat(convertResponse).isNotNull();
                    assertThat(convertResponse.getRate()).isEqualTo(150.0);
                    assertThat(convertResponse.getConverted()).isEqualTo(15000.0);
                })
                .verifyComplete();
    }
}
