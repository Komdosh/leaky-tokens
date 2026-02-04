package com.leaky.tokens.tokenservice.bucket;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "token.bucket")
public class TokenBucketProperties {
    private long capacity = 1000;
    private double leakRatePerSecond = 10.0;
    private long windowSeconds = 60;
    private TokenBucketStrategy strategy = TokenBucketStrategy.LEAKY_BUCKET;
}
