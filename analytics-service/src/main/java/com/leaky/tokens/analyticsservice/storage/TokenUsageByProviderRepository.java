package com.leaky.tokens.analyticsservice.storage;

import java.util.List;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

public interface TokenUsageByProviderRepository extends CassandraRepository<TokenUsageByProviderRecord, TokenUsageByProviderKey> {
    @Query("SELECT * FROM token_usage_by_provider WHERE provider=?0 LIMIT ?1")
    List<TokenUsageByProviderRecord> findRecentByProvider(String provider, int limit);
}
