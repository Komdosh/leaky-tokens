package com.leaky.tokens.analyticsservice.events;

import java.util.UUID;

import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderKey;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRecord;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRepository;
import com.leaky.tokens.analyticsservice.storage.TokenUsageRecord;
import com.leaky.tokens.analyticsservice.storage.TokenUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "analytics.token-usage.enabled", havingValue = "true", matchIfMissing = true)
public class TokenUsageListener {
    private static final Logger logger = LoggerFactory.getLogger(TokenUsageListener.class);

    private final ObjectMapper objectMapper;
    private final TokenUsageRepository repository;
    private final TokenUsageByProviderRepository byProviderRepository;

    public TokenUsageListener(ObjectMapper objectMapper,
                              TokenUsageRepository repository,
                              TokenUsageByProviderRepository byProviderRepository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.byProviderRepository = byProviderRepository;
    }

    @KafkaListener(
        topics = "${analytics.token-usage.topic:token-usage}",
        groupId = "${analytics.token-usage.group:analytics-service}"
    )
    public void onMessage(String payload) {
        try {
            TokenUsageEvent event = objectMapper.readValue(payload, TokenUsageEvent.class);
            TokenUsageRecord record = new TokenUsageRecord();
            record.setId(UUID.randomUUID());
            record.setUserId(event.getUserId());
            record.setProvider(event.getProvider());
            record.setTokens(event.getTokens());
            record.setAllowed(event.isAllowed());
            record.setTimestamp(event.getTimestamp());
            repository.save(record);

            TokenUsageByProviderKey key = new TokenUsageByProviderKey(event.getProvider(), event.getTimestamp());
            TokenUsageByProviderRecord byProvider = new TokenUsageByProviderRecord(
                key,
                event.getUserId(),
                event.getTokens(),
                event.isAllowed()
            );
            byProviderRepository.save(byProvider);

            logger.info(
                "token-usage saved userId={} provider={} tokens={} allowed={} timestamp={}",
                event.getUserId(),
                event.getProvider(),
                event.getTokens(),
                event.isAllowed(),
                event.getTimestamp()
            );
        } catch (Exception e) {
            logger.warn("Failed to parse or persist token usage event: {}", payload, e);
        }
    }
}
