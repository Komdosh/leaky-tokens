package com.leaky.tokens.analyticsservice.storage;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@Setter
@Getter
@PrimaryKeyClass
public class TokenUsageByProviderKey implements Serializable {
    @PrimaryKeyColumn(name = "provider", type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    @Schema(example = "openai")
    private String provider;

    @PrimaryKeyColumn(name = "timestamp", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING, ordinal = 1)
    @Schema(example = "2026-02-04T16:59:00Z")
    private Instant timestamp;

    public TokenUsageByProviderKey() {
    }

    public TokenUsageByProviderKey(String provider, Instant timestamp) {
        this.provider = provider;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TokenUsageByProviderKey that = (TokenUsageByProviderKey) o;
        return Objects.equals(provider, that.provider) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, timestamp);
    }
}
