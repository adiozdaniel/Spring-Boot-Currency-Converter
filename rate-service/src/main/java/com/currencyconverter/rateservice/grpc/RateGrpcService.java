package com.currencyconverter.rateservice.grpc;

import com.currencyconverter.common.grpc.rate.RateRequest;
import com.currencyconverter.common.grpc.rate.RateResponse;
import com.currencyconverter.common.grpc.rate.RateServiceGrpc;
import com.currencyconverter.rateservice.service.RateService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@GrpcService
public class RateGrpcService extends RateServiceGrpc.RateServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(RateGrpcService.class);
    private final RateService rateService;

    public RateGrpcService(RateService rateService) {
        this.rateService = rateService;
    }

    @Override
    public void getRate(RateRequest request, StreamObserver<RateResponse> responseObserver) {
        String from = request.getFrom().toUpperCase();
        String to = request.getTo().toUpperCase();

        rateService.fetchRate(from, to)
                .map(rateMap -> {
                    double rate = (double) rateMap.get("rate");
                    return RateResponse.newBuilder()
                            .setRate(rate)
                            .setFrom(from)
                            .setTo(to)
                            .build();
                })
                .onErrorResume(e -> {
                    logger.error("gRPC GetRate failed: {}", e.getMessage());
                    return Mono.error(Status.INTERNAL.withDescription(e.getMessage()).asException());
                })
                .subscribe(
                        responseObserver::onNext,
                        error -> responseObserver.onError(error instanceof Exception exception ? exception
                                : Status.INTERNAL.withDescription(error.toString()).asException()),
                        responseObserver::onCompleted);
    }
}
