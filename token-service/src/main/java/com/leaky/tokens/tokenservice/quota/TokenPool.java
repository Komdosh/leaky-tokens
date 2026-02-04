package com.leaky.tokens.tokenservice.quota;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "token_pools")
public class TokenPool {
    @Id
    private UUID id;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "user_id")
    private UUID userId;

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

    protected TokenPool() {
    }

    public TokenPool(UUID id,
                     UUID userId,
                     String provider,
                     long totalTokens,
                     long remainingTokens,
                     Instant resetTime,
                     Instant createdAt,
                     Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.totalTokens = totalTokens;
        this.remainingTokens = remainingTokens;
        this.resetTime = resetTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public void resetWindow(Instant now, Duration window) {
        remainingTokens = totalTokens;
        resetTime = now.plus(window);
        updatedAt = now;
    }

    public void ensureResetTime(Instant now, Duration window) {
        if (resetTime == null) {
            resetTime = now.plus(window);
            updatedAt = now;
        }
    }
}
