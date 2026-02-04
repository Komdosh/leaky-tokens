package com.leaky.tokens.authserver.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.leaky.tokens.authserver.domain.ApiKey;
import com.leaky.tokens.authserver.domain.Role;
import com.leaky.tokens.authserver.domain.UserAccount;
import com.leaky.tokens.authserver.dto.ApiKeyCreateRequest;
import com.leaky.tokens.authserver.dto.ApiKeyResponse;
import com.leaky.tokens.authserver.dto.ApiKeySummary;
import com.leaky.tokens.authserver.dto.ApiKeyValidationResponse;
import com.leaky.tokens.authserver.metrics.AuthMetrics;
import com.leaky.tokens.authserver.repo.ApiKeyRepository;
import com.leaky.tokens.authserver.repo.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthMetrics metrics;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         UserAccountRepository userAccountRepository,
                         AuthMetrics metrics) {
        this.apiKeyRepository = apiKeyRepository;
        this.userAccountRepository = userAccountRepository;
        this.metrics = metrics;
    }

    public ApiKeyResponse create(ApiKeyCreateRequest request) {
        try {
            UUID userId = parseUserId(request.getUserId());
            UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

            String name = request.getName() == null ? null : request.getName().trim();
            String rawKey = generateRawKey(user.getId());
            String hashed = hash(rawKey);
            Instant now = Instant.now();

            ApiKey apiKey = new ApiKey(UUID.randomUUID(), user.getId(), hashed, name, now, request.getExpiresAt());
            apiKeyRepository.save(apiKey);

            metrics.apiKeyCreateSuccess();
            return new ApiKeyResponse(apiKey.getId(), apiKey.getUserId(), apiKey.getName(),
                apiKey.getCreatedAt(), apiKey.getExpiresAt(), rawKey);
        } catch (IllegalArgumentException ex) {
            metrics.apiKeyCreateFailure("invalid");
            throw ex;
        }
    }

    public List<ApiKeySummary> list(UUID userId) {
        return apiKeyRepository.findByUserId(userId).stream()
            .map(key -> new ApiKeySummary(key.getId(), key.getName(), key.getCreatedAt(), key.getExpiresAt()))
            .toList();
    }

    public void revoke(UUID userId, UUID keyId) {
        ApiKey apiKey = apiKeyRepository.findByIdAndUserId(keyId, userId)
            .orElseThrow(() -> new IllegalArgumentException("api key not found"));
        apiKeyRepository.delete(apiKey);
    }

    public ApiKeyValidationResponse validate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            metrics.apiKeyValidateFailure("missing");
            throw new IllegalArgumentException("api key is required");
        }
        String hashed = hash(rawKey.trim());
        ApiKey apiKey = apiKeyRepository.findByKeyValue(hashed)
            .orElseThrow(() -> {
                metrics.apiKeyValidateFailure("invalid");
                return new IllegalArgumentException("invalid api key");
            });
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
            metrics.apiKeyValidateFailure("expired");
            throw new IllegalArgumentException("api key expired");
        }
        List<String> roles = userAccountRepository.findById(apiKey.getUserId())
            .map(user -> user.getRoles().stream().map(Role::getName).sorted().toList())
            .orElseGet(List::of);
        metrics.apiKeyValidateSuccess();
        return new ApiKeyValidationResponse(apiKey.getUserId(), apiKey.getName(), apiKey.getExpiresAt(), roles);
    }

    private UUID parseUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid userId");
        }
    }

    private String generateRawKey(UUID userId) {
        byte[] random = new byte[24];
        secureRandom.nextBytes(random);
        return "leaky_" + userId + "_" + HexFormat.of().formatHex(random);
    }

    private String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("hashing algorithm missing", ex);
        }
    }
}
