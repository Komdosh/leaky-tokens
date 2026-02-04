package com.leaky.tokens.tokenservice.bucket;

import java.time.Instant;

public interface TokenBucketStore {
    TokenBucketState load(TokenBucketKey key, Instant now);

    void save(TokenBucketKey key, TokenBucketState state);
}
