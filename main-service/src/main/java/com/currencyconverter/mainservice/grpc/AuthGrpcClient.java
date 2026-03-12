package com.currencyconverter.mainservice.grpc;

import com.currencyconverter.common.grpc.auth.AuthServiceGrpc;
import com.currencyconverter.common.grpc.auth.ValidateRequest;
import com.currencyconverter.common.grpc.auth.ValidateResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Deadline;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Service
public class AuthGrpcClient {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceFutureStub authStub;

    public Mono<ValidateResponse> validateToken(String token) {
        return Mono.create(sink -> {
            ValidateRequest request = ValidateRequest.newBuilder()
                    .setToken(token)
                    .build();

            // Set 2 seconds deadline for auth
            ListenableFuture<ValidateResponse> future = authStub
                    .withDeadline(Deadline.after(2, TimeUnit.SECONDS))
                    .validateToken(request);

            Futures.addCallback(future, new FutureCallback<ValidateResponse>() {
                @Override
                public void onSuccess(ValidateResponse result) {
                    sink.success(result);
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
