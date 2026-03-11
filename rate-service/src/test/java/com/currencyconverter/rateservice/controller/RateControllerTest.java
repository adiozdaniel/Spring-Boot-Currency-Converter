package com.currencyconverter.rateservice.controller;

import com.currencyconverter.rateservice.service.RateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateController Tests")
class RateControllerTest {

    @Mock
    private RateService rateService;

    private RateController rateController;

    @BeforeEach
    void setUp() {
        rateController = new RateController(rateService);
    }

    @Test
    @DisplayName("Should get exchange rate successfully")
    void shouldGetExchangeRateSuccessfully() {
        // Given
        String from = "usd";
        String to = "eur";

        Map<String, Object> expectedResponse = Map.of(
                "from", "USD",
                "to", "EUR",
                "rate", 0.85
        );

        when(rateService.fetchRate("USD", "EUR")).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<Map<String, Object>> resultMono = rateController.getExchangeRate(from, to);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.get("from")).isEqualTo("USD");
                    assertThat(result.get("to")).isEqualTo("EUR");
                    assertThat(result.get("rate")).isEqualTo(0.85);
                })
                .verifyComplete();

        verify(rateService).fetchRate("USD", "EUR");
    }

    @Test
    @DisplayName("Should convert currencies to uppercase")
    void shouldConvertCurrenciesToUppercase() {
        // Given
        String from = "gbp";
        String to = "jpy";

        Map<String, Object> expectedResponse = Map.of(
                "from", "GBP",
                "to", "JPY",
                "rate", 170.50
        );

        when(rateService.fetchRate("GBP", "JPY")).thenReturn(Mono.just(expectedResponse));

        // When
        rateController.getExchangeRate(from, to).subscribe();

        // Then
        verify(rateService).fetchRate("GBP", "JPY");
    }

    @Test
    @DisplayName("Should handle mixed case currencies")
    void shouldHandleMixedCaseCurrencies() {
        // Given
        String from = "UsD";
        String to = "eUr";

        Map<String, Object> expectedResponse = Map.of(
                "from", "USD",
                "to", "EUR",
                "rate", 0.85
        );

        when(rateService.fetchRate("USD", "EUR")).thenReturn(Mono.just(expectedResponse));

        // When
        Mono<Map<String, Object>> resultMono = rateController.getExchangeRate(from, to);

        // Then
        StepVerifier.create(resultMono)
                .expectNextCount(1)
                .verifyComplete();
        
        verify(rateService).fetchRate("USD", "EUR");
    }

    @Test
    @DisplayName("Should handle various currency pairs")
    void shouldHandleVariousCurrencyPairs() {
        // Given
        String from = "CAD";
        String to = "AUD";
        
        Map<String, Object> response = Map.of(
                "from", "CAD",
                "to", "AUD",
                "rate", 1.0
        );
        when(rateService.fetchRate("CAD", "AUD")).thenReturn(Mono.just(response));

        // When
        Mono<Map<String, Object>> resultMono = rateController.getExchangeRate(from.toLowerCase(), to.toLowerCase());

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertThat(result).isNotNull();
                    assertThat(result.get("from")).isEqualTo("CAD");
                    assertThat(result.get("to")).isEqualTo("AUD");
                })
                .verifyComplete();

        verify(rateService).fetchRate("CAD", "AUD");
    }
}
