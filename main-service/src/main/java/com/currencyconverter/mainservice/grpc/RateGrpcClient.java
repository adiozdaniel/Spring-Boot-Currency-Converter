package com.currencyconverter.mainservice.grpc;

import com.currencyconverter.common.grpc.rate.RateRequest;
import com.currencyconverter.common.grpc.rate.RateResponse;
import com.currencyconverter.common.grpc.rate.RateServiceGrpc;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
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
    private RateServiceGrpc.RateServiceFutureStub rateStub;

    @CircuitBreaker(name = "rate-service")
    @Retry(name = "rate-service")
    public Mono<BigDecimal> getExchangeRate(String from, String to) {
        return Mono.create(sink -> {
            RateRequest request = RateRequest.newBuilder()
                    .setFrom(from)
                    .setTo(to)
                    .build();

            ListenableFuture<RateResponse> future = rateStub
                    .withDeadline(Deadline.after(5, TimeUnit.SECONDS))
                    .getRate(request);

            Futures.addCallback(future, new FutureCallback<RateResponse>() {
                @Override
                public void onSuccess(RateResponse result) {
                    sink.success(BigDecimal.valueOf(result.getRate()));
                }

                @Override
                public void onFailure(Throwable t) {
                    sink.error(t);
                }
            }, MoreExecutors.directExecutor());
            
            sink.onCancel(() -> future.cancel(true));
        });
    }
}
