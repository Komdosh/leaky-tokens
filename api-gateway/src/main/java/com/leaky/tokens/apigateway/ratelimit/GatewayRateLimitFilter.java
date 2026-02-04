package com.leaky.tokens.apigateway.ratelimit;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.leaky.tokens.apigateway.metrics.GatewayMetrics;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(prefix = "gateway.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewayRateLimitFilter implements GlobalFilter, Ordered {
    private final GatewayRateLimitProperties properties;
    private final GatewayMetrics metrics;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<String> whitelistPatterns = new ArrayList<>();

    public GatewayRateLimitFilter(GatewayRateLimitProperties properties, GatewayMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
        List<String> whitelist = properties.getWhitelistPaths();
        if (whitelist != null) {
            whitelistPatterns.addAll(whitelist);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isWhitelisted(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        String key = resolveKey(exchange.getRequest());
        long capacity = Math.max(1L, resolveCapacity(exchange));
        long windowSeconds = Math.max(1L, resolveWindowSeconds(exchange));
        Instant now = Instant.now();

        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(now, 0L));
        synchronized (counter) {
            counter.lastSeen = now;
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

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

    @Scheduled(fixedDelayString = "${gateway.rate-limit.cleanup-interval:PT30M}")
    public void cleanup() {
        Duration ttl = properties.getCounterTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        Instant now = Instant.now();
        counters.entrySet().removeIf(entry -> entry.getValue().lastSeen.plus(ttl).isBefore(now));
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
        if (strategy == RateLimitKeyStrategy.AUTO || strategy == RateLimitKeyStrategy.API_KEY_HEADER) {
            String header = request.getHeaders().getFirst(properties.getHeaderName());
            if (header != null && !header.isBlank()) {
                return "apiKey:" + header.trim();
            }
        }
        if (strategy == RateLimitKeyStrategy.AUTO || strategy == RateLimitKeyStrategy.USER_HEADER) {
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
        private Instant lastSeen;

        private WindowCounter(Instant windowStart, long count) {
            this.windowStart = windowStart;
            this.count = count;
            this.lastSeen = windowStart;
        }
    }

    private boolean isWhitelisted(String path) {
        if (whitelistPatterns.isEmpty()) {
            return false;
        }
        for (String pattern : whitelistPatterns) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private long resolveCapacity(ServerWebExchange exchange) {
        GatewayRateLimitProperties.RouteLimitConfig config = resolveRouteConfig(exchange);
        if (config != null && config.getCapacity() != null && config.getCapacity() > 0) {
            return config.getCapacity();
        }
        return properties.getCapacity();
    }

    private long resolveWindowSeconds(ServerWebExchange exchange) {
        GatewayRateLimitProperties.RouteLimitConfig config = resolveRouteConfig(exchange);
        if (config != null && config.getWindowSeconds() != null && config.getWindowSeconds() > 0) {
            return config.getWindowSeconds();
        }
        return properties.getWindowSeconds();
    }

    private GatewayRateLimitProperties.RouteLimitConfig resolveRouteConfig(ServerWebExchange exchange) {
        Object routeAttr = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (!(routeAttr instanceof Route route)) {
            return null;
        }
        return properties.getRoutes().get(route.getId());
    }
}
