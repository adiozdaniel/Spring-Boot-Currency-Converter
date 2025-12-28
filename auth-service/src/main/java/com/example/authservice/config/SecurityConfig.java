package com.example.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuration class for web security.
 * <p>
 * This class enables web security and configures CORS, CSRF, session management,
 * and request authorization rules for the application.
 * </p>
 */
@Configuration
@EnableWebSecurity
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
     * @param http the {@link HttpSecurity} to configure.
     * @return the configured {@link SecurityFilterChain}.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .anyRequest().authenticated()
            );

        return http.build();
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
