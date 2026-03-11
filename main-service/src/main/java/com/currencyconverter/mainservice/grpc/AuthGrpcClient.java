package com.currencyconverter.mainservice.grpc;

import com.currencyconverter.common.grpc.auth.AuthServiceGrpc;
import com.currencyconverter.common.grpc.auth.ValidateRequest;
import com.currencyconverter.common.grpc.auth.ValidateResponse;
import io.grpc.Deadline;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Service
public class AuthGrpcClient {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    public Mono<ValidateResponse> validateToken(String token) {
        return Mono.fromCallable(() -> {
            ValidateRequest request = ValidateRequest.newBuilder()
                    .setToken(token)
                    .build();
            
            // Set 2 seconds deadline for auth
            return authStub.withDeadline(Deadline.after(2, TimeUnit.SECONDS))
                    .validateToken(request);
        });
    }
}
