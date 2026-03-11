package com.currencyconverter.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Configuration class for mapping API key properties from the application's
 * configuration file.
 * <p>
 * This class is annotated with {@link ConfigurationProperties} to bind
 * properties prefixed with "api-keys". The {@link RefreshScope} annotation
 * allows these properties to be refreshed dynamically without restarting the
 * application.
 * </p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "api-keys")
public class ApiKeyConfig {

    private String web;
    private String mobile;
    private String platform;

    /**
     * Gets the API key for the web client.
     *
     * @return the web client API key.
     */
    public String getWeb() {
        return web;
    }

    /**
     * Sets the API key for the web client.
     *
     * @param web the web client API key.
     */
    public void setWeb(String web) {
        this.web = web;
    }

    /**
     * Gets the API key for the mobile client.
     *
     * @return the mobile client API key.
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * Sets the API key for the mobile client.
     *
     * @param mobile the mobile client API key.
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    /**
     * Gets the API key for the platform client.
     *
     * @return the platform client API key.
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Sets the API key for the platform client.
     *
     * @param platform the platform client API key.
     */
    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
