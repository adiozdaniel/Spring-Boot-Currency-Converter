package com.example.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A simple controller for checking the operational status of the service.
 * <p>
 * This controller provides a basic {@code /status} endpoint to confirm that the
 * service is running.
 * </p>
 */
@RestController
public class StatusController {

    /**
     * Returns the current status of the service.
     *
     * @return a {@link Map} containing the service name and its status.
     */
    @GetMapping("/v1/status")
    public Map<String, String> status() {
        return Map.of(
                "service", "auth-service",
                "status", "UP"
        );
    }
}
