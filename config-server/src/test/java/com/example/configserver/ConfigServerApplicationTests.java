package com.example.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.cloud.config.server.git.uri=https://github.com/spring-cloud-samples/config-repo",
		"spring.cloud.config.server.git.clone-on-start=false",
		"spring.cloud.config.server.encrypt.enabled=false",
		"encrypt.key-store.location=",
		"encrypt.key-store.password=",
		"encrypt.key-store.alias=",
		"encrypt.key-store.secret="
})
class ConfigServerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainMethodStartsApplication() {
		ConfigServerApplication.main(new String[]{
				"--spring.cloud.config.server.git.uri=https://github.com/spring-cloud-samples/config-repo",
				"--spring.cloud.config.server.git.clone-on-start=false",
				"--spring.cloud.config.server.encrypt.enabled=false",
				"--encrypt.key-store.location=",
				"--spring.main.web-application-type=none",
				"--spring.main.allow-bean-definition-overriding=true"
		});
	}

}
