package com.currencyconverter.mainservice.filter;

import com.currencyconverter.mainservice.grpc.AuthGrpcClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class GrpcAuthFilter implements WebFilter {

    private final AuthGrpcClient authGrpcClient;

    public GrpcAuthFilter(AuthGrpcClient authGrpcClient) {
        this.authGrpcClient = authGrpcClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Skip auth for health and public endpoints
        if (path.startsWith("/actuator") || path.startsWith("/api/v1/public")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        return authGrpcClient.validateToken(token)
                .flatMap(response -> {
                    if (response.getValid()) {
                        // Propagate user ID in exchange attributes
                        exchange.getAttributes().put("userId", response.getUserId());
                        return chain.filter(exchange);
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                })
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }
}
