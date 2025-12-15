package com.example.authservice.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

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
    @Value("${kafka.topics.auth-events.name:auth.events}")
    private String authEventsTopicName;

    @Value("${kafka.topics.auth-events.partitions:6}")
    private int authEventsPartitions;

    @Value("${kafka.topics.auth-events.replication-factor:3}")
    private short authEventsReplicationFactor;

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

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        addSecurityConfig(configs);
        return new KafkaAdmin(configs);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Performance optimizations
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        addSecurityConfig(configProps);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Topic definitions
    @Bean
    public NewTopic authEventsTopic() {
        return TopicBuilder.name(authEventsTopicName)
                .partitions(authEventsPartitions)
                .replicas(authEventsReplicationFactor)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic authLoginSuccessTopic() {
        return TopicBuilder.name(authLoginSuccessTopicName)
                .partitions(authLoginSuccessPartitions)
                .replicas(authEventsReplicationFactor)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic authLoginFailedTopic() {
        return TopicBuilder.name(authLoginFailedTopicName)
                .partitions(authLoginFailedPartitions)
                .replicas(authEventsReplicationFactor)
                .config("retention.ms", "2592000000") // 30 days for security analysis
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic authTokensTopic() {
        return TopicBuilder.name(authTokensTopicName)
                .partitions(authTokensPartitions)
                .replicas(authEventsReplicationFactor)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

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
