package com.leaky.tokens.tokenservice.bucket;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "token.bucket", name = "store", havingValue = "redis")
public class RedisTokenBucketStore implements TokenBucketStore {
    private final RedisTemplate<String, TokenBucketState> redisTemplate;

    public RedisTokenBucketStore(RedisTemplate<String, TokenBucketState> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public TokenBucketState load(TokenBucketKey key, Instant now) {
        String redisKey = toRedisKey(key);
        TokenBucketState state = redisTemplate.opsForValue().get(redisKey);
        if (state == null) {
            state = new TokenBucketState(0L, now);
            redisTemplate.opsForValue().set(redisKey, state);
        }
        return state;
    }

    @Override
    public void save(TokenBucketKey key, TokenBucketState state) {
        redisTemplate.opsForValue().set(toRedisKey(key), state);
    }

    private String toRedisKey(TokenBucketKey key) {
        return "token-bucket:" + key.userId() + ":" + key.provider();
    }
}
