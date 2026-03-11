package com.currencyconverter.rateservice.service;

import com.currencyconverter.rateservice.config.ExchangeRateApiConfig;
import com.currencyconverter.rateservice.exception.CurrencyNotSupportedException;
import com.currencyconverter.rateservice.exception.ExchangeRateFetchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateService Tests")
class RateServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ExchangeRateApiConfig apiConfig;

    private RateService rateService;

    @BeforeEach
    void setUp() {
        lenient().when(apiConfig.getUrl()).thenReturn("https://api.exchangerate-api.com");
        lenient().when(apiConfig.getEndpoint()).thenReturn("/v4/latest");
        lenient().when(apiConfig.getKey()).thenReturn("test-api-key");

        rateService = new RateService(webClient, apiConfig);

        // Mock WebClient fluent API
        lenient().doReturn(requestHeadersUriSpec).when(webClient).get();
        lenient().doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(ArgumentMatchers.<Function<UriBuilder, URI>>any());
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
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

        doReturn(Mono.just(apiResponse)).when(responseSpec)
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        // When
        Mono<Map<String, Object>> resultMono = rateService.fetchRate(from, to);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertThat(result)
                            .isNotNull()
                            .containsEntry("from", from)
                            .containsEntry("to", to)
                            .containsEntry("rate", 0.85);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle integer conversion rate")
    void shouldHandleIntegerConversionRate() {
        // Given
        String from = "USD";
        String to = "JPY";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);
        apiResponse.put("conversion_rate", 150);

        doReturn(Mono.just(apiResponse)).when(responseSpec)
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        // When
        Mono<Map<String, Object>> resultMono = rateService.fetchRate(from, to);

        // Then
        StepVerifier.create(resultMono)
                .assertNext(result -> {
                    assertThat(result)
                            .isNotNull()
                            .containsEntry("rate", 150.0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw ExchangeRateFetchException when response is empty")
    void shouldThrowExceptionWhenResponseIsEmpty() {
        // Given
        String from = "USD";
        String to = "EUR";

        // bodyToMono returning Mono.empty()
        doReturn(Mono.empty()).when(responseSpec)
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        // When
        Mono<Map<String, Object>> resultMono = rateService.fetchRate(from, to);

        // Then
        StepVerifier.create(resultMono)
                .expectError(ExchangeRateFetchException.class)
                .verify();
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

        doReturn(Mono.just(apiResponse)).when(responseSpec)
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        // When
        Mono<Map<String, Object>> resultMono = rateService.fetchRate(from, to);

        // Then
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof ExchangeRateFetchException &&
                        throwable.getMessage().contains("Invalid API key"))
                .verify();
    }

    @Test
    @DisplayName("Should throw ExchangeRateFetchException when conversion_rate is missing")
    void shouldThrowExceptionWhenConversionRateIsMissing() {
        // Given
        String from = "USD";
        String to = "EUR";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", true);

        doReturn(Mono.just(apiResponse)).when(responseSpec)
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        // When
        Mono<Map<String, Object>> resultMono = rateService.fetchRate(from, to);

        // Then
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof ExchangeRateFetchException &&
                        throwable.getMessage().contains("conversion_rate"))
                .verify();
    }

    @Test
    @DisplayName("Should throw ExchangeRateFetchException when response map is null or success is missing")
    void shouldThrowExceptionWhenResponseIsNull() {
        // Given
        String from = "USD";
        String to = "EUR";

        doReturn(Mono.just(new HashMap<>())).when(responseSpec)
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        // When
        Mono<Map<String, Object>> resultMono = rateService.fetchRate(from, to);

        // Then
        StepVerifier.create(resultMono)
                .expectError(ExchangeRateFetchException.class)
                .verify();
    }

    @Test
    @DisplayName("Should throw ExchangeRateFetchException when success is false without error info")
    void shouldThrowExceptionWhenSuccessIsFalseWithoutErrorInfo() {
        // Given
        String from = "USD";
        String to = "EUR";

        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("success", false);

        doReturn(Mono.just(apiResponse)).when(responseSpec)
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        // When
        Mono<Map<String, Object>> resultMono = rateService.fetchRate(from, to);

        // Then
        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> throwable instanceof ExchangeRateFetchException &&
                        throwable.getMessage().contains("No error information provided"))
                .verify();
    }

    @Test
    @DisplayName("Should cover 4xx status mapping")
    @SuppressWarnings("unchecked")
    void shouldCover4xxStatusMapping() {
        // Arrange
        String from = "USD";
        String to = "EUR";

        // Ensure the fluent chain continues
        doReturn(Mono.empty()).when(responseSpec)
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<Map<String, Object>>>any());

        // Act
        rateService.fetchRate(from, to).subscribe();

        // Assert
        ArgumentCaptor<Function<ClientResponse, Mono<? extends Throwable>>> statusHandlerCaptor = ArgumentCaptor
                .forClass(Function.class);
        verify(responseSpec, atLeastOnce()).onStatus(any(), statusHandlerCaptor.capture());

        // Trigger the 4xx handler (the first one)
        Function<ClientResponse, Mono<? extends Throwable>> handler4xx = statusHandlerCaptor.getAllValues().get(0);
        Mono<? extends Throwable> result4xx = handler4xx
                .apply(mock(ClientResponse.class));
        StepVerifier.create(result4xx).expectError(CurrencyNotSupportedException.class).verify();

        // Trigger the general error handler (the second one)
        Function<ClientResponse, Mono<? extends Throwable>> handlerError = statusHandlerCaptor.getAllValues().get(1);
        ClientResponse mockResponse = mock(ClientResponse.class);
        when(mockResponse.bodyToMono(String.class)).thenReturn(Mono.just("API Error"));
        Mono<? extends Throwable> resultError = handlerError.apply(mockResponse);
        StepVerifier.create(resultError).expectError(ExchangeRateFetchException.class).verify();
    }
}
