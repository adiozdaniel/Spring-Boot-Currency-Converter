package com.currencyconverter.rateservice.grpc;

import com.currencyconverter.common.grpc.rate.RateRequest;
import com.currencyconverter.common.grpc.rate.RateResponse;
import com.currencyconverter.rateservice.service.RateService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateGrpcService Tests")
class RateGrpcServiceTest {

    @Mock
    private RateService rateService;

    @Mock
    private StreamObserver<RateResponse> responseObserver;

    private RateGrpcService rateGrpcService;

    @BeforeEach
    void setUp() {
        rateGrpcService = new RateGrpcService(rateService);
    }

    @Test
    @DisplayName("Should get rate successfully via gRPC")
    void shouldGetRateSuccessfully() {
        // Given
        RateRequest request = RateRequest.newBuilder()
                .setFrom("USD")
                .setTo("EUR")
                .build();

        Map<String, Object> rateMap = new HashMap<>();
        rateMap.put("from", "USD");
        rateMap.put("to", "EUR");
        rateMap.put("rate", 0.85);

        when(rateService.fetchRate("USD", "EUR")).thenReturn(Mono.just(rateMap));

        // When
        rateGrpcService.getRate(request, responseObserver);

        // Then
        ArgumentCaptor<RateResponse> responseCaptor = ArgumentCaptor.forClass(RateResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        RateResponse response = responseCaptor.getValue();
        assertThat(response.getFrom()).isEqualTo("USD");
        assertThat(response.getTo()).isEqualTo("EUR");
        assertThat(response.getRate()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("Should handle lowercase currencies in gRPC request")
    void shouldHandleLowercaseCurrencies() {
        // Given
        RateRequest request = RateRequest.newBuilder()
                .setFrom("usd")
                .setTo("eur")
                .build();

        Map<String, Object> rateMap = new HashMap<>();
        rateMap.put("from", "USD");
        rateMap.put("to", "EUR");
        rateMap.put("rate", 0.85);

        when(rateService.fetchRate("USD", "EUR")).thenReturn(Mono.just(rateMap));

        // When
        rateGrpcService.getRate(request, responseObserver);

        // Then
        verify(rateService).fetchRate("USD", "EUR");
        verify(responseObserver).onNext(any(RateResponse.class));
        verify(responseObserver).onCompleted();
    }

    @Test
    @DisplayName("Should handle error from RateService in gRPC call")
    void shouldHandleError() {
        // Given
        RateRequest request = RateRequest.newBuilder()
                .setFrom("USD")
                .setTo("EUR")
                .build();

        when(rateService.fetchRate(anyString(), anyString())).thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        rateGrpcService.getRate(request, responseObserver);

        // Then
        verify(responseObserver).onError(any(Exception.class));
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    @DisplayName("Should handle non-Exception throwable in gRPC call")
    void shouldHandleNonExceptionThrowable() {
        // Given
        RateRequest request = RateRequest.newBuilder()
                .setFrom("USD")
                .setTo("EUR")
                .build();

        // Throwable that is not an Exception (e.g. an Error)
        when(rateService.fetchRate(anyString(), anyString())).thenReturn(Mono.error(new OutOfMemoryError("OOM")));

        // When
        rateGrpcService.getRate(request, responseObserver);

        // Then
        verify(responseObserver).onError(any(Exception.class));
    }
}
