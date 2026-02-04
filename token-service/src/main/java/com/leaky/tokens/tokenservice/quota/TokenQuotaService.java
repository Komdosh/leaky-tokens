package com.leaky.tokens.tokenservice.quota;

import java.time.Instant;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

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

    public Optional<TokenPool> getQuota(UUID userId, String provider) {
        Optional<TokenPool> pool = repository.findByUserIdAndProvider(userId, provider);
        pool.ifPresent(existing -> applyResetIfNeeded(existing, Instant.now()));
        return pool;
    }

    @Transactional
    public TokenQuotaReservation reserve(UUID userId, String provider, long tokens) {
        TokenPool pool = repository.findForUpdate(userId, provider).orElse(null);
        if (pool == null) {
            return new TokenQuotaReservation(false, 0, 0);
        }
        applyResetIfNeeded(pool, Instant.now());
        if (pool.getRemainingTokens() < tokens) {
            return new TokenQuotaReservation(false, pool.getTotalTokens(), pool.getRemainingTokens());
        }
        pool.reserveTokens(tokens, Instant.now());
        repository.save(pool);
        return new TokenQuotaReservation(true, pool.getTotalTokens(), pool.getRemainingTokens());
    }

    @Transactional
    public void release(UUID userId, String provider, long tokens) {
        TokenPool pool = repository.findForUpdate(userId, provider).orElse(null);
        if (pool == null) {
            return;
        }
        applyResetIfNeeded(pool, Instant.now());
        pool.releaseTokens(tokens, Instant.now());
        repository.save(pool);
    }

    @Transactional
    public TokenPool addTokens(UUID userId, String provider, long tokens) {
        TokenPool pool = repository.findForUpdate(userId, provider).orElse(null);
        Instant now = Instant.now();
        if (pool == null) {
            pool = new TokenPool(UUID.randomUUID(), userId, provider, tokens, tokens, nextResetTime(now), now, now);
        } else {
            applyResetIfNeeded(pool, now);
            pool.addTokens(tokens, now);
            ensureResetTime(pool, now);
        }
        return repository.save(pool);
    }

    private void applyResetIfNeeded(TokenPool pool, Instant now) {
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
            repository.save(pool);
        }
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
