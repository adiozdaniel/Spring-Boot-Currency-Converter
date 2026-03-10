package com.example.configserver.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServiceStatusLogger Unit Tests")
class ServiceStatusLoggerTest {

    private ServiceStatusLogger serviceStatusLogger;

    @BeforeEach
    void setUp() {
        serviceStatusLogger = new ServiceStatusLogger(
                "ms-baze-config-server", "8888", "production");
    }

    @Test
    @DisplayName("Should create ServiceStatusLogger instance")
    void shouldCreateInstance() {
        assertNotNull(serviceStatusLogger);
    }

    @Test
    @DisplayName("Should have Component annotation")
    void shouldHaveComponentAnnotation() {
        assertTrue(ServiceStatusLogger.class.isAnnotationPresent(
                org.springframework.stereotype.Component.class));
    }

    @Test
    @DisplayName("onApplicationReady should execute without error")
    void onApplicationReadyShouldExecuteWithoutError() {
        assertDoesNotThrow(() -> serviceStatusLogger.onApplicationReady());
    }

    @Test
    @DisplayName("onApplicationReady method should have EventListener annotation")
    void onApplicationReadyShouldHaveEventListenerAnnotation() throws NoSuchMethodException {
        Method method = ServiceStatusLogger.class.getDeclaredMethod("onApplicationReady");
        var annotation = method.getAnnotation(org.springframework.context.event.EventListener.class);
        assertNotNull(annotation);
        assertEquals(ApplicationReadyEvent.class, annotation.value()[0]);
    }

    @Test
    @DisplayName("heartbeat should execute without error")
    void heartbeatShouldExecuteWithoutError() {
        serviceStatusLogger.onApplicationReady();
        assertDoesNotThrow(() -> serviceStatusLogger.heartbeat());
    }

    @Test
    @DisplayName("heartbeat method should have Scheduled annotation")
    void heartbeatShouldHaveScheduledAnnotation() throws NoSuchMethodException {
        Method method = ServiceStatusLogger.class.getDeclaredMethod("heartbeat");
        Scheduled annotation = method.getAnnotation(Scheduled.class);
        assertNotNull(annotation);
        assertEquals("${service.heartbeat.interval-ms:60000}", annotation.fixedRateString());
    }

    @Test
    @DisplayName("onShutdown should execute without error")
    void onShutdownShouldExecuteWithoutError() {
        serviceStatusLogger.onApplicationReady();
        assertDoesNotThrow(() -> serviceStatusLogger.onShutdown());
    }

    @Test
    @DisplayName("onShutdown method should have PreDestroy annotation")
    void onShutdownShouldHavePreDestroyAnnotation() throws NoSuchMethodException {
        Method method = ServiceStatusLogger.class.getDeclaredMethod("onShutdown");
        assertTrue(method.isAnnotationPresent(jakarta.annotation.PreDestroy.class));
    }

    @Test
    @DisplayName("onApplicationReady should set startup timestamp")
    void onApplicationReadyShouldSetStartupTimestamp() {
        assertEquals(0, serviceStatusLogger.getStartupTimestamp());
        serviceStatusLogger.onApplicationReady();
        assertTrue(serviceStatusLogger.getStartupTimestamp() > 0);
    }

    @Test
    @DisplayName("Should have exactly three public methods")
    void shouldHaveExactlyThreePublicMethods() {
        long publicMethodCount = java.util.Arrays.stream(
                        ServiceStatusLogger.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .count();
        assertEquals(3, publicMethodCount);
    }
}
