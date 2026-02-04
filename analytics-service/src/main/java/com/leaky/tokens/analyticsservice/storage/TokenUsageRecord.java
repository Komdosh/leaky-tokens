package com.leaky.tokens.analyticsservice.storage;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

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

    public TokenUsageRecord() {
    }

    public TokenUsageRecord(UUID id, String userId, String provider, long tokens, boolean allowed, Instant timestamp) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.tokens = tokens;
        this.allowed = allowed;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getTokens() {
        return tokens;
    }

    public void setTokens(long tokens) {
        this.tokens = tokens;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
