package com.example.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.kafka.enabled=false",
				"kafka.enabled=false"
		}
)
@ActiveProfiles("test")
class AuthServiceApplicationTests {

	@Configuration
	@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
	static class TestConfig {
	}

	@Test
	void contextLoads() {
		// Test to ensure the application context loads successfully
	}

	@Test
	void mainMethodStartsApplication() {
		AuthServiceApplication.main(new String[]{
				"--spring.profiles.active=test",
				"--spring.kafka.enabled=false",
				"--kafka.enabled=false"
		});
	}

}
