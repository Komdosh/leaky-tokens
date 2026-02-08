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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final ApiKeyService apiKeyService;
    private final AuthMetrics metrics;

    @PostMapping("/api/v1/auth/register")
    @Operation(
        summary = "Register a new user",
        responses = {
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
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
    @Operation(
        summary = "Login and obtain JWT",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
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
    @Operation(
        summary = "Create API key for a user",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "API key created"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
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
    @Operation(
        summary = "List API keys for a user",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Keys returned"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
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
    @Operation(
        summary = "Revoke API key",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "204", description = "Key revoked"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
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
    @Operation(
        summary = "Validate API key (gateway use)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Key valid"),
            @ApiResponse(responseCode = "401", description = "Key invalid", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public ResponseEntity<?> validateApiKey(
        @Parameter(description = "API key to validate", required = true)
        @RequestHeader(value = "X-Api-Key", required = false) String apiKey
    ) {
        try {
            ApiKeyValidationResponse response = apiKeyService.validate(apiKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage(), Instant.now()));
        }
    }
}
