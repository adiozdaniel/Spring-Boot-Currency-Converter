package com.example.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Configuration class for mapping Kafka topic properties from the application's
 * configuration file.
 * <p>
 * This class is annotated with {@link ConfigurationProperties} to bind
 * properties prefixed with "kafka.topics". The {@link RefreshScope} annotation
 * allows these properties to be refreshed dynamically without restarting the
 * application.
 * </p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties {

    private TopicConfig authEvents = new TopicConfig();
    private TopicConfig authLoginSuccess = new TopicConfig();
    private TopicConfig authLoginFailed = new TopicConfig();
    private TopicConfig authTokens = new TopicConfig();

    /**
     * Gets the configuration for the 'authEvents' topic.
     *
     * @return the 'authEvents' topic configuration.
     */
    public TopicConfig getAuthEvents() {
        return authEvents;
    }

    /**
     * Sets the configuration for the 'authEvents' topic.
     *
     * @param authEvents the 'authEvents' topic configuration.
     */
    public void setAuthEvents(TopicConfig authEvents) {
        this.authEvents = authEvents;
    }

    /**
     * Gets the configuration for the 'authLoginSuccess' topic.
     *
     * @return the 'authLoginSuccess' topic configuration.
     */
    public TopicConfig getAuthLoginSuccess() {
        return authLoginSuccess;
    }

    /**
     * Sets the configuration for the 'authLoginSuccess' topic.
     *
     * @param authLoginSuccess the 'authLoginSuccess' topic configuration.
     */
    public void setAuthLoginSuccess(TopicConfig authLoginSuccess) {
        this.authLoginSuccess = authLoginSuccess;
    }

    /**
     * Gets the configuration for the 'authLoginFailed' topic.
     *
     * @return the 'authLoginFailed' topic configuration.
     */
    public TopicConfig getAuthLoginFailed() {
        return authLoginFailed;
    }

    /**
     * Sets the configuration for the 'authLoginFailed' topic.
     *
     * @param authLoginFailed the 'authLoginFailed' topic configuration.
     */
    public void setAuthLoginFailed(TopicConfig authLoginFailed) {
        this.authLoginFailed = authLoginFailed;
    }

    /**
     * Gets the configuration for the 'authTokens' topic.
     *
     * @return the 'authTokens' topic configuration.
     */
    public TopicConfig getAuthTokens() {
        return authTokens;
    }

    /**
     * Sets the configuration for the 'authTokens' topic.
     *
     * @param authTokens the 'authTokens' topic configuration.
     */
    public void setAuthTokens(TopicConfig authTokens) {
        this.authTokens = authTokens;
    }

    /**
     * A static inner class representing the configuration for a single Kafka topic.
     */
    public static class TopicConfig {
        private String name;
        private int partitions = 6;
        private short replicationFactor = 3;
        private long retentionMs = 604800000L; // 7 days default

        /**
         * Gets the name of the topic.
         *
         * @return the topic name.
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name of the topic.
         *
         * @param name the topic name.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the number of partitions for the topic.
         *
         * @return the number of partitions.
         */
        public int getPartitions() {
            return partitions;
        }

        /**
         * Sets the number of partitions for the topic.
         *
         * @param partitions the number of partitions.
         */
        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        /**
         * Gets the replication factor for the topic.
         *
         * @return the replication factor.
         */
        public short getReplicationFactor() {
            return replicationFactor;
        }

        /**
         * Sets the replication factor for the topic.
         *
         * @param replicationFactor the replication factor.
         */
        public void setReplicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
        }

        /**
         * Gets the retention time in milliseconds for messages in the topic.
         *
         * @return the retention time in milliseconds.
         */
        public long getRetentionMs() {
            return retentionMs;
        }

        /**
         * Sets the retention time in milliseconds for messages in the topic.
         *
         * @param retentionMs the retention time in milliseconds.
         */
        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }
    }
}
