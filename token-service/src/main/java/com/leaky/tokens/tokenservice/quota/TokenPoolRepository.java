package com.leaky.tokens.tokenservice.quota;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TokenPoolRepository extends JpaRepository<TokenPool, UUID> {
    Optional<TokenPool> findByUserIdAndProvider(UUID userId, String provider);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pool from TokenPool pool where pool.userId = :userId and pool.provider = :provider")
    Optional<TokenPool> findForUpdate(@Param("userId") UUID userId, @Param("provider") String provider);
}
