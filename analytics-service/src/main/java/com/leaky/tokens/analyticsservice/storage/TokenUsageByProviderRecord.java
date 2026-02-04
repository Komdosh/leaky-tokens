package com.leaky.tokens.analyticsservice.storage;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("token_usage_by_provider")
public class TokenUsageByProviderRecord {
    @PrimaryKey
    private TokenUsageByProviderKey key;

    @Column("user_id")
    private String userId;

    @Column("tokens")
    private long tokens;

    @Column("allowed")
    private boolean allowed;

    public TokenUsageByProviderRecord() {
    }

    public TokenUsageByProviderRecord(TokenUsageByProviderKey key, String userId, long tokens, boolean allowed) {
        this.key = key;
        this.userId = userId;
        this.tokens = tokens;
        this.allowed = allowed;
    }

    public TokenUsageByProviderKey getKey() {
        return key;
    }

    public void setKey(TokenUsageByProviderKey key) {
        this.key = key;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
}
