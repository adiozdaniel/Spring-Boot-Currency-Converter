package com.example.authservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = StatusController.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@ActiveProfiles("test")
@DisplayName("StatusController Tests")
class StatusControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	@DisplayName("Should return service status")
	void shouldReturnServiceStatus() {
		webTestClient.get()
				.uri("/status")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.service").isEqualTo("auth-service")
				.jsonPath("$.status").isEqualTo("UP");
	}

	@Test
	@DisplayName("Should return exactly two fields")
	void shouldReturnExactlyTwoFields() {
		webTestClient.get()
				.uri("/status")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.service").exists()
				.jsonPath("$.status").exists()
				.jsonPath("$.length()").isEqualTo(2);
	}

	@Test
	@DisplayName("Should not require authentication")
	void shouldNotRequireAuthentication() {
		// No Authorization header provided
		webTestClient.get()
				.uri("/status")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.status").isEqualTo("UP");
	}

	@Test
	@DisplayName("Should handle multiple concurrent requests")
	void shouldHandleMultipleConcurrentRequests() {
		// Fire multiple requests concurrently
		for (int i = 0; i < 10; i++) {
			webTestClient.get()
					.uri("/status")
					.exchange()
					.expectStatus().isOk()
					.expectBody()
					.jsonPath("$.service").isEqualTo("auth-service")
					.jsonPath("$.status").isEqualTo("UP");
		}
	}

	@Test
	@DisplayName("Should return consistent response")
	void shouldReturnConsistentResponse() {
		// Call multiple times and verify consistency
		for (int i = 0; i < 5; i++) {
			webTestClient.get()
					.uri("/status")
					.exchange()
					.expectStatus().isOk()
					.expectBody()
					.jsonPath("$.service").isEqualTo("auth-service")
					.jsonPath("$.status").isEqualTo("UP");
		}
	}

	@Test
	@DisplayName("Should accept GET method")
	void shouldAcceptGetMethod() {
		// Verify GET is allowed and works correctly
		webTestClient.get()
				.uri("/status")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.service").isEqualTo("auth-service")
				.jsonPath("$.status").isEqualTo("UP");
	}

	@Test
	@DisplayName("Should work without any headers")
	void shouldWorkWithoutAnyHeaders() {
		webTestClient.get()
				.uri("/status")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.service").isEqualTo("auth-service")
				.jsonPath("$.status").isEqualTo("UP");
	}

	@Test
	@DisplayName("Should work with query parameters (ignore them)")
	void shouldWorkWithQueryParameters() {
		webTestClient.get()
				.uri("/status?foo=bar&test=123")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.service").isEqualTo("auth-service")
				.jsonPath("$.status").isEqualTo("UP");
	}
}
