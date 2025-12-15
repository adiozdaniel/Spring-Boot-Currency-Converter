package com.example.mainservice;

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
    @DisplayName("Should load application context")
    void contextLoads() {
        // Context loads successfully if this test passes
    }

    @Test
    @DisplayName("Should run main method without exceptions")
    void shouldRunMainMethod() {
        // Given & When & Then
        assertThatCode(() -> MainServiceApplication.main(new String[]{}))
                .doesNotThrowAnyException();
    }
}
