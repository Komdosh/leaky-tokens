package com.leaky.tokens.tokenservice.quota;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenQuotaService {
    private final TokenPoolRepository repository;

    public TokenQuotaService(TokenPoolRepository repository) {
        this.repository = repository;
    }

    public Optional<TokenPool> getQuota(UUID userId, String provider) {
        return repository.findByUserIdAndProvider(userId, provider);
    }

    @Transactional
    public TokenQuotaReservation reserve(UUID userId, String provider, long tokens) {
        TokenPool pool = repository.findForUpdate(userId, provider).orElse(null);
        if (pool == null) {
            return new TokenQuotaReservation(false, 0, 0);
        }
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
        pool.releaseTokens(tokens, Instant.now());
        repository.save(pool);
    }

    @Transactional
    public TokenPool addTokens(UUID userId, String provider, long tokens) {
        TokenPool pool = repository.findForUpdate(userId, provider).orElse(null);
        Instant now = Instant.now();
        if (pool == null) {
            pool = new TokenPool(UUID.randomUUID(), userId, provider, tokens, tokens, null, now, now);
        } else {
            pool.addTokens(tokens, now);
        }
        return repository.save(pool);
    }
}
