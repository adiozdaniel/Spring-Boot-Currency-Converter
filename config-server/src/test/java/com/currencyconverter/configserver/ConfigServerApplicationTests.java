package com.currencyconverter.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.cloud.config.server.git.uri=https://github.com/spring-cloud-samples/config-repo",
    "spring.cloud.config.server.git.clone-on-start=false",
    "encrypt.key-store.location=classpath:/test.jks",
    "encrypt.key-store.password=testpassword",
    "encrypt.key-store.alias=testalias",
    "encrypt.key-store.secret=testsecret"
})
@ActiveProfiles("test")
class ConfigServerApplicationTests {

	@Test
	void contextLoads() {
	}

    @Test
    void mainMethodStartsApplication() {
        ConfigServerApplication.main(new String[]{"--server.port=0"});
    }
}
