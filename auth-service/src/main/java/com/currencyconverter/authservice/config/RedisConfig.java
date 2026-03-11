package com.currencyconverter.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

/**
 * Configuration class for Redis.
 * 
 * <p>
 * This class sets up the necessary beans for reactive Redis interactions,
 * including a {@link ReactiveRedisTemplate} with string serializers.
 * </p>
 */

@Configuration

public class RedisConfig {

  /**
   * Creates a {@link ReactiveRedisTemplate} bean for reactive Redis operations.
   * 
   * <p>
   * This template is configured to use {@link StringRedisSerializer} for keys,
   * values, hash keys, and hash values, ensuring consistent string-based
   * serialization across all Redis operations.
   * </p>
   *
   * @param connectionFactory the factory for creating reactive Redis connections.
   * @return a configured {@link ReactiveRedisTemplate} for string-based operations.       
   */

  @Bean
  @Primary
  public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {

    StringRedisSerializer serializer = new StringRedisSerializer();

    RedisSerializationContext<String, String> serializationContext = RedisSerializationContext
        .<String, String>newSerializationContext(serializer)
        .key(serializer)
        .value(serializer)
        .hashKey(serializer)
        .hashValue(serializer)
        .build();

    return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);

  }
}
