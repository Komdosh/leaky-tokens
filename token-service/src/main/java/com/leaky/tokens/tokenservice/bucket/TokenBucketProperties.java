package com.leaky.tokens.tokenservice.bucket;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "token.bucket")
public class TokenBucketProperties {
    @Min(1)
    private long capacity = 1000;
    @Positive
    private double leakRatePerSecond = 10.0;
    @Min(1)
    private long windowSeconds = 60;
    @NotNull
    private TokenBucketStrategy strategy = TokenBucketStrategy.LEAKY_BUCKET;
    private java.time.Duration entryTtl = java.time.Duration.ofHours(6);
    private java.time.Duration cleanupInterval = java.time.Duration.ofMinutes(30);
}
