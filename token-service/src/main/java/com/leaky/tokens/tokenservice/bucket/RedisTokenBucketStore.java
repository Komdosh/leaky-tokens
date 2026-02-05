package com.leaky.tokens.tokenservice.bucket;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "token.bucket", name = "store", havingValue = "redis")
public class RedisTokenBucketStore implements TokenBucketStore {
    private final RedisTemplate<String, TokenBucketState> redisTemplate;
    private final TokenBucketProperties properties;

    @Override
    public TokenBucketState load(TokenBucketKey key, Instant now) {
        String redisKey = toRedisKey(key);
        TokenBucketState state = redisTemplate.opsForValue().get(redisKey);
        if (state == null) {
            state = new TokenBucketState(0L, now);
            set(redisKey, state);
        }
        return state;
    }

    @Override
    public void save(TokenBucketKey key, TokenBucketState state) {
        set(toRedisKey(key), state);
    }

    private void set(String redisKey, TokenBucketState state) {
        java.time.Duration ttl = properties.getEntryTtl();
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            redisTemplate.opsForValue().set(redisKey, state, ttl);
        } else {
            redisTemplate.opsForValue().set(redisKey, state);
        }
    }

    private String toRedisKey(TokenBucketKey key) {
        return "token-bucket:" + key.userId() + ":" + key.provider();
    }
}
