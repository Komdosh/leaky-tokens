package com.leaky.tokens.apigateway.config;

import java.util.List;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadBalancerCacheConfig {
    @Bean
    public CacheManager loadBalancerCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of("load-balancer"));
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(java.time.Duration.ofMinutes(5)));
        return cacheManager;
    }
}
