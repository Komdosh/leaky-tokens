package com.leaky.tokens.tokenservice.bucket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenBucketState {
    private long currentTokens;
    private Instant lastUpdated;
    private Instant windowStart;
    private long windowCount;

    public TokenBucketState(long currentTokens, Instant lastUpdated) {
        this.currentTokens = currentTokens;
        this.lastUpdated = lastUpdated;
    }

}
