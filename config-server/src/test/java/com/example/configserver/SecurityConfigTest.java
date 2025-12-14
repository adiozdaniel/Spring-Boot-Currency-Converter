package com.example.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "spring.cloud.config.server.git.uri=https://github.com/spring-cloud-samples/config-repo",
        "spring.cloud.config.server.git.clone-on-start=false",
        "spring.cloud.config.server.encrypt.enabled=false",
        "encrypt.key-store.location=",
        "encrypt.key-store.password=",
        "encrypt.key-store.alias=",
        "encrypt.key-store.secret=",
        "config.server.username=config-user",
        "config.server.password=changeme",
        "management.endpoints.web.exposure.include=health,info,env"
})
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void infoEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void otherActuatorEndpointsAreSecure() throws Exception {
        mvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "config-user", roles = "USER")
    void otherActuatorEndpointsAreAccessibleWithMockUser() throws Exception {
        mvc.perform(get("/actuator/env"))
                .andExpect(status().isOk());
    }

    @Test
    void otherActuatorEndpointsAreAccessibleWithBasicAuth() throws Exception {
        mvc.perform(get("/actuator/env")
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("config-user:changeme".getBytes())))
                .andExpect(status().isOk());
    }

    @Test
    void anyOtherRequestIsSecure() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "config-user", roles = "USER")
    void anyOtherRequestIsAccessibleWithAuth() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isNotFound());
    }
}
