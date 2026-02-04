package com.leaky.tokens.tokenservice.outbox;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnBean(KafkaTemplate.class)
public class OutboxPublisherJob {
    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final TokenOutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;

    public OutboxPublisherJob(TokenOutboxRepository repository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              @Value("${token.usage.topic:token-usage}") String topic,
                              @Value("${token.outbox.batch-size:50}") int batchSize) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${token.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishBatch() {
        List<TokenOutboxEntry> entries = repository.findUnpublished(PageRequest.of(0, batchSize));
        if (entries.isEmpty()) {
            return;
        }

        for (TokenOutboxEntry entry : entries) {
            try {
                kafkaTemplate.send(topic, entry.getId().toString(), entry.getPayload()).get();
                entry.setPublishedAt(Instant.now());
                repository.save(entry);
            } catch (Exception ex) {
                logger.warn("Failed to publish outbox entry {}", entry.getId(), ex);
                break;
            }
        }
    }
}
