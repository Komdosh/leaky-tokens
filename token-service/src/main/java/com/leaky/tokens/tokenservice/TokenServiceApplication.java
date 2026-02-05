package com.leaky.tokens.tokenservice;

import com.leaky.tokens.tokenservice.bucket.TokenBucketProperties;
import com.leaky.tokens.tokenservice.flags.TokenServiceFeatureFlags;
import com.leaky.tokens.tokenservice.quota.TokenQuotaProperties;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@EnableScheduling
@EnableConfigurationProperties({TokenBucketProperties.class, TokenQuotaProperties.class, TokenTierProperties.class, TokenServiceFeatureFlags.class})
@SpringBootApplication
public class TokenServiceApplication {
    static void main(String[] args) {
        SpringApplication.run(TokenServiceApplication.class, args);
    }
}
