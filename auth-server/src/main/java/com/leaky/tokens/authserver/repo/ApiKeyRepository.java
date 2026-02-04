package com.leaky.tokens.authserver.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.leaky.tokens.authserver.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByUserId(UUID userId);

    Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId);

    Optional<ApiKey> findByKeyValue(String keyValue);
}
