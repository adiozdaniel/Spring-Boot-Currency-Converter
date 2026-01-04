package com.example.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Configuration class for web security.
 * <p>
 * This class enables web security and configures CORS, CSRF, session management,
 * and request authorization rules for the application.
 * </p>
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity 
public class SecurityConfig {

    private final CorsConfig corsConfig;

    /**
     * Constructs a new {@link SecurityConfig} with the specified CORS configuration.
     *
     * @param corsConfig the CORS configuration properties.
     */
    public SecurityConfig(CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
    }

    /**
     * Configures the security filter chain.
     * <p>
     * This method defines the security rules for HTTP requests, including CORS,
     * CSRF, session management, and authorization rules.
     * </p>
     *
     * @param http the {@link ServerHttpSecurity} to configure.
     * @return the configured {@link SecurityWebFilterChain}.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers("/actuator/**").authenticated()
                        .anyExchange().authenticated())
                .build();
    }

    /**
     * Creates a {@link CorsConfigurationSource} bean based on the properties
     * defined in {@link CorsConfig}.
     *
     * @return a configured {@link CorsConfigurationSource}.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (corsConfig.getAllowedOrigins() != null) {
            configuration.setAllowedOrigins(corsConfig.getAllowedOrigins());
        }
        if (corsConfig.getAllowedMethods() != null) {
            configuration.setAllowedMethods(corsConfig.getAllowedMethods());
        }
        if (corsConfig.getAllowedHeaders() != null) {
            configuration.setAllowedHeaders(corsConfig.getAllowedHeaders());
        }
        configuration.setAllowCredentials(corsConfig.isAllowCredentials());
        configuration.setMaxAge(corsConfig.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
