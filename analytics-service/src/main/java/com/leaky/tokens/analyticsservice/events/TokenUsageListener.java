package com.leaky.tokens.analyticsservice.events;

import com.leaky.tokens.analyticsservice.storage.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "analytics.token-usage.enabled", havingValue = "true", matchIfMissing = true)
public class TokenUsageListener {
    private static final Logger logger = LoggerFactory.getLogger(TokenUsageListener.class);

    private final ObjectMapper objectMapper;
    private final TokenUsageRepository repository;
    private final TokenUsageByProviderRepository byProviderRepository;

    @KafkaListener(
            topics = "${analytics.token-usage.topic:token-usage}",
            groupId = "${analytics.token-usage.group:analytics-service}"
    )
    public void onMessage(String payload) {
        logger.info("Received token usage event: {}", payload);
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
