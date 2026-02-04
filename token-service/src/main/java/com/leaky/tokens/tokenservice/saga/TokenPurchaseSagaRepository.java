package com.leaky.tokens.tokenservice.saga;

import java.util.UUID;
import java.util.Optional;
import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenPurchaseSagaRepository extends JpaRepository<TokenPurchaseSaga, UUID> {
    Optional<TokenPurchaseSaga> findByIdempotencyKey(String idempotencyKey);

    List<TokenPurchaseSaga> findByStatusInAndUpdatedAtBefore(List<TokenPurchaseSagaStatus> statuses, Instant cutoff);
}
