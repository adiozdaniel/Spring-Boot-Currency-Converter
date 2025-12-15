package com.example.rateservice.service;

import com.example.rateservice.config.ExchangeRateApiConfig;
import com.example.rateservice.exception.CurrencyNotSupportedException;
import com.example.rateservice.exception.ExchangeRateFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateService Tests")
class RateServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ExchangeRateApiConfig apiConfig;

    private RateService rateService;

    @BeforeEach
    void setUp() {
        when(apiConfig.getUrl()).thenReturn("https://api.exchangerate-api.com");
        when(apiConfig.getEndpoint()).thenReturn("/v4/latest");
        when(apiConfig.getKey()).thenReturn("test-api-key");

        rateService = new RateService(restTemplate, apiConfig);
    }

    @Test
    @DisplayName("Should fetch exchange rate successfully")
    void shouldFetchExchangeRateSuccessfully() {
        // Given
        String from = "USD";
        String to = "EUR";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);
        apiResponse.put("conversion_rate", 0.85);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        // When
        Map<String, Object> result = rateService.fetchRate(from, to);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("from")).isEqualTo(from);
        assertThat(result.get("to")).isEqualTo(to);
        assertThat(result.get("rate")).isEqualTo(0.85);

        verify(restTemplate).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Should handle integer conversion rate")
    void shouldHandleIntegerConversionRate() {
        // Given
        String from = "USD";
        String to = "JPY";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);
        apiResponse.put("conversion_rate", 150); // Integer instead of double

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        // When
        Map<String, Object> result = rateService.fetchRate(from, to);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("rate")).isEqualTo(150.0);
    }

    @Test
    @DisplayName("Should throw ExchangeRateFetchException when response is null")
    void shouldThrowExceptionWhenResponseIsNull() {
        // Given
        String from = "USD";
        String to = "EUR";

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> rateService.fetchRate(from, to))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("API call failed");
    }

    @Test
    @DisplayName("Should throw ExchangeRateFetchException when success is false")
    void shouldThrowExceptionWhenSuccessIsFalse() {
        // Given
        String from = "USD";
        String to = "EUR";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", false);
        apiResponse.put("error", "Invalid API key");

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        // When & Then
        assertThatThrownBy(() -> rateService.fetchRate(from, to))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("API call failed")
                .hasMessageContaining("Invalid API key");
    }

    @Test
    @DisplayName("Should throw ExchangeRateFetchException when success field is missing")
    void shouldThrowExceptionWhenSuccessFieldMissing() {
        // Given
        String from = "USD";
        String to = "EUR";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("conversion_rate", 0.85);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        // When & Then
        assertThatThrownBy(() -> rateService.fetchRate(from, to))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("API call failed");
    }

    @Test
    @DisplayName("Should throw ExchangeRateFetchException when conversion_rate is missing")
    void shouldThrowExceptionWhenConversionRateIsMissing() {
        // Given
        String from = "USD";
        String to = "EUR";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        // When & Then
        assertThatThrownBy(() -> rateService.fetchRate(from, to))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("Response did not contain 'conversion_rate'");
    }

    @Test
    @DisplayName("Should throw CurrencyNotSupportedException on HttpClientErrorException")
    void shouldThrowCurrencyNotSupportedExceptionOnHttpClientError() {
        // Given
        String from = "XXX";
        String to = "YYY";

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid currency"));

        // When & Then
        assertThatThrownBy(() -> rateService.fetchRate(from, to))
                .isInstanceOf(CurrencyNotSupportedException.class)
                .hasMessageContaining("Currency not supported");
    }

    @Test
    @DisplayName("Should throw ExchangeRateFetchException on generic exception")
    void shouldThrowExchangeRateFetchExceptionOnGenericException() {
        // Given
        String from = "USD";
        String to = "EUR";

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When & Then
        assertThatThrownBy(() -> rateService.fetchRate(from, to))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("Failed to retrieve exchange rate")
                .hasMessageContaining("Network error");
    }

    @Test
    @DisplayName("Should handle error info without error field")
    void shouldHandleErrorInfoWithoutErrorField() {
        // Given
        String from = "USD";
        String to = "EUR";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", false);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        // When & Then
        assertThatThrownBy(() -> rateService.fetchRate(from, to))
                .isInstanceOf(ExchangeRateFetchException.class)
                .hasMessageContaining("No error information provided");
    }

    @Test
    @DisplayName("Should fetch rate with different currency pairs")
    void shouldFetchRateWithDifferentCurrencyPairs() {
        // Given
        String from = "GBP";
        String to = "JPY";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);
        apiResponse.put("conversion_rate", 170.50);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        // When
        Map<String, Object> result = rateService.fetchRate(from, to);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("from")).isEqualTo(from);
        assertThat(result.get("to")).isEqualTo(to);
        assertThat(result.get("rate")).isEqualTo(170.50);
    }

    @Test
    @DisplayName("Should handle very small conversion rates")
    void shouldHandleVerySmallConversionRates() {
        // Given
        String from = "BTC";
        String to = "USD";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);
        apiResponse.put("conversion_rate", 0.0000123);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        // When
        Map<String, Object> result = rateService.fetchRate(from, to);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("rate")).isEqualTo(0.0000123);
    }

    @Test
    @DisplayName("Should handle very large conversion rates")
    void shouldHandleVeryLargeConversionRates() {
        // Given
        String from = "USD";
        String to = "IDR";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);
        apiResponse.put("conversion_rate", 15000.50);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        // When
        Map<String, Object> result = rateService.fetchRate(from, to);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("rate")).isEqualTo(15000.50);
    }
}
