package com.leaky.tokens.apigateway.ratelimit;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.leaky.tokens.apigateway.metrics.GatewayMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "gateway.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewayRateLimitFilter implements WebFilter {
    private final GatewayRateLimitProperties properties;
    private final GatewayMetrics metrics;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public GatewayRateLimitFilter(GatewayRateLimitProperties properties, GatewayMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String key = resolveKey(exchange.getRequest());
        long capacity = Math.max(1L, properties.getCapacity());
        long windowSeconds = Math.max(1L, properties.getWindowSeconds());
        Instant now = Instant.now();

        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(now, 0L));
        synchronized (counter) {
            if (!now.isBefore(counter.windowStart.plusSeconds(windowSeconds))) {
                counter.windowStart = now;
                counter.count = 0L;
            }
            if (counter.count + 1 > capacity) {
                long retryAfter = Math.max(0L, Duration.between(now, counter.windowStart.plusSeconds(windowSeconds)).getSeconds());
                metrics.rateLimit("blocked");
                return reject(exchange, capacity, 0L, retryAfter);
            }
            counter.count += 1;
            long remaining = Math.max(0L, capacity - counter.count);
            exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(capacity));
            exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));
            exchange.getResponse().getHeaders().set("X-RateLimit-Reset", String.valueOf(counter.windowStart.plusSeconds(windowSeconds).getEpochSecond()));
        }

        metrics.rateLimit("allowed");
        return chain.filter(exchange);
    }

    private Mono<Void> reject(ServerWebExchange exchange, long capacity, long remaining, long retryAfterSeconds) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfterSeconds));
        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(capacity));
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));
        exchange.getResponse().getHeaders().set("X-RateLimit-Reset",
            String.valueOf(Instant.now().plusSeconds(retryAfterSeconds).getEpochSecond()));
        String body = "{\"message\":\"rate limit exceeded\",\"retryAfterSeconds\":" + retryAfterSeconds + "}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
            .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String resolveKey(ServerHttpRequest request) {
        RateLimitKeyStrategy strategy = properties.getKeyStrategy();
        if (strategy == RateLimitKeyStrategy.API_KEY_HEADER) {
            String header = request.getHeaders().getFirst(properties.getHeaderName());
            if (header != null && !header.isBlank()) {
                return "apiKey:" + header.trim();
            }
        } else if (strategy == RateLimitKeyStrategy.USER_HEADER) {
            String header = request.getHeaders().getFirst(properties.getUserHeaderName());
            if (header != null && !header.isBlank()) {
                return "user:" + header.trim();
            }
        }

        String ip = Objects.toString(request.getRemoteAddress(), "unknown");
        return "ip:" + ip;
    }

    private static final class WindowCounter {
        private Instant windowStart;
        private long count;

        private WindowCounter(Instant windowStart, long count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
