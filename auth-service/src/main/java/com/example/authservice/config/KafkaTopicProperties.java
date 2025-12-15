package com.example.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties {

    private TopicConfig authEvents = new TopicConfig();
    private TopicConfig authLoginSuccess = new TopicConfig();
    private TopicConfig authLoginFailed = new TopicConfig();
    private TopicConfig authTokens = new TopicConfig();

    public TopicConfig getAuthEvents() {
        return authEvents;
    }

    public void setAuthEvents(TopicConfig authEvents) {
        this.authEvents = authEvents;
    }

    public TopicConfig getAuthLoginSuccess() {
        return authLoginSuccess;
    }

    public void setAuthLoginSuccess(TopicConfig authLoginSuccess) {
        this.authLoginSuccess = authLoginSuccess;
    }

    public TopicConfig getAuthLoginFailed() {
        return authLoginFailed;
    }

    public void setAuthLoginFailed(TopicConfig authLoginFailed) {
        this.authLoginFailed = authLoginFailed;
    }

    public TopicConfig getAuthTokens() {
        return authTokens;
    }

    public void setAuthTokens(TopicConfig authTokens) {
        this.authTokens = authTokens;
    }

    public static class TopicConfig {
        private String name;
        private int partitions = 6;
        private short replicationFactor = 3;
        private long retentionMs = 604800000L; // 7 days default

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public short getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
        }

        public long getRetentionMs() {
            return retentionMs;
        }

        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }
    }
}
