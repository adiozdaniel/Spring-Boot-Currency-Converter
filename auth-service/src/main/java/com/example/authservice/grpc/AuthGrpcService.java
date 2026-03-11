package com.example.authservice.grpc;

import com.currencyconverter.common.grpc.auth.AuthGrpcRequest;
import com.currencyconverter.common.grpc.auth.AuthGrpcResponse;
import com.currencyconverter.common.grpc.auth.AuthServiceGrpc;
import com.currencyconverter.common.grpc.auth.ValidateRequest;
import com.currencyconverter.common.grpc.auth.ValidateResponse;
import com.example.authservice.constant.AuthConstants;
import com.example.authservice.dto.AuthRequest;
import com.example.authservice.service.AuthenticationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;

@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AuthGrpcService.class);
    private final AuthenticationService authenticationService;

    public AuthGrpcService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public void authenticate(AuthGrpcRequest request, StreamObserver<AuthGrpcResponse> responseObserver) {
        AuthRequest authRequest = new AuthRequest(request.getApiKey(), request.getClientId(), request.getClientType());
        
        authenticationService.authenticate(authRequest, "gRPC")
                .map(response -> AuthGrpcResponse.newBuilder()
                        .setToken(response.getAccessToken())
                        .setRefreshToken(response.getRefreshToken())
                        .setSuccess(true)
                        .setMessage("Authentication successful")
                        .build())
                .onErrorResume(e -> {
                    logger.error("gRPC Authentication failed: {}", e.getMessage());
                    return Mono.just(AuthGrpcResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Authentication failed: " + e.getMessage())
                            .build());
                })
                .subscribe(
                        responseObserver::onNext,
                        error -> responseObserver.onError(Status.INTERNAL.withDescription(error.getMessage()).asException()),
                        responseObserver::onCompleted
                );
    }

    @Override
    public void validateToken(ValidateRequest request, StreamObserver<ValidateResponse> responseObserver) {
        authenticationService.validateToken(request.getToken())
                .map(claimsMap -> {
                    boolean valid = (boolean) claimsMap.getOrDefault(AuthConstants.VALID, false);
                    String userId = (String) claimsMap.getOrDefault(AuthConstants.SUBJECT, "");
                    
                    // Roles could be a list in claims, let's handle gracefully
                    Object rolesObj = claimsMap.getOrDefault("roles", Collections.emptyList());
                    Iterable<String> roles = rolesObj instanceof Iterable ? (Iterable<String>) rolesObj : Collections.emptyList();

                    return ValidateResponse.newBuilder()
                            .setValid(valid)
                            .setUserId(userId)
                            .addAllRoles(roles)
                            .build();
                })
                .onErrorResume(e -> {
                    logger.warn("gRPC Token validation failed: {}", e.getMessage());
                    return Mono.just(ValidateResponse.newBuilder()
                            .setValid(false)
                            .build());
                })
                .subscribe(
                        responseObserver::onNext,
                        error -> responseObserver.onError(Status.INTERNAL.withDescription(error.getMessage()).asException()),
                        responseObserver::onCompleted
                );
    }
}
