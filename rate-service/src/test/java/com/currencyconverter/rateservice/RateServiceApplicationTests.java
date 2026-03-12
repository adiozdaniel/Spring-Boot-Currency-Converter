package com.currencyconverter.rateservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("RateServiceApplication Tests")
class RateServiceApplicationTests {

	@Test
	void contextLoads() {
		// Test to ensure the Spring application context loads successfully
	}

	@Test
  @DisplayName("Should run main method without exceptions")
  void shouldRunMainMethod() {
    // Given & When & Then
    assertThatCode(() -> RateServiceApplication.main(new String[]{}))
		.doesNotThrowAnyException();
    }

}
