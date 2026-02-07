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
import java.util.List;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import com.leaky.tokens.authserver.domain.ApiKey;
import com.leaky.tokens.authserver.domain.Role;
import com.leaky.tokens.authserver.domain.UserAccount;
import com.leaky.tokens.authserver.dto.ApiKeyCreateRequest;
import com.leaky.tokens.authserver.dto.ApiKeySummary;
import com.leaky.tokens.authserver.dto.ApiKeyValidationResponse;
import com.leaky.tokens.authserver.metrics.AuthMetrics;
import com.leaky.tokens.authserver.repo.ApiKeyRepository;
import com.leaky.tokens.authserver.repo.UserAccountRepository;
import com.leaky.tokens.authserver.service.ApiKeyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

class ApiKeyServiceTest {
    @Test
    void createReturnsRawKey() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount(userId, "alice", "alice@example.com", "hashed");
        when(userAccountRepository.findById(eq(userId))).thenReturn(Optional.of(user));

        ApiKeyCreateRequest request = new ApiKeyCreateRequest();
        request.setUserId(userId.toString());
        request.setName("cli");

        var response = service.create(request);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.apiKey()).startsWith("leaky_" + userId + "_");

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    void createRejectsMissingUserId() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        ApiKeyCreateRequest request = new ApiKeyCreateRequest();
        request.setUserId(" ");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is required");

        verify(metrics).apiKeyCreateFailure("invalid");
    }

    @Test
    void createRejectsInvalidUserId() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        ApiKeyCreateRequest request = new ApiKeyCreateRequest();
        request.setUserId("not-a-uuid");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid userId");

        verify(metrics).apiKeyCreateFailure("invalid");
    }

    @Test
    void createRejectsMissingUser() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        when(userAccountRepository.findById(eq(userId))).thenReturn(Optional.empty());

        ApiKeyCreateRequest request = new ApiKeyCreateRequest();
        request.setUserId(userId.toString());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user not found");

        verify(metrics).apiKeyCreateFailure("invalid");
    }

    @Test
    void validateReturnsRoles() throws Exception {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        String rawKey = "leaky_" + userId + "_raw";
        String hashed = hash(rawKey);

        ApiKey apiKey = new ApiKey(UUID.randomUUID(), userId, hashed, "cli", Instant.now(), null);
        when(apiKeyRepository.findByKeyValue(eq(hashed))).thenReturn(Optional.of(apiKey));

        UserAccount user = new UserAccount(userId, "alice", "alice@example.com", "hashed");
        user.addRole(new Role(UUID.randomUUID(), "USER", "Default user role"));
        when(userAccountRepository.findById(eq(userId))).thenReturn(Optional.of(user));

        ApiKeyValidationResponse response = service.validate(rawKey);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.roles()).containsExactly("USER");
    }

    @Test
    void validateReturnsEmptyRolesWhenUserMissing() throws Exception {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        String rawKey = "leaky_" + userId + "_raw";
        String hashed = hash(rawKey);

        ApiKey apiKey = new ApiKey(UUID.randomUUID(), userId, hashed, "cli", Instant.now(), null);
        when(apiKeyRepository.findByKeyValue(eq(hashed))).thenReturn(Optional.of(apiKey));
        when(userAccountRepository.findById(eq(userId))).thenReturn(Optional.empty());

        ApiKeyValidationResponse response = service.validate(rawKey);

        assertThat(response.roles()).isEmpty();
    }

    @Test
    void validateRejectsExpiredKey() throws Exception {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

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

    @Test
    void validateRejectsInvalidKey() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        when(apiKeyRepository.findByKeyValue(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate("leaky_bad_key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid api key");

        verify(metrics).apiKeyValidateFailure("invalid");
    }

    @Test
    void validateRejectsMissingKey() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        assertThatThrownBy(() -> service.validate(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("api key is required");
    }

    @Test
    void listReturnsSummaries() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        ApiKey apiKey = new ApiKey(UUID.randomUUID(), userId, "hashed", "cli", Instant.now(), null);
        when(apiKeyRepository.findByUserId(eq(userId))).thenReturn(List.of(apiKey));

        List<ApiKeySummary> summaries = service.list(userId);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.getFirst().name()).isEqualTo("cli");
    }

    @Test
    void revokeDeletesKey() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        ApiKey apiKey = new ApiKey(keyId, userId, "hashed", "cli", Instant.now(), null);
        when(apiKeyRepository.findByIdAndUserId(eq(keyId), eq(userId))).thenReturn(Optional.of(apiKey));

        service.revoke(userId, keyId);

        verify(apiKeyRepository).delete(eq(apiKey));
    }

    @Test
    void revokeRejectsMissingKey() {
        ApiKeyRepository apiKeyRepository = Mockito.mock(ApiKeyRepository.class);
        UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);
        AuthMetrics metrics = Mockito.mock(AuthMetrics.class);
        ApiKeyService service = newService(apiKeyRepository, userAccountRepository, metrics);

        UUID userId = UUID.randomUUID();
        UUID keyId = UUID.randomUUID();
        when(apiKeyRepository.findByIdAndUserId(eq(keyId), eq(userId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(userId, keyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("api key not found");
    }

    private String hash(String rawKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }

    private ApiKeyService newService(ApiKeyRepository apiKeyRepository,
                                     UserAccountRepository userAccountRepository,
                                     AuthMetrics metrics) {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        ObjectProvider<StringRedisTemplate> redisProvider = beanFactory.getBeanProvider(StringRedisTemplate.class);
        return new ApiKeyService(
            apiKeyRepository,
            userAccountRepository,
            metrics,
            objectMapper,
            redisProvider,
            300,
            3600
        );
    }
}
