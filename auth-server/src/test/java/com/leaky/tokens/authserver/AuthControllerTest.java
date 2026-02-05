package com.leaky.tokens.authserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaky.tokens.authserver.dto.ApiKeyCreateRequest;
import com.leaky.tokens.authserver.dto.ApiKeyResponse;
import com.leaky.tokens.authserver.dto.ApiKeySummary;
import com.leaky.tokens.authserver.dto.ApiKeyValidationResponse;
import com.leaky.tokens.authserver.dto.AuthResponse;
import com.leaky.tokens.authserver.dto.LoginRequest;
import com.leaky.tokens.authserver.dto.RegisterRequest;
import com.leaky.tokens.authserver.metrics.AuthMetrics;
import com.leaky.tokens.authserver.service.ApiKeyService;
import com.leaky.tokens.authserver.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void registerReturnsCreatedOnSuccess() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        UUID userId = UUID.randomUUID();
        when(authService.register(any(RegisterRequest.class)))
            .thenReturn(new AuthResponse(userId, "alice", "token", List.of("USER")));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.roles[0]").value("USER"));

        verify(metrics).registerSuccess();
    }

    @Test
    void registerReturnsBadRequestOnInvalidInput() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        when(authService.register(any(RegisterRequest.class)))
            .thenThrow(new IllegalArgumentException("invalid input"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("invalid input"));

        verify(metrics).registerFailure("invalid");
    }

    @Test
    void loginReturnsUnauthorizedOnBadCredentials() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new IllegalArgumentException("invalid credentials"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("bad");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("invalid credentials"));

        verify(metrics).loginFailure("invalid");
    }

    @Test
    void loginReturnsOkOnSuccess() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        UUID userId = UUID.randomUUID();
        when(authService.login(any(LoginRequest.class)))
            .thenReturn(new AuthResponse(userId, "alice", "token", List.of("USER")));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.roles[0]").value("USER"));

        verify(metrics).loginSuccess();
    }

    @Test
    void createApiKeyReturnsCreated() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        UUID userId = UUID.randomUUID();
        when(apiKeyService.create(any(ApiKeyCreateRequest.class)))
            .thenReturn(new ApiKeyResponse(UUID.randomUUID(), userId, "cli", java.time.Instant.now(), null, "leaky_key"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        ApiKeyCreateRequest request = new ApiKeyCreateRequest();
        request.setUserId(userId.toString());
        request.setName("cli");

        mockMvc.perform(post("/api/v1/auth/api-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.apiKey").value("leaky_key"));
    }

    @Test
    void createApiKeyReturnsBadRequestOnInvalidInput() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        when(apiKeyService.create(any(ApiKeyCreateRequest.class)))
            .thenThrow(new IllegalArgumentException("invalid request"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        ApiKeyCreateRequest request = new ApiKeyCreateRequest();
        request.setUserId("bad-id");
        request.setName("cli");

        mockMvc.perform(post("/api/v1/auth/api-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("invalid request"));
    }

    @Test
    void listApiKeysRejectsMissingUserId() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        mockMvc.perform(get("/api/v1/auth/api-keys"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listApiKeysRejectsInvalidUserId() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        mockMvc.perform(get("/api/v1/auth/api-keys")
                .param("userId", "not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("invalid userId"));
    }

    @Test
    void listApiKeysReturnsItems() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        UUID userId = UUID.randomUUID();
        when(apiKeyService.list(eq(userId)))
            .thenReturn(List.of(new ApiKeySummary(UUID.randomUUID(), "cli", java.time.Instant.now(), null)));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        mockMvc.perform(get("/api/v1/auth/api-keys")
                .param("userId", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("cli"));
    }

    @Test
    void revokeApiKeyRejectsMissingUserId() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        mockMvc.perform(delete("/api/v1/auth/api-keys")
                .param("userId", " ")
                .param("apiKeyId", UUID.randomUUID().toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("userId is required"));
    }

    @Test
    void revokeApiKeyRejectsMissingApiKeyId() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        mockMvc.perform(delete("/api/v1/auth/api-keys")
                .param("userId", UUID.randomUUID().toString())
                .param("apiKeyId", " "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("apiKeyId is required"));
    }

    @Test
    void revokeApiKeyRejectsInvalidIds() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        mockMvc.perform(delete("/api/v1/auth/api-keys")
                .param("userId", "not-a-uuid")
            .param("apiKeyId", "also-bad"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid UUID string: not-a-uuid"));
    }

    @Test
    void validateApiKeyReturnsUnauthorizedWhenMissingHeader() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        when(apiKeyService.validate(eq(null))).thenThrow(new IllegalArgumentException("api key is required"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        mockMvc.perform(get("/api/v1/auth/api-keys/validate"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("api key is required"));
    }

    @Test
    void validateApiKeyReturnsUnauthorizedOnInvalidKey() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        when(apiKeyService.validate(eq("bad-key"))).thenThrow(new IllegalArgumentException("key invalid"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        mockMvc.perform(get("/api/v1/auth/api-keys/validate")
                .header("X-Api-Key", "bad-key"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("key invalid"));
    }

    @Test
    void validateApiKeyReturnsPayload() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);

        UUID userId = UUID.randomUUID();
        when(apiKeyService.validate(eq("good-key")))
            .thenReturn(new ApiKeyValidationResponse(userId, "cli", null, List.of("USER")));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, apiKeyService, metrics)).build();

        mockMvc.perform(get("/api/v1/auth/api-keys/validate")
                .header("X-Api-Key", "good-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.roles[0]").value("USER"));
    }
}
