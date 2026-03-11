package com.currencyconverter.authservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * Configuration class for asynchronous processing in the application.
 * <p>
 * This class enables Spring's asynchronous method execution capability via the
 * {@link EnableAsync} annotation. It defines a custom thread pool executor
 * for handling Kafka-related asynchronous tasks.
 * </p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Creates a custom thread pool executor for Kafka-related asynchronous tasks.
     * <p>
     * This executor is configured with a specific core/max pool size, queue capacity,
     * and a custom rejected execution handler to manage task overflow.
     * </p>
     *
     * @param meterRegistry the registry for collecting and managing metrics.
     * @return a configured {@link Executor} for Kafka async tasks.
     */
    @Bean(name = "kafkaAsyncExecutor")
    public Executor kafkaAsyncExecutor(MeterRegistry meterRegistry) {
        Counter rejectedCounter = Counter.builder("auth.events.rejected")
                .description("Number of authentication events rejected due to queue overflow")
                .register(meterRegistry);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("kafka-async-");
        executor.setRejectedExecutionHandler(createRejectedExecutionHandler(rejectedCounter));
        executor.initialize();
        return executor;
    }

    /**
     * Creates a {@link RejectedExecutionHandler} that logs rejected tasks, increments a counter,
     * and attempts to run the task synchronously as a fallback.
     *
     * @param rejectedCounter a {@link Counter} to increment when a task is rejected.
     * @return a configured {@link RejectedExecutionHandler}.
     */
    private RejectedExecutionHandler createRejectedExecutionHandler(Counter rejectedCounter) {
        return (runnable, executor) -> {
            rejectedCounter.increment();
            logger.error("Kafka async task rejected - queue full. Task: {}. " +
                    "Consider increasing queue capacity or pool size. " +
                    "Active threads: {}, Pool size: {}, Queue size: {}",
                    runnable.getClass().getSimpleName(),
                    executor.getActiveCount(),
                    executor.getPoolSize(),
                    executor.getQueue().size());

            // Attempt to run synchronously as a fallback if caller thread is available
            if (!executor.isShutdown()) {
                try {
                    logger.warn("Attempting synchronous execution as fallback");
                    runnable.run();
                } catch (Exception e) {
                    logger.error("Fallback synchronous execution failed: {}", e.getMessage());
                }
            }
        };
    }
}
