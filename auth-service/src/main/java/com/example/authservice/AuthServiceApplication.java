package com.example.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Authentication Service application.
 * <p>
 * This class initializes and runs the Spring Boot application. It is annotated
 * with {@link SpringBootApplication}, which enables auto-configuration, component
 * scanning, and other Spring Boot features.
 * </p>
 */
@SpringBootApplication
public class AuthServiceApplication {

	/**
	 * The main method that starts the Authentication Service.
	 *
	 * @param args command-line arguments passed to the application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
