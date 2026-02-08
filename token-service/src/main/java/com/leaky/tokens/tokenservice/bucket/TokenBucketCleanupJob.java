package com.leaky.tokens.tokenservice.bucket;

import java.time.Duration;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenBucketCleanupJob {
    private static final Logger logger = LoggerFactory.getLogger(TokenBucketCleanupJob.class);

    private final TokenBucketStore store;
    private final TokenBucketProperties properties;

    @Scheduled(fixedDelayString = "${token.bucket.cleanup-interval:PT30M}")
    public void cleanup() {
        Duration ttl = properties.getEntryTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        if (store instanceof InMemoryTokenBucketStore inMemory) {
            int removed = inMemory.cleanup(Instant.now(), ttl);
            if (removed > 0) {
                logger.info("Cleaned up {} expired token bucket entries", removed);
            }
        }
    }
}
