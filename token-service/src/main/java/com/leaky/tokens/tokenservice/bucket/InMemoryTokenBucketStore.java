package com.leaky.tokens.tokenservice.bucket;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(TokenBucketStore.class)
public class InMemoryTokenBucketStore implements TokenBucketStore {
    private final Map<TokenBucketKey, TokenBucketState> store = new ConcurrentHashMap<>();

    @Override
    public TokenBucketState load(TokenBucketKey key, Instant now) {
        return store.computeIfAbsent(key, ignored -> new TokenBucketState(0L, now));
    }

    @Override
    public void save(TokenBucketKey key, TokenBucketState state) {
        store.put(key, state);
    }
}
