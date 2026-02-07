package com.leaky.tokens.authserver.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthMetrics metrics;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final long cacheTtlSeconds;
    private final long blacklistTtlSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         UserAccountRepository userAccountRepository,
                         AuthMetrics metrics,
                         ObjectMapper objectMapper,
                         ObjectProvider<StringRedisTemplate> redisProvider,
                         @Value("${auth.api-key.cache-ttl-seconds:300}") long cacheTtlSeconds,
                         @Value("${auth.api-key.blacklist-ttl-seconds:3600}") long blacklistTtlSeconds) {
        this.apiKeyRepository = apiKeyRepository;
        this.userAccountRepository = userAccountRepository;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisProvider.getIfAvailable();
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.blacklistTtlSeconds = blacklistTtlSeconds;
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

            cacheApiKey(apiKey, user.getRoles().stream().map(Role::getName).sorted().toList());
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
        blacklistKey(apiKey);
    }

    public ApiKeyValidationResponse validate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            metrics.apiKeyValidateFailure("missing");
            throw new IllegalArgumentException("api key is required");
        }
        String hashed = hash(rawKey.trim());
        if (isBlacklisted(hashed)) {
            metrics.apiKeyValidateFailure("revoked");
            throw new IllegalArgumentException("invalid api key");
        }
        ApiKeyValidationResponse cached = getCachedValidation(hashed);
        if (cached != null) {
            metrics.apiKeyValidateSuccess();
            return cached;
        }
        ApiKey apiKey = apiKeyRepository.findByKeyValue(hashed)
            .orElseThrow(() -> {
                metrics.apiKeyValidateFailure("invalid");
                return new IllegalArgumentException("invalid api key");
            });
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
            metrics.apiKeyValidateFailure("expired");
            evictCache(hashed);
            throw new IllegalArgumentException("api key expired");
        }
        List<String> roles = userAccountRepository.findById(apiKey.getUserId())
            .map(user -> user.getRoles().stream().map(Role::getName).sorted().toList())
            .orElseGet(List::of);
        metrics.apiKeyValidateSuccess();
        ApiKeyValidationResponse response =
            new ApiKeyValidationResponse(apiKey.getUserId(), apiKey.getName(), apiKey.getExpiresAt(), roles);
        cacheValidation(hashed, response);
        return response;
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

    private void cacheApiKey(ApiKey apiKey, List<String> roles) {
        if (redisTemplate == null) {
            return;
        }
        ApiKeyValidationResponse response =
            new ApiKeyValidationResponse(apiKey.getUserId(), apiKey.getName(), apiKey.getExpiresAt(), roles);
        cacheValidation(apiKey.getKeyValue(), response);
    }

    private void cacheValidation(String hashedKey, ApiKeyValidationResponse response) {
        if (redisTemplate == null) {
            return;
        }
        Duration ttl = cacheTtl(response.expiresAt());
        if (ttl == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey(hashedKey), payload, ttl);
        } catch (JacksonException ex) {
            // Cache is best-effort; ignore serialization errors.
        }
    }

    private ApiKeyValidationResponse getCachedValidation(String hashedKey) {
        if (redisTemplate == null) {
            return null;
        }
        String cached = redisTemplate.opsForValue().get(cacheKey(hashedKey));
        if (cached == null || cached.isBlank()) {
            return null;
        }
        try {
            ApiKeyValidationResponse response = objectMapper.readValue(cached, ApiKeyValidationResponse.class);
            if (response.expiresAt() != null && response.expiresAt().isBefore(Instant.now())) {
                evictCache(hashedKey);
                return null;
            }
            return response;
        } catch (JacksonException ex) {
            evictCache(hashedKey);
            return null;
        }
    }

    private void evictCache(String hashedKey) {
        if (redisTemplate == null) {
            return;
        }
        redisTemplate.delete(cacheKey(hashedKey));
    }

    private void blacklistKey(ApiKey apiKey) {
        if (redisTemplate == null) {
            return;
        }
        String hashed = apiKey.getKeyValue();
        redisTemplate.delete(cacheKey(hashed));
        Duration ttl = blacklistTtl(apiKey.getExpiresAt());
        if (ttl != null) {
            redisTemplate.opsForValue().set(blacklistKey(hashed), "1", ttl);
        }
    }

    private boolean isBlacklisted(String hashedKey) {
        if (redisTemplate == null) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(blacklistKey(hashedKey));
        return exists != null && exists;
    }

    private Duration cacheTtl(Instant expiresAt) {
        if (cacheTtlSeconds <= 0) {
            return null;
        }
        Duration ttl = Duration.ofSeconds(cacheTtlSeconds);
        if (expiresAt == null) {
            return ttl;
        }
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) {
            return null;
        }
        return remaining.compareTo(ttl) < 0 ? remaining : ttl;
    }

    private Duration blacklistTtl(Instant expiresAt) {
        if (blacklistTtlSeconds <= 0) {
            return null;
        }
        Duration ttl = Duration.ofSeconds(blacklistTtlSeconds);
        if (expiresAt == null) {
            return ttl;
        }
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) {
            return ttl;
        }
        return remaining.compareTo(ttl) < 0 ? remaining : ttl;
    }

    private String cacheKey(String hashedKey) {
        return "auth:api-key:cache:" + hashedKey;
    }

    private String blacklistKey(String hashedKey) {
        return "auth:api-key:blacklist:" + hashedKey;
    }
}
