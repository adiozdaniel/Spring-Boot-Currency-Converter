package com.example.configserver.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ServiceStatusLogger {

    private static final Logger logger = LoggerFactory.getLogger(ServiceStatusLogger.class);

    private final String applicationName;
    private final String serverPort;
    private final String activeProfiles;
    private long startupTimestamp;

    public ServiceStatusLogger(
            @Value("${spring.application.name}") String applicationName,
            @Value("${server.port}") String serverPort,
            @Value("${spring.profiles.active:default}") String activeProfiles) {
        this.applicationName = applicationName;
        this.serverPort = serverPort;
        this.activeProfiles = activeProfiles;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        this.startupTimestamp = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();

        logger.info("Event=STARTUP | Status=UP | Application={} | Port={} | Profiles={} | "
                        + "FreeMemoryMB={} | TotalMemoryMB={} | MaxMemoryMB={} | Processors={}",
                applicationName, serverPort, activeProfiles,
                runtime.freeMemory() / (1024 * 1024),
                runtime.totalMemory() / (1024 * 1024),
                runtime.maxMemory() / (1024 * 1024),
                runtime.availableProcessors());
    }

    @Scheduled(fixedRateString = "${service.heartbeat.interval-ms:60000}")
    public void heartbeat() {
        Runtime runtime = Runtime.getRuntime();
        long uptimeSeconds = (System.currentTimeMillis() - startupTimestamp) / 1000;

        logger.info("Event=HEARTBEAT | Status=UP | Application={} | UptimeSeconds={} | "
                        + "FreeMemoryMB={} | TotalMemoryMB={} | MaxMemoryMB={} | UsedMemoryMB={}",
                applicationName, uptimeSeconds,
                runtime.freeMemory() / (1024 * 1024),
                runtime.totalMemory() / (1024 * 1024),
                runtime.maxMemory() / (1024 * 1024),
                (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
    }

    long getStartupTimestamp() {
        return startupTimestamp;
    }

    @PreDestroy
    public void onShutdown() {
        long uptimeSeconds = (System.currentTimeMillis() - startupTimestamp) / 1000;

        logger.info("Event=SHUTDOWN | Status=DOWN | Application={} | UptimeSeconds={}",
                applicationName, uptimeSeconds);
    }
}
