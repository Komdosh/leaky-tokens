package com.leaky.tokens.authserver;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.leaky.tokens.authserver.dto.ApiKeyCreateRequest;
import com.leaky.tokens.authserver.dto.ApiKeyResponse;
import com.leaky.tokens.authserver.dto.ApiKeySummary;
import com.leaky.tokens.authserver.dto.ApiKeyValidationResponse;
import com.leaky.tokens.authserver.dto.AuthResponse;
import com.leaky.tokens.authserver.dto.ErrorResponse;
import com.leaky.tokens.authserver.dto.LoginRequest;
import com.leaky.tokens.authserver.dto.RegisterRequest;
import com.leaky.tokens.authserver.metrics.AuthMetrics;
import com.leaky.tokens.authserver.service.AuthService;
import com.leaky.tokens.authserver.service.ApiKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService authService;
    private final ApiKeyService apiKeyService;
    private final AuthMetrics metrics;

    public AuthController(AuthService authService, ApiKeyService apiKeyService, AuthMetrics metrics) {
        this.authService = authService;
        this.apiKeyService = apiKeyService;
        this.metrics = metrics;
    }

    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            metrics.registerSuccess();
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            metrics.registerFailure("invalid");
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), Instant.now()));
        }
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            metrics.loginSuccess();
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            metrics.loginFailure("invalid");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage(), Instant.now()));
        }
    }

    @PostMapping("/api/v1/auth/api-keys")
    @PreAuthorize("hasRole('ADMIN') or #request.userId == authentication.name")
    public ResponseEntity<?> createApiKey(@RequestBody ApiKeyCreateRequest request) {
        try {
            ApiKeyResponse response = apiKeyService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), Instant.now()));
        }
    }

    @GetMapping("/api/v1/auth/api-keys")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<?> listApiKeys(@RequestParam("userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("userId is required", Instant.now()));
        }
        try {
            UUID userUuid = UUID.fromString(userId.trim());
            List<ApiKeySummary> keys = apiKeyService.list(userUuid);
            return ResponseEntity.ok(keys);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid userId", Instant.now()));
        }
    }

    @DeleteMapping("/api/v1/auth/api-keys")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<?> revokeApiKey(@RequestParam("userId") String userId,
                                          @RequestParam("apiKeyId") String apiKeyId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("userId is required", Instant.now()));
        }
        if (apiKeyId == null || apiKeyId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("apiKeyId is required", Instant.now()));
        }
        try {
            UUID userUuid = UUID.fromString(userId.trim());
            UUID keyUuid = UUID.fromString(apiKeyId.trim());
            apiKeyService.revoke(userUuid, keyUuid);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), Instant.now()));
        }
    }

    @GetMapping("/api/v1/auth/api-keys/validate")
    public ResponseEntity<?> validateApiKey(@RequestHeader(value = "X-Api-Key", required = false) String apiKey) {
        try {
            ApiKeyValidationResponse response = apiKeyService.validate(apiKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage(), Instant.now()));
        }
    }
}
