package com.example.mainservice.controller;

import com.example.mainservice.config.RateServiceConfig;
import com.example.mainservice.dto.ConvertRequest;
import com.example.mainservice.dto.ConvertResponse;
import com.example.mainservice.exception.ServiceException;
import com.example.mainservice.model.Conversion;
import com.example.mainservice.service.ConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConvertController Tests")
class ConvertControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RateServiceConfig rateServiceConfig;

    @Mock
    private ConversionService conversionService;

    private ConvertController convertController;

    @BeforeEach
    void setUp() {
        convertController = new ConvertController(restTemplate, rateServiceConfig, conversionService);
        when(rateServiceConfig.getUrl()).thenReturn("http://localhost:8082/rates");
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
        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(rateResponse);

        Conversion savedConversion = new Conversion("USD", "EUR",
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(85.0), LocalDateTime.now());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        when(conversionService.saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(savedConversion);

        // When
        ResponseEntity<?> response = convertController.convert(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ConvertResponse.class);

        ConvertResponse convertResponse = (ConvertResponse) response.getBody();
        assertThat(convertResponse.getFrom()).isEqualTo("USD");
        assertThat(convertResponse.getTo()).isEqualTo("EUR");
        assertThat(convertResponse.getRate()).isEqualTo(0.85);
        assertThat(convertResponse.getAmount()).isEqualTo(100.0);
        assertThat(convertResponse.getConverted()).isEqualTo(85.0);

        verify(restTemplate).exchange(
                eq("http://localhost:8082/rates?from=USD&to=EUR"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class));

        verify(conversionService).saveConversion(
                eq("USD"),
                eq("EUR"),
                eq(BigDecimal.valueOf(100.0)),
                any(BigDecimal.class),
                any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should return bad request when from currency is null")
    void shouldReturnBadRequestWhenFromCurrencyIsNull() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom(null);
        request.setTo("EUR");
        request.setAmount(100.0);

        // When
        ResponseEntity<?> response = convertController.convert(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, String> errorBody = (Map<String, String>) response.getBody();
        assertThat(errorBody).containsKey("error");
        assertThat(errorBody.get("error")).contains("Invalid input");

        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
        verify(conversionService, never()).saveConversion(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return bad request when to currency is null")
    void shouldReturnBadRequestWhenToCurrencyIsNull() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo(null);
        request.setAmount(100.0);

        // When
        ResponseEntity<?> response = convertController.convert(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, String> errorBody = (Map<String, String>) response.getBody();
        assertThat(errorBody).containsKey("error");
        assertThat(errorBody.get("error")).contains("Invalid input");

        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
        verify(conversionService, never()).saveConversion(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return bad request when amount is zero")
    void shouldReturnBadRequestWhenAmountIsZero() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo("EUR");
        request.setAmount(0.0);

        // When
        ResponseEntity<?> response = convertController.convert(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, String> errorBody = (Map<String, String>) response.getBody();
        assertThat(errorBody).containsKey("error");
        assertThat(errorBody.get("error")).contains("Invalid input");

        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
        verify(conversionService, never()).saveConversion(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return bad request when amount is negative")
    void shouldReturnBadRequestWhenAmountIsNegative() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo("EUR");
        request.setAmount(-50.0);

        // When
        ResponseEntity<?> response = convertController.convert(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, String> errorBody = (Map<String, String>) response.getBody();
        assertThat(errorBody).containsKey("error");
        assertThat(errorBody.get("error")).contains("Invalid input");

        verify(restTemplate, never()).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("Should throw ServiceException when rate service returns non-2xx status")
    void shouldThrowServiceExceptionWhenRateServiceReturnsNon2xxStatus() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo("EUR");
        request.setAmount(100.0);

        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // When & Then
        assertThatThrownBy(() -> convertController.convert(request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Rate service returned invalid response");

        verify(conversionService, never()).saveConversion(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw ServiceException when rate service returns null body")
    void shouldThrowServiceExceptionWhenRateServiceReturnsNullBody() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo("EUR");
        request.setAmount(100.0);

        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok().build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        // When & Then
        assertThatThrownBy(() -> convertController.convert(request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Rate service returned invalid response");

        verify(conversionService, never()).saveConversion(anyString(), anyString(), any(), any(), any());
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
        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(rateResponse);

        Conversion savedConversion = new Conversion("USD", "EUR",
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(85.0), LocalDateTime.now());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        when(conversionService.saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(savedConversion);

        // When
        ResponseEntity<?> response = convertController.convert(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ConvertResponse convertResponse = (ConvertResponse) response.getBody();
        assertThat(convertResponse.getFrom()).isEqualTo("USD");
        assertThat(convertResponse.getTo()).isEqualTo("EUR");

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
        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(rateResponse);

        Conversion savedConversion = new Conversion("USD", "EUR",
                BigDecimal.valueOf(1000000.0), BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(850000.0), LocalDateTime.now());

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        when(conversionService.saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(savedConversion);

        // When
        ResponseEntity<?> response = convertController.convert(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ConvertResponse convertResponse = (ConvertResponse) response.getBody();
        assertThat(convertResponse.getAmount()).isEqualTo(1000000.0);
        assertThat(convertResponse.getConverted()).isEqualTo(850000.0);
    }

    @Test
    @DisplayName("Should handle decimal amounts correctly")
    void shouldHandleDecimalAmountsCorrectly() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("GBP");
        request.setTo("USD");
        request.setAmount(123.45);

        Map<String, Object> rateResponse = Map.of("rate", 1.25);
        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(rateResponse);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        when(conversionService.saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new Conversion("GBP", "USD", BigDecimal.valueOf(123.45),
                        BigDecimal.valueOf(1.25), BigDecimal.valueOf(154.3125), LocalDateTime.now()));

        // When
        ResponseEntity<?> response = convertController.convert(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ConvertResponse convertResponse = (ConvertResponse) response.getBody();
        assertThat(convertResponse.getAmount()).isEqualTo(123.45);
    }

    @Test
    @DisplayName("Should handle rate as Integer type")
    void shouldHandleRateAsIntegerType() {
        // Given
        ConvertRequest request = new ConvertRequest();
        request.setFrom("USD");
        request.setTo("JPY");
        request.setAmount(100.0);

        Map<String, Object> rateResponse = Map.of("rate", 150); // Integer instead of Double
        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(rateResponse);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        when(conversionService.saveConversion(anyString(), anyString(), any(BigDecimal.class),
                any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(new Conversion("USD", "JPY", BigDecimal.valueOf(100.0),
                        BigDecimal.valueOf(150), BigDecimal.valueOf(15000.0), LocalDateTime.now()));

        // When
        ResponseEntity<?> response = convertController.convert(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ConvertResponse convertResponse = (ConvertResponse) response.getBody();
        assertThat(convertResponse.getRate()).isEqualTo(150.0);
        assertThat(convertResponse.getConverted()).isEqualTo(15000.0);
    }
}
