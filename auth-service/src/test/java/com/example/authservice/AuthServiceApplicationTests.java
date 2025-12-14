package com.example.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainMethodStartsApplication() {
		AuthServiceApplication.main(new String[]{
				"--spring.profiles.active=test"
		});
	}

}
