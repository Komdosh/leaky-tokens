package com.leaky.tokens.tokenservice.quota;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenQuotaService {
    private final TokenPoolRepository repository;
    private final TokenQuotaProperties properties;

    public TokenQuotaService(TokenPoolRepository repository, TokenQuotaProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public Optional<TokenPool> getQuota(UUID userId, String provider, TokenTierProperties.TierConfig tier) {
        Optional<TokenPool> pool = repository.findByUserIdAndProvider(userId, provider);
        pool.ifPresent(existing -> applyResetIfNeeded(existing, tier, Instant.now()));
        return pool;
    }

    @Transactional
    public TokenQuotaReservation reserve(UUID userId, String provider, long tokens, TokenTierProperties.TierConfig tier) {
        TokenPool pool = repository.findForUpdate(userId, provider).orElse(null);
        if (pool == null) {
            return new TokenQuotaReservation(false, 0, 0);
        }
        applyResetIfNeeded(pool, tier, Instant.now());
        long effectiveRemaining = applyQuotaCap(pool, tier, Instant.now());
        if (effectiveRemaining < tokens) {
            return new TokenQuotaReservation(false, pool.getTotalTokens(), effectiveRemaining);
        }
        pool.reserveTokens(tokens, Instant.now());
        repository.save(pool);
        return new TokenQuotaReservation(true, pool.getTotalTokens(), applyQuotaCap(pool, tier, Instant.now()));
    }

    @Transactional
    public void release(UUID userId, String provider, long tokens, TokenTierProperties.TierConfig tier) {
        TokenPool pool = repository.findForUpdate(userId, provider).orElse(null);
        if (pool == null) {
            return;
        }
        applyResetIfNeeded(pool, tier, Instant.now());
        pool.releaseTokens(tokens, Instant.now());
        applyQuotaCap(pool, tier, Instant.now());
        repository.save(pool);
    }

    @Transactional
    public TokenPool addTokens(UUID userId, String provider, long tokens, TokenTierProperties.TierConfig tier) {
        TokenPool pool = repository.findForUpdate(userId, provider).orElse(null);
        Instant now = Instant.now();
        if (pool == null) {
            pool = new TokenPool(UUID.randomUUID(), userId, provider, tokens, tokens, nextResetTime(now), now, now);
        } else {
            applyResetIfNeeded(pool, tier, now);
            pool.addTokens(tokens, now);
            ensureResetTime(pool, now);
        }
        applyQuotaCap(pool, tier, now);
        return repository.save(pool);
    }

    private void applyResetIfNeeded(TokenPool pool, TokenTierProperties.TierConfig tier, Instant now) {
        if (!properties.isEnabled()) {
            return;
        }
        Duration window = properties.getWindow();
        if (window == null || window.isZero() || window.isNegative()) {
            return;
        }
        Instant resetTime = pool.getResetTime();
        if (resetTime == null) {
            pool.ensureResetTime(now, window);
            repository.save(pool);
            return;
        }
        if (!resetTime.isAfter(now)) {
            pool.resetWindow(now, window);
            applyQuotaCap(pool, tier, now);
            repository.save(pool);
        }
    }

    private long applyQuotaCap(TokenPool pool, TokenTierProperties.TierConfig tier, Instant now) {
        if (tier == null || tier.getQuotaMaxTokens() == null) {
            return pool.getRemainingTokens();
        }
        long cap = tier.getQuotaMaxTokens();
        if (cap <= 0) {
            return pool.getRemainingTokens();
        }
        pool.capRemainingTokens(cap, now);
        return pool.getRemainingTokens();
    }

    private void ensureResetTime(TokenPool pool, Instant now) {
        if (!properties.isEnabled()) {
            return;
        }
        Duration window = properties.getWindow();
        if (window == null || window.isZero() || window.isNegative()) {
            return;
        }
        pool.ensureResetTime(now, window);
    }

    private Instant nextResetTime(Instant now) {
        if (!properties.isEnabled()) {
            return null;
        }
        Duration window = properties.getWindow();
        if (window == null || window.isZero() || window.isNegative()) {
            return null;
        }
        return now.plus(window);
    }
}
