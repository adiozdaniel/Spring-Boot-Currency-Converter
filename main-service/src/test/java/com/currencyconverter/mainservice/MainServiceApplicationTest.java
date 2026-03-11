package com.currencyconverter.mainservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MainServiceApplication Tests")
class MainServiceApplicationTest {

    @Test
    @DisplayName("Should load application context successfully")
    void contextLoads() {
        // Test to ensure the Spring application context loads successfully
    }

    @Test
    @DisplayName("Should run main method without exceptions")
    void shouldRunMainMethod() {
        // Given & When & Then
        assertThatCode(() -> MainServiceApplication.main(new String[]{}))
                .doesNotThrowAnyException();
    }
}
