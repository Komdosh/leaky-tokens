package com.leaky.tokens.authserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import com.leaky.tokens.authserver.domain.ApiKey;
import com.leaky.tokens.authserver.domain.Role;
import com.leaky.tokens.authserver.domain.UserAccount;
import com.leaky.tokens.authserver.dto.ApiKeyCreateRequest;
import com.leaky.tokens.authserver.dto.ApiKeyValidationResponse;
import com.leaky.tokens.authserver.metrics.AuthMetrics;
import com.leaky.tokens.authserver.repo.ApiKeyRepository;
import com.leaky.tokens.authserver.repo.UserAccountRepository;
import com.leaky.tokens.authserver.service.ApiKeyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ApiKeyServiceTest {
    @Test
    void createReturnsRawKey() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = new ApiKeyService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount(userId, "alice", "alice@example.com", "hashed");
        when(userAccountRepository.findById(eq(userId))).thenReturn(Optional.of(user));

        ApiKeyCreateRequest request = new ApiKeyCreateRequest();
        request.setUserId(userId.toString());
        request.setName("cli");

        var response = service.create(request);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getApiKey()).startsWith("leaky_" + userId + "_");

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    void validateReturnsRoles() throws Exception {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = new ApiKeyService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        String rawKey = "leaky_" + userId + "_raw";
        String hashed = hash(rawKey);

        ApiKey apiKey = new ApiKey(UUID.randomUUID(), userId, hashed, "cli", Instant.now(), null);
        when(apiKeyRepository.findByKeyValue(eq(hashed))).thenReturn(Optional.of(apiKey));

        UserAccount user = new UserAccount(userId, "alice", "alice@example.com", "hashed");
        user.addRole(new Role(UUID.randomUUID(), "USER", "Default user role"));
        when(userAccountRepository.findById(eq(userId))).thenReturn(Optional.of(user));

        ApiKeyValidationResponse response = service.validate(rawKey);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRoles()).containsExactly("USER");
    }

    @Test
    void validateRejectsExpiredKey() throws Exception {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = new ApiKeyService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        String rawKey = "leaky_" + userId + "_raw";
        String hashed = hash(rawKey);

        ApiKey apiKey = new ApiKey(
            UUID.randomUUID(),
            userId,
            hashed,
            "cli",
            Instant.now().minusSeconds(10),
            Instant.now().minusSeconds(5)
        );
        when(apiKeyRepository.findByKeyValue(eq(hashed))).thenReturn(Optional.of(apiKey));

        assertThatThrownBy(() -> service.validate(rawKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("api key expired");
    }

    private String hash(String rawKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }
}
