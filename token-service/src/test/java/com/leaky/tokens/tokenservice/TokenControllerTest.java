package com.leaky.tokens.tokenservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import com.leaky.tokens.tokenservice.bucket.TokenBucketProperties;
import com.leaky.tokens.tokenservice.bucket.TokenBucketResult;
import com.leaky.tokens.tokenservice.bucket.TokenBucketService;
import com.leaky.tokens.tokenservice.events.TokenUsageEventFactory;
import com.leaky.tokens.tokenservice.events.TokenUsagePublisher;
import com.leaky.tokens.tokenservice.metrics.TokenServiceMetrics;
import com.leaky.tokens.tokenservice.provider.ProviderCallService;
import com.leaky.tokens.tokenservice.provider.ProviderRequest;
import com.leaky.tokens.tokenservice.provider.ProviderResponse;
import com.leaky.tokens.tokenservice.quota.TokenQuotaReservation;
import com.leaky.tokens.tokenservice.quota.TokenQuotaService;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import com.leaky.tokens.tokenservice.tier.TokenTierResolver;
import com.leaky.tokens.tokenservice.web.RateLimitHeadersFilter;
import com.leaky.tokens.tokenservice.web.SecurityHeadersFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TokenControllerTest {
    @Test
    void consumeReturnsOkWhenAllowed() throws Exception {
        Instant now = Instant.parse("2026-02-03T10:00:00Z");
        TokenBucketResult allowed = TokenBucketResult.allowed(1000, 100, 0L, now);
        TokenQuotaService quotaService = mock(TokenQuotaService.class);
        TokenTierResolver tierResolver = mock(TokenTierResolver.class);
        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        when(tierResolver.resolveTier()).thenReturn(tier);
        when(quotaService.reserve(
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "openai",
            100,
            tier
        )).thenReturn(new TokenQuotaReservation(true, 1000, 900));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new TokenController(new StubTokenBucketService(allowed),
                new StubProviderCallService(),
                quotaService,
                new TokenServiceMetrics(new SimpleMeterRegistry()),
                tierResolver)
        ).addFilters(new RateLimitHeadersFilter(), new SecurityHeadersFilter()).build();

        mockMvc.perform(
                post("/api/v1/tokens/consume")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":100,\"prompt\":\"hi\"}")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("X-RateLimit-Limit", "1000"))
            .andExpect(header().string("X-RateLimit-Remaining", "900"))
            .andExpect(header().string("X-RateLimit-Used", "100"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(jsonPath("$.allowed").value(true))
            .andExpect(jsonPath("$.capacity").value(1000))
            .andExpect(jsonPath("$.used").value(100))
            .andExpect(jsonPath("$.remaining").value(900))
            .andExpect(jsonPath("$.providerResponse.message").value("ok"));
    }

    @Test
    void consumeReturns429WhenDenied() throws Exception {
        Instant now = Instant.parse("2026-02-03T10:00:00Z");
        TokenBucketResult denied = TokenBucketResult.denied(1000, 1000, 5L, now);
        TokenQuotaService quotaService = mock(TokenQuotaService.class);
        TokenTierResolver tierResolver = mock(TokenTierResolver.class);
        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        when(tierResolver.resolveTier()).thenReturn(tier);
        when(quotaService.reserve(
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "openai",
            1,
            tier
        )).thenReturn(new TokenQuotaReservation(true, 10, 9));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new TokenController(new StubTokenBucketService(denied),
                new StubProviderCallService(),
                quotaService,
                new TokenServiceMetrics(new SimpleMeterRegistry()),
                tierResolver)
        ).addFilters(new RateLimitHeadersFilter(), new SecurityHeadersFilter()).build();

        mockMvc.perform(
                post("/api/v1/tokens/consume")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":1}")
            )
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("X-RateLimit-Limit", "1000"))
            .andExpect(header().string("X-RateLimit-Remaining", "0"))
            .andExpect(header().string("X-RateLimit-Used", "1000"))
            .andExpect(jsonPath("$.allowed").value(false))
            .andExpect(jsonPath("$.waitSeconds").value(5));
    }

    @Test
    void consumeValidatesRequest() throws Exception {
        TokenQuotaService quotaService = mock(TokenQuotaService.class);
        TokenTierResolver tierResolver = mock(TokenTierResolver.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new TokenController(
                new StubTokenBucketService(TokenBucketResult.allowed(1, 0, 0L, Instant.now())),
                new StubProviderCallService(),
                quotaService,
                new TokenServiceMetrics(new SimpleMeterRegistry()),
                tierResolver
            )
        ).addFilters(new RateLimitHeadersFilter(), new SecurityHeadersFilter()).build();

        mockMvc.perform(
                post("/api/v1/tokens/consume")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"userId\":\"\",\"provider\":\"\",\"tokens\":0}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    private static final class StubTokenBucketService extends TokenBucketService {
        private final TokenBucketResult result;

        private StubTokenBucketService(TokenBucketResult result) {
            super(new TokenBucketProperties(), new StubTokenBucketStore(), new StubTokenUsagePublisher(), new TokenUsageEventFactory());
            this.result = result;
        }

        @Override
        public TokenBucketResult consume(String userId,
                                         String provider,
                                         long tokens,
                                         TokenTierProperties.TierConfig tier) {
            return result;
        }
    }

    private static final class StubTokenBucketStore implements com.leaky.tokens.tokenservice.bucket.TokenBucketStore {
        @Override
        public com.leaky.tokens.tokenservice.bucket.TokenBucketState load(
            com.leaky.tokens.tokenservice.bucket.TokenBucketKey key,
            java.time.Instant now
        ) {
            return new com.leaky.tokens.tokenservice.bucket.TokenBucketState(0L, now);
        }

        @Override
        public void save(com.leaky.tokens.tokenservice.bucket.TokenBucketKey key,
                         com.leaky.tokens.tokenservice.bucket.TokenBucketState state) {
        }
    }

    private static final class StubTokenUsagePublisher implements TokenUsagePublisher {
        @Override
        public void publish(com.leaky.tokens.tokenservice.events.TokenUsageEvent event) {
        }
    }

    private static final class StubProviderCallService extends ProviderCallService {
        private StubProviderCallService() {
            super((provider, request) -> new ProviderResponse(provider, Map.of("message", "ok")),
                io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.ofDefaults(),
                io.github.resilience4j.retry.RetryRegistry.ofDefaults(),
                io.github.resilience4j.bulkhead.BulkheadRegistry.ofDefaults(),
                io.github.resilience4j.timelimiter.TimeLimiterRegistry.ofDefaults());
        }

        @Override
        public ProviderResponse call(String provider, ProviderRequest request) {
            return new ProviderResponse(provider, Map.of("message", "ok"));
        }
    }
}
