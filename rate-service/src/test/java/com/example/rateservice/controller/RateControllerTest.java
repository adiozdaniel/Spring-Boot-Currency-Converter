package com.example.rateservice.controller;

import com.example.rateservice.exception.CurrencyNotSupportedException;
import com.example.rateservice.exception.ExchangeRateFetchException;
import com.example.rateservice.service.RateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
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

        when(rateService.fetchRate("USD", "EUR")).thenReturn(expectedResponse);

        // When
        Map<String, Object> result = rateController.getExchangeRate(from, to);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("from")).isEqualTo("USD");
        assertThat(result.get("to")).isEqualTo("EUR");
        assertThat(result.get("rate")).isEqualTo(0.85);

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

        when(rateService.fetchRate("GBP", "JPY")).thenReturn(expectedResponse);

        // When
        rateController.getExchangeRate(from, to);

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

        when(rateService.fetchRate("USD", "EUR")).thenReturn(expectedResponse);

        // When
        Map<String, Object> result = rateController.getExchangeRate(from, to);

        // Then
        assertThat(result).isNotNull();
        verify(rateService).fetchRate("USD", "EUR");
    }

    @Test
    @DisplayName("Should propagate CurrencyNotSupportedException")
    void shouldPropagateCurrencyNotSupportedException() {
        // Given
        String from = "XXX";
        String to = "YYY";

        when(rateService.fetchRate("XXX", "YYY"))
                .thenThrow(new CurrencyNotSupportedException("Currency not supported: XXX or YYY"));

        // When & Then
        assertThatThrownBy(() -> rateController.getExchangeRate(from, to))
                .isInstanceOf(CurrencyNotSupportedException.class)
                .hasMessageContaining("Currency not supported");

        verify(rateService).fetchRate("XXX", "YYY");
    }

    @Test
    @DisplayName("Should propagate ExchangeRateFetchException")
    void shouldPropagateExchangeRateFetchException() {
        // Given
        String from = "USD";
        String to = "EUR";

        when(rateService.fetchRate("USD", "EUR"))
                .thenThrow(new ExchangeRateFetchException("Failed to retrieve exchange rate"));

        // When & Then
        assertThatThrownBy(() -> rateController.getExchangeRate(from, to))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("Failed to retrieve exchange rate");

        verify(rateService).fetchRate("USD", "EUR");
    }

    @Test
    @DisplayName("Should handle various currency pairs")
    void shouldHandleVariousCurrencyPairs() {
        // Given
        String[][] currencyPairs = {
                {"USD", "EUR"},
                {"GBP", "JPY"},
                {"CAD", "AUD"},
                {"CHF", "SEK"}
        };

        for (String[] pair : currencyPairs) {
            Map<String, Object> response = Map.of(
                    "from", pair[0],
                    "to", pair[1],
                    "rate", 1.0
            );
            when(rateService.fetchRate(pair[0], pair[1])).thenReturn(response);

            // When
            Map<String, Object> result = rateController.getExchangeRate(pair[0].toLowerCase(), pair[1].toLowerCase());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("from")).isEqualTo(pair[0]);
            assertThat(result.get("to")).isEqualTo(pair[1]);

            verify(rateService).fetchRate(pair[0], pair[1]);
        }
    }

    @Test
    @DisplayName("Should handle uppercase input without modification")
    void shouldHandleUppercaseInputWithoutModification() {
        // Given
        String from = "USD";
        String to = "EUR";

        Map<String, Object> expectedResponse = Map.of(
                "from", "USD",
                "to", "EUR",
                "rate", 0.85
        );

        when(rateService.fetchRate("USD", "EUR")).thenReturn(expectedResponse);

        // When
        Map<String, Object> result = rateController.getExchangeRate(from, to);

        // Then
        assertThat(result).isNotNull();
        verify(rateService).fetchRate("USD", "EUR");
    }

    @Test
    @DisplayName("Should return response with correct structure")
    void shouldReturnResponseWithCorrectStructure() {
        // Given
        String from = "USD";
        String to = "EUR";

        Map<String, Object> expectedResponse = Map.of(
                "from", "USD",
                "to", "EUR",
                "rate", 0.85
        );

        when(rateService.fetchRate("USD", "EUR")).thenReturn(expectedResponse);

        // When
        Map<String, Object> result = rateController.getExchangeRate(from, to);

        // Then
        assertThat(result).containsKeys("from", "to", "rate");
        assertThat(result).hasSize(3);
    }
}
