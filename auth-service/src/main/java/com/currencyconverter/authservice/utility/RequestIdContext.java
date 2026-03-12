package com.currencyconverter.authservice.utility;

import com.currencyconverter.authservice.filter.RequestIdFilter;

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

    private RequestIdContext() {
        throw new AssertionError("Cannot instantiate RequestIdContext");
    }
}
