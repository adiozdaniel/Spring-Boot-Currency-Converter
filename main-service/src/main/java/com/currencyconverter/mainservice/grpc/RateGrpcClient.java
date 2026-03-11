package com.currencyconverter.mainservice.grpc;

import com.currencyconverter.common.grpc.rate.RateRequest;
import com.currencyconverter.common.grpc.rate.RateResponse;
import com.currencyconverter.common.grpc.rate.RateServiceGrpc;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.Deadline;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
public class RateGrpcClient {

    @GrpcClient("rate-service")
    private RateServiceGrpc.RateServiceBlockingStub rateStub;

    @CircuitBreaker(name = "rate-service")
    @Retry(name = "rate-service")
    public Mono<BigDecimal> getExchangeRate(String from, String to) {
        return Mono.fromCallable(() -> {
            RateRequest request = RateRequest.newBuilder()
                    .setFrom(from)
                    .setTo(to)
                    .build();
            
            // Set 5 seconds deadline
            RateResponse response = rateStub.withDeadline(Deadline.after(5, TimeUnit.SECONDS))
                    .getRate(request);
            
            return BigDecimal.valueOf(response.getRate());
        });
    }
}
