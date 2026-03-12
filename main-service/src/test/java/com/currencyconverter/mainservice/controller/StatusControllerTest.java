package com.currencyconverter.mainservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatusController Tests")
class StatusControllerTest {

    private StatusController statusController;

    @BeforeEach
    void setUp() {
        statusController = new StatusController();
    }

    @Test
    @DisplayName("Should return status UP")
    void shouldReturnStatusUp() {
        // When
        Map<String, String> status = statusController.getStatus();

        // Then
        assertThat(status)
                .isNotNull()
                .containsEntry("status", "UP")
                .hasSize(1);
    }

    @Test
    @DisplayName("Should return status as a Map")
    void shouldReturnStatusAsMap() {
        // When
        Map<String, String> status = statusController.getStatus();

        // Then
        assertThat(status)
                .isInstanceOf(Map.class)
                .containsKey("status");
    }

    @Test
    @DisplayName("Should always return consistent status")
    void shouldAlwaysReturnConsistentStatus() {
        // When
        Map<String, String> status1 = statusController.getStatus();
        Map<String, String> status2 = statusController.getStatus();
        Map<String, String> status3 = statusController.getStatus();

        // Then
        assertThat(status1).isEqualTo(status2);
        assertThat(status2).isEqualTo(status3);
        assertThat(status1).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("Should not return null")
    void shouldNotReturnNull() {
        // When
        Map<String, String> status = statusController.getStatus();

        // Then
        assertThat(status).isNotNull();
    }

    @Test
    @DisplayName("Should not return empty map")
    void shouldNotReturnEmptyMap() {
        // When
        Map<String, String> status = statusController.getStatus();

        // Then
        assertThat(status).isNotEmpty();
    }
}
