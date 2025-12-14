package com.example.authservice.controller;

import com.example.authservice.dto.AuthRequest;
import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.RefreshTokenRequest;
import com.example.authservice.dto.RevokeTokenRequest;
import com.example.authservice.exception.InvalidApiKeyException;
import com.example.authservice.exception.RateLimitExceededException;
import com.example.authservice.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Should authenticate with valid API key")
    @WithMockUser
    void shouldAuthenticateWithValidApiKey() throws Exception {
        AuthRequest request = new AuthRequest("valid-api-key", "client123", "web");
        AuthResponse response = new AuthResponse("access-token", "refresh-token", "Bearer", 3600);

        when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                .thenReturn(response);

        mockMvc.perform(post("/auth/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Should reject request with missing API key")
    @WithMockUser
    void shouldRejectRequestWithMissingApiKey() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setClientId("client123");

        mockMvc.perform(post("/auth/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 for invalid API key")
    @WithMockUser
    void shouldReturn401ForInvalidApiKey() throws Exception {
        AuthRequest request = new AuthRequest("invalid-api-key", "client123", "web");

        when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                .thenThrow(new InvalidApiKeyException());

        mockMvc.perform(post("/auth/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 429 when rate limit exceeded")
    @WithMockUser
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        AuthRequest request = new AuthRequest("valid-api-key", "client123", "web");

        when(authenticationService.authenticate(any(AuthRequest.class), anyString()))
                .thenThrow(new RateLimitExceededException());

        mockMvc.perform(post("/auth/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    @WithMockUser
    void shouldRefreshTokenSuccessfully() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        AuthResponse response = new AuthResponse("new-access-token", "new-refresh-token", "Bearer", 3600);

        when(authenticationService.refreshToken(anyString())).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("Should revoke token successfully")
    @WithMockUser
    void shouldRevokeTokenSuccessfully() throws Exception {
        RevokeTokenRequest request = new RevokeTokenRequest("token-to-revoke");

        mockMvc.perform(post("/auth/revoke")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token revoked successfully"));
    }

    @Test
    @DisplayName("Should validate token successfully")
    @WithMockUser
    void shouldValidateTokenSuccessfully() throws Exception {
        when(authenticationService.validateToken(anyString()))
                .thenReturn(Map.of("valid", true, "subject", "client123"));

        mockMvc.perform(post("/auth/validate")
                        .with(csrf())
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }
}
