package com.leaky.tokens.tokenservice.bucket;

import java.time.Instant;

import com.leaky.tokens.tokenservice.events.TokenUsageEventFactory;
import com.leaky.tokens.tokenservice.events.TokenUsagePublisher;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
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

    public TokenBucketResult consume(String userId,
                                     String provider,
                                     long tokens,
                                     TokenTierProperties.TierConfig tier) {
        TokenBucketProperties effective = applyTier(properties, tier);
        TokenBucketKey key = new TokenBucketKey(userId, provider);
        Instant now = Instant.now();
        TokenBucketState state = store.load(key, now);
        TokenBucketResult result = engine.tryConsume(state, effective, tokens, now);
        store.save(key, state);
        publisher.publish(eventFactory.build(userId, provider, tokens, result.isAllowed(), now));
        return result;
    }

    private TokenBucketProperties applyTier(TokenBucketProperties base, TokenTierProperties.TierConfig tier) {
        if (tier == null) {
            return base;
        }
        double capacityMultiplier = tier.getBucketCapacityMultiplier();
        double leakMultiplier = tier.getBucketLeakRateMultiplier();

        TokenBucketProperties effective = new TokenBucketProperties();
        effective.setStrategy(base.getStrategy());
        effective.setWindowSeconds(base.getWindowSeconds());
        effective.setCapacity(scaleLong(base.getCapacity(), capacityMultiplier));
        effective.setLeakRatePerSecond(scaleDouble(base.getLeakRatePerSecond(), leakMultiplier));
        return effective;
    }

    private long scaleLong(long value, double multiplier) {
        if (multiplier <= 0) {
            return value;
        }
        long scaled = Math.round(value * multiplier);
        return Math.max(1, scaled);
    }

    private double scaleDouble(double value, double multiplier) {
        if (multiplier <= 0) {
            return value;
        }
        double scaled = value * multiplier;
        return Math.max(0.0001, scaled);
    }
}
