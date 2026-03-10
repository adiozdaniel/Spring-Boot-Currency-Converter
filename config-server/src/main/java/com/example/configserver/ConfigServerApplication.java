package com.example.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import com.example.configserver.config.EnvironmentRepositoryConfig;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigServer
@Import(EnvironmentRepositoryConfig.class)
public class ConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigServerApplication.class, args);
	}

}
