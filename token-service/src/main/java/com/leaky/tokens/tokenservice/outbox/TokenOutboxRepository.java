package com.leaky.tokens.tokenservice.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TokenOutboxRepository extends JpaRepository<TokenOutboxEntry, UUID> {
    @Query("select e from TokenOutboxEntry e where e.publishedAt is null order by e.createdAt asc")
    List<TokenOutboxEntry> findUnpublished(Pageable pageable);
}
