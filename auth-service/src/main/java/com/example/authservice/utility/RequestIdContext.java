package com.example.authservice.utility;

import com.example.authservice.filter.RequestIdFilter;
import reactor.core.publisher.Mono;

/**
 * Utility class for accessing request ID from Reactor Context.
 * <p>
 * Provides convenient methods to retrieve the correlation ID
 * added by {@link RequestIdFilter}.
 * </p>
 */
public final class RequestIdContext {

    /**
     * Retrieves the request ID from Reactor Context.
     * <p>
     * Usage in reactive services/controllers:
     * <pre>
     * return RequestIdContext.getRequestId()
     *     .flatMap(requestId -> {
     *         logger.info("Processing request: {}", requestId);
     *         return someService.process();
     *     });
     * </pre>
     * </p>
     *
     * @return Mono containing the request ID, or empty Mono if not found
     */
    public static Mono<String> getRequestId() {
        return Mono.deferContextual(ctx ->
                Mono.justOrEmpty(ctx.getOrEmpty(RequestIdFilter.REQUEST_ID_KEY))
        );
    }

    /**
     * Retrieves the request ID from Reactor Context synchronously.
     * <p>
     * <b>Warning:</b> This should only be used in non-reactive code paths
     * or when you're certain the context is available.
     * </p>
     * <p>
     * Usage with doOnNext/doOnEach:
     * <pre>
     * return someService.process()
     *     .doOnNext(result -> {
     *         String requestId = RequestIdContext.getRequestIdOrDefault("UNKNOWN");
     *         logger.info("Request {} completed", requestId);
     *     });
     * </pre>
     * </p>
     *
     * @param defaultValue the default value if request ID is not found
     * @return the request ID or default value
     */
    public static String getRequestIdOrDefault(String defaultValue) {
        return Mono.deferContextual(ctx ->
                        Mono.just(ctx.getOrDefault(RequestIdFilter.REQUEST_ID_KEY, defaultValue))
                )
                .block();
    }

    private RequestIdContext() {
        throw new AssertionError("Cannot instantiate RequestIdContext");
    }
}
