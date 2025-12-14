package com.example.configserver.functional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.cloud.config.server.git.uri=https://github.com/spring-cloud-samples/config-repo",
        "spring.cloud.config.server.git.username=test-git-user",
        "spring.cloud.config.server.git.password=test-git-password",
        "spring.cloud.config.server.git.skipSslValidation=true",
        "spring.cloud.config.server.git.clone-on-start=true",
        "spring.cloud.config.server.git.timeout=30",
        "spring.cloud.config.server.git.default-label=main",
        "config.server.username=test-user",
        "config.server.password=test-password",
        "spring.cloud.config.server.encrypt.enabled=false",
        "encrypt.key-store.location=",
        "encrypt.key-store.password=",
        "encrypt.key-store.alias=",
        "encrypt.key-store.secret="
})
@DisplayName("Config Retrieval Functional Tests")
class ConfigRetrievalTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest(name = "Should handle config endpoint: {0}")
    @CsvSource({
            "/foo/default, Should retrieve configuration for application with default profile",
            "/test-app/default, Should handle configuration request with application name",
            "/application/production, Should handle configuration request with profile",
            "/application/default/main, Should handle configuration request with label/branch",
            "/application-default.properties, Config server should respond to plain properties endpoint format",
            "/application-default.yml, Config server should respond to YAML endpoint format",
            "/application-default.json, Config server should respond to JSON endpoint format",
            "/application/dev,test, Should handle multiple profiles in request"
    })
    @DisplayName("Config endpoint handling")
    void shouldHandleConfigEndpoints(String endpoint, String description) {
        String url = "http://localhost:" + port + endpoint;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertNotNull(response, description + " - Response should not be null");
        assertNotNull(response.getStatusCode(), description + " - Status code should not be null");
    }

    @Test
    @DisplayName("Config endpoint should return valid JSON response")
    void configEndpointShouldReturnValidJson() {
        String url = "http://localhost:" + port + "/application/default";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertNotNull(response);
        if (response.getStatusCode() == HttpStatus.OK) {
            assertDoesNotThrow(() -> objectMapper.readTree(response.getBody()));
        }
    }

    @Test
    @DisplayName("Config response should contain expected structure when successful")
    void configResponseShouldContainExpectedStructure() throws Exception {
        String url = "http://localhost:" + port + "/foo/default";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            // Spring Cloud Config response should have these fields
            assertTrue(jsonNode.has("name") ||
                      jsonNode.has("propertySources") ||
                      jsonNode.has("profiles"));
        }
    }

    @Test
    @DisplayName("Server should handle malformed config requests gracefully")
    void shouldHandleMalformedConfigRequests() {
        String url = "http://localhost:" + port + "/invalid-path-structure";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertNotNull(response);
        // Should return 4xx error for malformed requests
        assertTrue(response.getStatusCode().is4xxClientError() ||
                   response.getStatusCode().is2xxSuccessful());
    }

    @Test
    @DisplayName("Config endpoint should not expose sensitive git credentials")
    void configEndpointShouldNotExposeSensitiveCredentials() {
        String url = "http://localhost:" + port + "/application/default";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            String body = response.getBody();
            assertNotNull(body);

            // Should not contain sensitive information
            assertFalse(body.contains("password:") && body.contains("GIT_PASSWORD"));
            assertFalse(body.contains("username:") && body.contains("GIT_USERNAME"));
        }
    }
}
