package com.example.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "kafkaAsyncExecutor")
    public Executor kafkaAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("kafka-async-");
        executor.setRejectedExecutionHandler((r, e) -> {
            // Log rejected tasks but don't block the main thread
            org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                    .warn("Kafka async task rejected, queue full");
        });
        executor.initialize();
        return executor;
    }
}
