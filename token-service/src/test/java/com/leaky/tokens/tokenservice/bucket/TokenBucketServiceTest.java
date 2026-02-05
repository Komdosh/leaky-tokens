package com.leaky.tokens.tokenservice.bucket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import com.leaky.tokens.tokenservice.events.TokenUsageEvent;
import com.leaky.tokens.tokenservice.events.TokenUsageEventFactory;
import com.leaky.tokens.tokenservice.events.TokenUsagePublisher;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import org.junit.jupiter.api.Test;

class TokenBucketServiceTest {
    @Test
    void consumesAndPersistsToStore() {
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setCapacity(10);
        properties.setLeakRatePerSecond(1.0);

        InMemoryTokenBucketStore store = new InMemoryTokenBucketStore();
        TokenUsagePublisher publisher = event -> { };
        TokenUsageEventFactory eventFactory = new TokenUsageEventFactory();
        TokenBucketService service = new TokenBucketService(properties, store, publisher, eventFactory);

        TokenBucketResult result = service.consume("user-1", "openai", 5);
        assertThat(result.isAllowed()).isTrue();

        TokenBucketState state = store.load(new TokenBucketKey("user-1", "openai"), Instant.now());
        assertThat(state.getCurrentTokens()).isEqualTo(5);
    }

    @Test
    void consumePublishesUsageEvent() {
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setCapacity(10);
        properties.setLeakRatePerSecond(1.0);

        InMemoryTokenBucketStore store = new InMemoryTokenBucketStore();
        AtomicReference<TokenUsageEvent> eventRef = new AtomicReference<>();
        TokenUsagePublisher publisher = eventRef::set;
        TokenUsageEventFactory eventFactory = new TokenUsageEventFactory();
        TokenBucketService service = new TokenBucketService(properties, store, publisher, eventFactory);

        TokenBucketResult result = service.consume("user-2", "openai", 3);

        assertThat(result.isAllowed()).isTrue();
        TokenUsageEvent event = eventRef.get();
        assertThat(event).isNotNull();
        assertThat(event.userId()).isEqualTo("user-2");
        assertThat(event.provider()).isEqualTo("openai");
        assertThat(event.tokens()).isEqualTo(3);
        assertThat(event.allowed()).isTrue();
    }

    @Test
    void consumeWithTierScalesCapacity() {
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setCapacity(10);
        properties.setLeakRatePerSecond(1.0);

        InMemoryTokenBucketStore store = new InMemoryTokenBucketStore();
        TokenUsagePublisher publisher = event -> { };
        TokenUsageEventFactory eventFactory = new TokenUsageEventFactory();
        TokenBucketService service = new TokenBucketService(properties, store, publisher, eventFactory);

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        tier.setBucketCapacityMultiplier(2.0);
        tier.setBucketLeakRateMultiplier(0.5);

        TokenBucketResult result = service.consume("user-3", "openai", 5, tier);

        assertThat(result.getCapacity()).isEqualTo(20);
        assertThat(result.getUsed()).isEqualTo(5);
    }

    @Test
    void consumeWithInvalidTierMultipliersFallsBackToBase() {
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setCapacity(10);
        properties.setLeakRatePerSecond(1.0);

        InMemoryTokenBucketStore store = new InMemoryTokenBucketStore();
        TokenUsagePublisher publisher = event -> { };
        TokenUsageEventFactory eventFactory = new TokenUsageEventFactory();
        TokenBucketService service = new TokenBucketService(properties, store, publisher, eventFactory);

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        tier.setBucketCapacityMultiplier(0.0);
        tier.setBucketLeakRateMultiplier(-1.0);

        TokenBucketResult result = service.consume("user-4", "openai", 5, tier);

        assertThat(result.getCapacity()).isEqualTo(10);
        assertThat(result.getUsed()).isEqualTo(5);
    }
}
