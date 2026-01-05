
package com.example.authservice.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import org.slf4j.MDC;

import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * WebFilter for adding request ID to all incoming requests.
 * <p>
 * This filter generates or extracts a correlation ID for request tracing:
 * - Checks for existing X-Request-ID header
 * - Generates new UUID if not present
 * - Adds request ID to Reactor Context for logging
 * - Adds request ID to response headers
 * - Propagates through MDC for traditional logging
 * </p>
 * <p>
 * Ordered as highest precedence to ensure request ID is available for all
 * subsequent filters.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter implements WebFilter {

  /**
   * 
   * Request ID header name for incoming requests.
   * 
   */
  public static final String REQUEST_ID_HEADER = "X-Request-ID";

  /**
   * 
   * Context key for storing request ID in
   * Reactor Context.
   * 
   */
  public static final String REQUEST_ID_KEY = "requestId";

  /**
   * 
   * MDC key for storing request ID in logging
   * context.
   * 
   */
  public static final String MDC_REQUEST_ID_KEY = "requestId";

  /**
   * 
   * Filters incoming requests to add request ID.
   *
   * @param exchange the server web exchange
   * @param chain    the web filter chain
   * @return a Mono that indicates when request processing is complete
   * 
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // Extract or generate request ID
    String requestId = extractOrGenerateRequestId(exchange);

    // Add request ID to response headers
    exchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);

    // Set MDC at the start
    MDC.put(MDC_REQUEST_ID_KEY, requestId);

    // Continue filter chain with request ID in Reactor Context and MDC
    return chain.filter(exchange)
        .contextWrite(Context.of(REQUEST_ID_KEY, requestId))
        .transformDeferredContextual((call, ctx) ->
            call.doOnEach(signal -> {
              // Propagate request ID to MDC for each signal on this thread
              if (!signal.isOnComplete() && !signal.isOnError()) {
                MDC.put(MDC_REQUEST_ID_KEY, ctx.getOrDefault(REQUEST_ID_KEY, ""));
              }
            })
        )
        .doFinally(signalType -> 
          // Clean up MDC when request completes
          MDC.remove(MDC_REQUEST_ID_KEY)
        );
  }

  /**
   * 
   * Extracts request ID from header or generates a new one.
   *
   * @param exchange the server web exchange
   * @return the request ID
   * 
   */
  private String extractOrGenerateRequestId(ServerWebExchange exchange) {
    String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);

    if (requestId == null || requestId.isEmpty()) {
      requestId = UUID.randomUUID().toString();
    }

    return requestId;
  }
}
