package com.leaky.tokens.tokenservice.quota;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "token_org_pools")
@NoArgsConstructor
@AllArgsConstructor
public class OrgTokenPool {
    @Id
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "total_tokens", nullable = false)
    private long totalTokens;

    @Column(name = "remaining_tokens", nullable = false)
    private long remainingTokens;

    @Column(name = "reset_time")
    private Instant resetTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void addTokens(long tokens, Instant now) {
        totalTokens += tokens;
        remainingTokens += tokens;
        updatedAt = now;
    }

    public void reserveTokens(long tokens, Instant now) {
        remainingTokens -= tokens;
        updatedAt = now;
    }

    public void releaseTokens(long tokens, Instant now) {
        remainingTokens = Math.min(totalTokens, remainingTokens + tokens);
        updatedAt = now;
    }

    public void capRemainingTokens(long cap, Instant now) {
        if (cap <= 0) {
            return;
        }
        if (remainingTokens > cap) {
            remainingTokens = cap;
            updatedAt = now;
        }
    }

    public void resetWindow(Instant now, java.time.Duration window) {
        remainingTokens = totalTokens;
        resetTime = now.plus(window);
        updatedAt = now;
    }

    public void ensureResetTime(Instant now, java.time.Duration window) {
        if (resetTime == null) {
            resetTime = now.plus(window);
            updatedAt = now;
        }
    }
}
