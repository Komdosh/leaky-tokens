package com.leaky.tokens.tokenservice.bucket;

import java.time.Instant;

import com.leaky.tokens.tokenservice.events.TokenUsageEventFactory;
import com.leaky.tokens.tokenservice.events.TokenUsagePublisher;
import org.springframework.stereotype.Service;

@Service
public class TokenBucketService {
    private final TokenBucketProperties properties;
    private final TokenBucketStore store;
    private final TokenUsagePublisher publisher;
    private final TokenUsageEventFactory eventFactory;
    private final TokenBucketEngine engine = new TokenBucketEngine();

    public TokenBucketService(TokenBucketProperties properties,
                              TokenBucketStore store,
                              TokenUsagePublisher publisher,
                              TokenUsageEventFactory eventFactory) {
        this.properties = properties;
        this.store = store;
        this.publisher = publisher;
        this.eventFactory = eventFactory;
    }

    public TokenBucketResult consume(String userId, String provider, long tokens) {
        TokenBucketKey key = new TokenBucketKey(userId, provider);
        Instant now = Instant.now();
        TokenBucketState state = store.load(key, now);
        TokenBucketResult result = engine.tryConsume(state, properties, tokens, now);
        store.save(key, state);
        publisher.publish(eventFactory.build(userId, provider, tokens, result.isAllowed(), now));
        return result;
    }
}
