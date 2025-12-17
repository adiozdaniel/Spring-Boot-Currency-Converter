package com.example.mainservice.config;

import com.example.mainservice.event.AuthEvent;
import com.example.mainservice.event.TokenEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafka
@ConditionalOnProperty(value = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConsumerConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id:main-service-consumer-group}")
    private String groupId;

    @Value("${kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${kafka.consumer.max-poll-records:500}")
    private Integer maxPollRecords;

    @Value("${kafka.consumer.enable-auto-commit:false}")
    private Boolean enableAutoCommit;

    @Value("${kafka.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Value("${kafka.consumer.concurrency:3}")
    private Integer concurrency;

    @Value("${kafka.topics.auth-dlq}")
    private String authDlqTopic;

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.mainservice.event,com.example.authservice.event");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        // Security configuration
        if (!"PLAINTEXT".equals(securityProtocol)) {
            props.put("security.protocol", securityProtocol);
        }

        return props;
    }

    @Bean
    public ConsumerFactory<String, AuthEvent> authEventConsumerFactory() {
        Map<String, Object> props = baseConsumerConfig();
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AuthEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConsumerFactory<String, TokenEvent> tokenEventConsumerFactory() {
        Map<String, Object> props = baseConsumerConfig();
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TokenEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuthEvent> authEventKafkaListenerContainerFactory(
            ConsumerFactory<String, AuthEvent> authEventConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, AuthEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(authEventConsumerFactory);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.RECORD);

        // Error handler with retry and DLQ
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    log.error("Failed to process auth event after retries. Sending to DLQ: {}",
                            consumerRecord.value(), exception);
                    // Send to DLQ
                    String key = consumerRecord.key() != null ? consumerRecord.key().toString() : null;
                    kafkaTemplate.send(authDlqTopic, key, consumerRecord.value());
                },
                new FixedBackOff(1000L, 3L) // 3 retries with 1 second delay
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TokenEvent> tokenEventKafkaListenerContainerFactory(
            ConsumerFactory<String, TokenEvent> tokenEventConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, TokenEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(tokenEventConsumerFactory);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.RECORD);

        // Error handler with retry and DLQ
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    log.error("Failed to process token event after retries. Sending to DLQ: {}",
                            consumerRecord.value(), exception);
                    // Send to DLQ
                    String key = consumerRecord.key() != null ? consumerRecord.key().toString() : null;
                    kafkaTemplate.send(authDlqTopic, key, consumerRecord.value());
                },
                new FixedBackOff(1000L, 3L) // 3 retries with 1 second delay
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
