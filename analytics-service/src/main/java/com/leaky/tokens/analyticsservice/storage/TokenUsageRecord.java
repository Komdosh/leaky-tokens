package com.leaky.tokens.analyticsservice.storage;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Table("token_usage_events")
public class TokenUsageRecord {
    @PrimaryKey
    private UUID id;

    @Column("user_id")
    private String userId;

    @Column("provider")
    private String provider;

    @Column("tokens")
    private long tokens;

    @Column("allowed")
    private boolean allowed;

    @Column("timestamp")
    private Instant timestamp;
}
