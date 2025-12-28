package com.example.authservice.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.serializer.JsonSerializer;

import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration class for setting up Kafka producers, topics, and security.
 * <p>
 * This class is conditionally enabled based on the {@code kafka.enabled} property.
 * It provides beans for {@link KafkaAdmin}, a reactive {@link KafkaSender}, and
 * defines application-specific Kafka topics.
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Value("${kafka.ssl.truststore-location:}")
    private String truststoreLocation;

    @Value("${kafka.ssl.truststore-password:}")
    private String truststorePassword;

    @Value("${kafka.ssl.keystore-location:}")
    private String keystoreLocation;

    @Value("${kafka.ssl.keystore-password:}")
    private String keystorePassword;

    @Value("${kafka.ssl.key-password:}")
    private String keyPassword;

    @Value("${kafka.sasl.mechanism:}")
    private String saslMechanism;

    @Value("${kafka.sasl.jaas-config:}")
    private String saslJaasConfig;

    // Topic configuration
    @Value("${kafka.topics.auth-login-success.name:auth.login.success}")
    private String authLoginSuccessTopicName;

    @Value("${kafka.topics.auth-login-success.partitions:6}")
    private int authLoginSuccessPartitions;

    @Value("${kafka.topics.auth-login-failed.name:auth.login.failed}")
    private String authLoginFailedTopicName;

    @Value("${kafka.topics.auth-login-failed.partitions:3}")
    private int authLoginFailedPartitions;

    @Value("${kafka.topics.auth-tokens.name:auth.tokens}")
    private String authTokensTopicName;

    @Value("${kafka.topics.auth-tokens.partitions:6}")
    private int authTokensPartitions;

    @Value("${kafka.topics.auth-dlq.name:auth.dlq}")
    private String authDlqTopicName;

    @Value("${kafka.topics.replication-factor:3}")
    private short replicationFactor;

    // Retry configuration
    @Value("${kafka.producer.retries:3}")
    private int producerRetries;

    @Value("${kafka.producer.retry-backoff-ms:1000}")
    private int retryBackoffMs;

    @Value("${kafka.producer.delivery-timeout-ms:120000}")
    private int deliveryTimeoutMs;

    @Value("${kafka.producer.request-timeout-ms:30000}")
    private int requestTimeoutMs;

    /**
     * Configures the {@link KafkaAdmin} bean for managing Kafka topics.
     *
     * @return a configured {@link KafkaAdmin} instance.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        addSecurityConfig(configs);
        return new KafkaAdmin(configs);
    }

    /**
     * Creates a reactive Kafka sender using reactor-kafka.
     * <p>
     * This sender is configured for at-least-once delivery semantics with an
     * idempotent producer. It includes settings for retries, performance
     * optimizations, and security.
     * </p>
     *
     * @return a configured {@link KafkaSender} instance for sending records.
     */
    @Bean
    public KafkaSender<String, Object> kafkaSender() {
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Delivery guarantees - at-least-once with idempotent producer
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Retry configuration with exponential backoff
        producerProps.put(ProducerConfig.RETRIES_CONFIG, producerRetries);
        producerProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoffMs);
        producerProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);
        producerProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);

        // Performance optimizations
        producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        producerProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        producerProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        producerProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Max in-flight requests for ordering guarantee with idempotence
        producerProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Add security configuration
        addSecurityConfig(producerProps);

        SenderOptions<String, Object> senderOptions = SenderOptions.create(producerProps);

        logger.info("Reactive Kafka sender configured with bootstrap servers: {}", bootstrapServers);
        return KafkaSender.create(senderOptions);
    }

    /**
     * Defines the topic for successful authentication login events.
     *
     * @return a {@link NewTopic} bean for the login success topic.
     */
    @Bean
    public NewTopic authLoginSuccessTopic() {
        return TopicBuilder.name(authLoginSuccessTopicName)
                .partitions(authLoginSuccessPartitions)
                .replicas(replicationFactor)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Defines the topic for failed authentication login events.
     *
     * @return a {@link NewTopic} bean for the login failed topic.
     */
    @Bean
    public NewTopic authLoginFailedTopic() {
        return TopicBuilder.name(authLoginFailedTopicName)
                .partitions(authLoginFailedPartitions)
                .replicas(replicationFactor)
                .config("retention.ms", "2592000000") // 30 days for security analysis
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Defines the topic for token-related events (e.g., generation, refresh, revoke).
     *
     * @return a {@link NewTopic} bean for the tokens topic.
     */
    @Bean
    public NewTopic authTokensTopic() {
        return TopicBuilder.name(authTokensTopicName)
                .partitions(authTokensPartitions)
                .replicas(replicationFactor)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Defines the Dead Letter Queue (DLQ) topic for authentication events that
     * failed to be processed.
     *
     * @return a {@link NewTopic} bean for the authentication DLQ topic.
     */
    @Bean
    public NewTopic authDlqTopic() {
        return TopicBuilder.name(authDlqTopicName)
                .partitions(1)
                .replicas(replicationFactor)
                .config("retention.ms", "2592000000") // 30 days for DLQ
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Adds security-related properties to the Kafka configuration map.
     * <p>
     * This method configures SSL and/or SASL based on the application properties.
     * </p>
     *
     * @param configs the configuration map to which security properties will be added.
     */
    private void addSecurityConfig(Map<String, Object> configs) {
        configs.put("security.protocol", securityProtocol);

        if ("SSL".equals(securityProtocol) || "SASL_SSL".equals(securityProtocol)) {
            if (truststoreLocation != null && !truststoreLocation.isEmpty()) {
                configs.put("ssl.truststore.location", truststoreLocation);
                configs.put("ssl.truststore.password", truststorePassword);
            }
            if (keystoreLocation != null && !keystoreLocation.isEmpty()) {
                configs.put("ssl.keystore.location", keystoreLocation);
                configs.put("ssl.keystore.password", keystorePassword);
                configs.put("ssl.key.password", keyPassword);
            }
        }

        if ("SASL_SSL".equals(securityProtocol) || "SASL_PLAINTEXT".equals(securityProtocol)) {
            if (saslMechanism != null && !saslMechanism.isEmpty()) {
                configs.put("sasl.mechanism", saslMechanism);
                configs.put("sasl.jaas.config", saslJaasConfig);
            }
        }
    }
}
