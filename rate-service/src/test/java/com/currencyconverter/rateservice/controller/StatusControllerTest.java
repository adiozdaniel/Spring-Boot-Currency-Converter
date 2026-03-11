package com.currencyconverter.rateservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

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
        StepVerifier.create(statusController.getStatus())
                .assertNext(status -> {
                    assertThat(status).isNotNull();
                    assertThat(status.get("status")).isEqualTo("UP");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return status as a Map")
    void shouldReturnStatusAsMap() {
        // When
        StepVerifier.create(statusController.getStatus())
                .assertNext(status -> {
                    assertThat(status).isInstanceOf(Map.class);
                    assertThat(status).containsKey("status");
                })
                .verifyComplete();
    }
}
