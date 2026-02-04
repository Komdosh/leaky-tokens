package com.leaky.tokens.tokenservice.bucket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.leaky.tokens.tokenservice.events.TokenUsageEventFactory;
import com.leaky.tokens.tokenservice.events.TokenUsagePublisher;
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
}
