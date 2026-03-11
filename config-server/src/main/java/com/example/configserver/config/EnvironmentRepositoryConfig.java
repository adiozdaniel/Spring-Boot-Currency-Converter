package com.example.configserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepositoryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to enforce local directory configuration when spring.profiles.active=local.
 * This implementation uses @Value to programmatically check the active profile
 * and configure the appropriate repository source.
 */
@Configuration
public class EnvironmentRepositoryConfig {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    /**
     * EnvironmentRepository bean that checks the active profile
     * and configures the appropriate repository source.
     * When active profile is 'local', it uses the local filesystem.
     * Otherwise, it uses the Git-based repository.
     */
    @Bean
    public EnvironmentRepository localEnvironmentRepository(
            NativeEnvironmentRepositoryFactory nativeFactory,
            NativeEnvironmentProperties nativeProperties) {
        
        // Check if the active profile is 'local'
        if ("local".equals(activeProfile)) {
            // Configure for local filesystem when profile is 'local'
            String userDir = System.getProperty("user.dir");
            String configRepoPath = "file:" + userDir + "/../config-repo/";
            
            nativeProperties.setSearchLocations(new String[]{
                configRepoPath + "{application}",
                configRepoPath
            });
            
            return nativeFactory.build(nativeProperties);
        } else {
            // For non-local profiles, return null to let Spring use the default Git repository
            // The Git repository configuration is handled by application.properties
            return null;
        }
    }
}
