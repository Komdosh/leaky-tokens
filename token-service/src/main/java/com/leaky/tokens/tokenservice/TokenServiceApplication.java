package com.leaky.tokens.tokenservice;

import com.leaky.tokens.tokenservice.bucket.TokenBucketProperties;
import com.leaky.tokens.tokenservice.quota.TokenQuotaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties({TokenBucketProperties.class, TokenQuotaProperties.class})
@SpringBootApplication
public class TokenServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TokenServiceApplication.class, args);
    }
}
