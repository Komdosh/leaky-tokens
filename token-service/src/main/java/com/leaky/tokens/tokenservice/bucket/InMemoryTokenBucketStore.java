package com.leaky.tokens.tokenservice.bucket;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

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

    public int cleanup(Instant now, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return 0;
        }
        int removed = 0;
        Iterator<Map.Entry<TokenBucketKey, TokenBucketState>> iterator = store.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TokenBucketKey, TokenBucketState> entry = iterator.next();
            TokenBucketState state = entry.getValue();
            Instant lastTouched = state.getLastUpdated();
            if (lastTouched == null) {
                lastTouched = state.getWindowStart();
            }
            if (lastTouched == null) {
                lastTouched = now;
            }
            if (lastTouched.plus(ttl).isBefore(now)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }
}
