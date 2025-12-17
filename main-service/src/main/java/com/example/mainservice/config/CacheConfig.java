package com.example.mainservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager() {
            @Override
            protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(String name) {
                if ("processedEvents".equals(name)) {
                    return processedEventsCaffeineBuilder().build();
                }
                return userSessionsCaffeineBuilder().build();
            }
        };
        cacheManager.setCacheNames(java.util.Arrays.asList("userSessions", "processedEvents"));
        return cacheManager;
    }

    private Caffeine<Object, Object> userSessionsCaffeineBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats();
    }

    private Caffeine<Object, Object> processedEventsCaffeineBuilder() {
        // Processed events cache for idempotency - shorter TTL
        return Caffeine.newBuilder()
                .initialCapacity(1000)
                .maximumSize(50000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }
}
