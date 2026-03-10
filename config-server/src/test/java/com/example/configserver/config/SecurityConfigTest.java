package com.example.configserver.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Unit Tests")
class SecurityConfigTest {

    @InjectMocks
    private SecurityConfig securityConfig;

    @Mock
    private HttpSecurity httpSecurity;

    @Test
    @DisplayName("Should create SecurityConfig instance")
    void shouldCreateSecurityConfigInstance() {
        SecurityConfig config = new SecurityConfig();
        assertNotNull(config);
    }

    @Test
    @DisplayName("SecurityConfig should have Configuration annotation")
    void shouldHaveConfigurationAnnotation() {
        assertTrue(SecurityConfig.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class));
    }

    @Test
    @DisplayName("SecurityConfig should have EnableWebSecurity annotation")
    void shouldHaveEnableWebSecurityAnnotation() {
        assertTrue(SecurityConfig.class.isAnnotationPresent(
                org.springframework.security.config.annotation.web.configuration.EnableWebSecurity.class));
    }

    @Test
    @DisplayName("securityFilterChain method should exist and return SecurityFilterChain")
    void securityFilterChainMethodShouldExist() throws NoSuchMethodException {
        var method = SecurityConfig.class.getDeclaredMethod("securityFilterChain", HttpSecurity.class);
        assertNotNull(method);
        assertEquals(SecurityFilterChain.class, method.getReturnType());
    }

    @Test
    @DisplayName("securityFilterChain method should have Bean annotation")
    void securityFilterChainMethodShouldHaveBeanAnnotation() throws NoSuchMethodException {
        var method = SecurityConfig.class.getDeclaredMethod("securityFilterChain", HttpSecurity.class);
        assertTrue(method.isAnnotationPresent(org.springframework.context.annotation.Bean.class));
    }

    @Test
    @DisplayName("SecurityConfig class should be public")
    void securityConfigClassShouldBePublic() {
        assertTrue(java.lang.reflect.Modifier.isPublic(SecurityConfig.class.getModifiers()));
    }

    @Test
    @DisplayName("securityFilterChain method should be public")
    void securityFilterChainMethodShouldBePublic() throws NoSuchMethodException {
        var method = SecurityConfig.class.getDeclaredMethod("securityFilterChain", HttpSecurity.class);
        assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    }

    @Test
    @DisplayName("SecurityConfig should have exactly one public method")
    void shouldHaveExactlyOnePublicMethod() {
        long publicMethodCount = java.util.Arrays.stream(SecurityConfig.class.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .count();
        assertEquals(1, publicMethodCount);
    }
}
