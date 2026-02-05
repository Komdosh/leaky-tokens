package com.leaky.tokens.tokenservice;

import com.leaky.tokens.tokenservice.bucket.TokenBucketState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, TokenBucketState> tokenBucketRedisTemplate(RedisConnectionFactory connectionFactory) {
        JacksonJsonRedisSerializer<TokenBucketState> s = new JacksonJsonRedisSerializer<>(TokenBucketState.class);

        RedisTemplate<String, TokenBucketState> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(s);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(s);
        template.afterPropertiesSet();
        return template;
    }
}
