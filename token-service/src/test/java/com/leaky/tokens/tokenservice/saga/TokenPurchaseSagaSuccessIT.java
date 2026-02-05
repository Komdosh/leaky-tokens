package com.leaky.tokens.tokenservice.saga;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.leaky.tokens.tokenservice.TokenServiceApplication;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import com.leaky.tokens.tokenservice.quota.TokenPool;
import com.leaky.tokens.tokenservice.quota.TokenPoolRepository;
import com.leaky.tokens.tokenservice.support.TokenServiceIntegrationTestBase;
import com.leaky.tokens.tokenservice.support.TokenServiceTestConfig;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = {TokenServiceApplication.class, TokenServiceTestConfig.class},
    properties = {
        "spring.main.banner-mode=off"
    }
)
@ActiveProfiles("test")
class TokenPurchaseSagaSuccessIT extends TokenServiceIntegrationTestBase {
    @Autowired
    private TokenPurchaseSagaService sagaService;

    @Autowired
    private TokenOutboxRepository outboxRepository;

    @Autowired
    private TokenPoolRepository tokenPoolRepository;

    @Autowired
    private TokenPurchaseSagaRepository sagaRepository;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void setup() {
        flyway.migrate();
        outboxRepository.deleteAll();
        sagaRepository.deleteAll();
        tokenPoolRepository.deleteAll();
    }

    @Test
    void completesSagaAndAllocatesQuota() {
        TokenPurchaseRequest request = new TokenPurchaseRequest();
        request.setUserId("00000000-0000-0000-0000-000000000001");
        request.setProvider("openai");
        request.setTokens(10);

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        TokenPurchaseResponse response = sagaService.start(request, tier, null);

        assertThat(response.status()).isEqualTo(TokenPurchaseSagaStatus.COMPLETED);

        List<TokenOutboxEntry> outboxEntries = outboxRepository.findAll();
        List<String> eventTypes = outboxEntries.stream().map(TokenOutboxEntry::getEventType).toList();
        assertThat(eventTypes).contains("TOKEN_PURCHASE_COMPLETED", "TOKEN_ALLOCATED", "TOKEN_PAYMENT_RESERVED");
        assertThat(eventTypes).doesNotContain("PAYMENT_RELEASE_REQUESTED");

        TokenPool pool = tokenPoolRepository.findByUserIdAndProvider(
            UUID.fromString(request.getUserId()), request.getProvider()
        ).orElse(null);

        assertThat(pool).isNotNull();
        assertThat(pool.getTotalTokens()).isEqualTo(10);
        assertThat(pool.getRemainingTokens()).isEqualTo(10);
    }
}
