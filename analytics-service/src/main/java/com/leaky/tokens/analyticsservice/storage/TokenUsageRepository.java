package com.leaky.tokens.analyticsservice.storage;

import java.util.UUID;

import org.springframework.data.cassandra.repository.CassandraRepository;

public interface TokenUsageRepository extends CassandraRepository<TokenUsageRecord, UUID> {
}
