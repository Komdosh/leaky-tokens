package com.leaky.tokens.tokenservice.quota;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OrgTokenPoolRepository extends JpaRepository<OrgTokenPool, UUID> {
    Optional<OrgTokenPool> findByOrgIdAndProvider(UUID orgId, String provider);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pool from OrgTokenPool pool where pool.orgId = :orgId and pool.provider = :provider")
    Optional<OrgTokenPool> findForUpdate(@Param("orgId") UUID orgId, @Param("provider") String provider);
}
