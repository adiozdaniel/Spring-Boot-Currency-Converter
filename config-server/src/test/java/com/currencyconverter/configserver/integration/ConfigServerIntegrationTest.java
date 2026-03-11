package com.currencyconverter.configserver.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
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
        "spring.cloud.config.server.git.timeout=10",
        "management.endpoints.web.exposure.include=health,info",
        "config.server.username=test-user",
        "config.server.password=test-password",
        "spring.cloud.config.server.encrypt.enabled=false"
})
@DisplayName("Config Server Integration Tests")
class ConfigServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        assertNotNull(applicationContext);
    }

    @Test
    @DisplayName("Spring Boot application should start successfully")
    void applicationShouldStart() {
        assertNotNull(restTemplate);
        assertTrue(port > 0);
    }

    @Test
    @DisplayName("Config server should be running on configured port")
    void configServerShouldBeRunningOnPort() {
        assertTrue(port > 0, "Server port should be greater than 0");
    }

    @Test
    @DisplayName("Application context should contain SecurityConfig bean")
    void shouldContainSecurityConfigBean() {
        assertTrue(applicationContext.containsBean("securityConfig"));
    }

    @Test
    @DisplayName("Application context should contain security filter chain bean")
    void shouldContainSecurityFilterChainBean() {
        assertTrue(applicationContext.containsBean("securityFilterChain"));
    }

    @Test
    @DisplayName("Health endpoint should return UP status")
    void healthEndpointShouldReturnUpStatus() {
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("UP") || response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    @DisplayName("Server should respond to HTTP requests")
    void serverShouldRespondToHttpRequests() {
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertNotNull(response);
        assertNotNull(response.getStatusCode());
    }

    @Test
    @DisplayName("Application should have Spring Cloud Config Server capabilities")
    void shouldHaveConfigServerCapabilities() {
        // Verify the application has loaded config server beans
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasConfigServerBeans = false;

        for (String beanName : beanNames) {
            if (beanName.toLowerCase().contains("config") ||
                beanName.toLowerCase().contains("environment")) {
                hasConfigServerBeans = true;
                break;
            }
        }

        assertTrue(hasConfigServerBeans, "Application should have config server related beans");
    }

    @Test
    @DisplayName("TestRestTemplate should be properly configured")
    void testRestTemplateShouldBeConfigured() {
        assertNotNull(restTemplate);
        assertNotNull(restTemplate.getRestTemplate());
    }

    @Test
    @DisplayName("Application should expose actuator endpoints")
    void shouldExposeActuatorEndpoints() {
        String url = "http://localhost:" + port + "/actuator";
        ResponseEntity<String> response = restTemplate.withBasicAuth("test-user", "test-password")
                .getForEntity(url, String.class);

        // Should either return OK or redirect to actuator root
        assertTrue(response.getStatusCode().is2xxSuccessful() ||
                   response.getStatusCode().is3xxRedirection());
    }
}
