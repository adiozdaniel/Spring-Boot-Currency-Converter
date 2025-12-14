package com.example.configserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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
        "spring.cloud.config.server.git.clone-on-start=false",
        "management.endpoints.web.exposure.include=health,info,metrics,prometheus",
        "management.endpoint.health.show-details=always",
        "config.server.username=test-user",
        "config.server.password=test-password",
        "spring.cloud.config.server.encrypt.enabled=false",
        "encrypt.key-store.location=",
        "encrypt.key-store.password=",
        "encrypt.key-store.alias=",
        "encrypt.key-store.secret="
})
@DisplayName("Actuator Endpoints Tests")
class ActuatorEndpointsTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Health endpoint should be accessible")
    void healthEndpointShouldBeAccessible() {
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Health endpoint should return UP status")
    void healthEndpointShouldReturnUpStatus() throws Exception {
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertEquals("UP", jsonNode.get("status").asText());
    }

    @Test
    @DisplayName("Health endpoint should show detailed information")
    void healthEndpointShouldShowDetails() throws Exception {
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertTrue(jsonNode.has("status"));
    }

    @Test
    @DisplayName("Info endpoint should be accessible")
    void infoEndpointShouldBeAccessible() {
        String url = "http://localhost:" + port + "/actuator/info";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // Info endpoint returns 200 even if empty
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Actuator base endpoint should be accessible with auth")
    void actuatorBaseEndpointShouldBeAccessible() {
        String url = "http://localhost:" + port + "/actuator";
        ResponseEntity<String> response = restTemplate.withBasicAuth("test-user", "test-password")
                .getForEntity(url, String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().is3xxRedirection());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Actuator should expose health endpoint in links")
    void actuatorShouldExposeHealthEndpointInLinks() throws Exception {
        String url = "http://localhost:" + port + "/actuator";
        ResponseEntity<String> response = restTemplate.withBasicAuth("test-user", "test-password")
                .getForEntity(url, String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertTrue(jsonNode.has("_links"));

        JsonNode links = jsonNode.get("_links");
        assertTrue(links.has("health") || links.has("health-path"));
    }

    @Test
    @DisplayName("Actuator should expose info endpoint in links")
    void actuatorShouldExposeInfoEndpointInLinks() throws Exception {
        String url = "http://localhost:" + port + "/actuator";
        ResponseEntity<String> response = restTemplate.withBasicAuth("test-user", "test-password")
                .getForEntity(url, String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        assertTrue(jsonNode.has("_links"));

        JsonNode links = jsonNode.get("_links");
        assertTrue(links.has("info") || links.has("info-path"));
    }

    @Test
    @DisplayName("Health endpoint response should be valid JSON")
    void healthEndpointResponseShouldBeValidJson() {
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertDoesNotThrow(() -> objectMapper.readTree(response.getBody()));
    }

    @Test
    @DisplayName("Info endpoint response should be valid JSON")
    void infoEndpointResponseShouldBeValidJson() {
        String url = "http://localhost:" + port + "/actuator/info";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertDoesNotThrow(() -> objectMapper.readTree(response.getBody()));
    }

    @Test
    @DisplayName("Actuator endpoints should return proper content type")
    void actuatorEndpointsShouldReturnProperContentType() {
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getContentType().toString().contains("application"));
    }
}
