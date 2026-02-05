# 🕵️ Distributed Tracing Setup

This guide explains how to set up and use distributed tracing with Jaeger for the Leaky Tokens platform.

## Overview

Distributed tracing helps you understand the flow of requests across microservices, identify performance bottlenecks, and debug issues in distributed systems.

### Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│ API Gateway │────▶│Auth Server  │
└─────────────┘     └──────┬──────┘     └─────────────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌────────────┐ ┌────────────┐ ┌────────────┐
    │Token Service│ │ Analytics  │ │   Stubs    │
    └──────┬──────┘ └────────────┘ └────────────┘
           │
           ▼
    ┌────────────┐
    │   Jaeger   │
    │  Collector │
    └──────┬──────┘
           │
           ▼
    ┌────────────┐
    │ Jaeger UI  │
    │  :16686    │
    └────────────┘
```

## Quick Start

### 1. Start the Tracing Stack

Jaeger is included in the main Docker Compose files. Choose the one that fits your needs:

**Option A: Infrastructure Only (Jaeger + Databases)**
```bash
# Start infrastructure including Jaeger
docker-compose -f docker-compose.infra.yml up -d

# Run services locally with tracing
./gradlew bootRun --parallel
```

**Option B: Full Stack (Everything in Docker)**
```bash
# Start all services including Jaeger
docker-compose -f docker-compose.full.yml up -d

# Wait for services to start
sleep 60
```

**Check Jaeger UI:**
```bash
open http://localhost:16686
```

### 2. Generate Some Traffic

```bash
# Create a user and get JWT
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"trace-test","email":"test@example.com","password":"password"}' \
  | jq -r '.token')

# Make some API calls
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8082/api/v1/tokens/quota?userId=YOUR_USER_ID&provider=openai"

# Consume tokens
curl -X POST http://localhost:8082/api/v1/tokens/consume \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "YOUR_USER_ID",
    "provider": "openai",
    "tokens": 50,
    "prompt": "Hello, trace me!"
  }'
```

### 3. View Traces in Jaeger

1. Open http://localhost:16686
2. Select a service from the dropdown (e.g., `api-gateway`)
3. Click **Find Traces**
4. Click on any trace to see the detailed call graph

## Configuration

### OTLP Endpoints

| Protocol | Endpoint | Port | Usage |
|----------|----------|------|-------|
| OTLP HTTP | http://localhost:4318 | 4318 | Default for services |
| OTLP gRPC | http://localhost:4317 | 4317 | Alternative protocol |
| Jaeger UI | http://localhost:16686 | 16686 | View traces |

### Service Configuration

Services are configured via Spring Cloud Config with tracing profiles:

**Local Development (`tracing.yml`):**
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 100% sampling for dev
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

**Docker Environment (`tracing-docker.yml`):**
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

### Enabling Tracing in Services

All services automatically enable tracing when the `tracing` profile is active:

```bash
# Via environment variable
SPRING_PROFILES_ACTIVE=tracing

# Or in application.yml
spring:
  profiles:
    include: tracing
```

### Sampling Rates

**Development:** 100% sampling (captures all requests)
**Production:** Reduce to 1-10% to manage overhead:

```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling
```

## Using Jaeger UI

### Search Interface

1. **Service**: Select the service that received the request
2. **Operation**: Filter by specific endpoint
3. **Tags**: Search by custom tags (e.g., `http.method=POST`)
4. **Lookback**: Time range for search
5. **Limit**: Number of traces to return

### Trace View

Shows a Gantt chart of all spans in a trace:
- **Timeline**: Visual representation of request duration
- **Services**: Color-coded by service
- **Duration**: Length of each operation
- **Tags**: Metadata attached to each span
- **Logs**: Structured log events

### Service Dependencies

Click the **System Architecture** tab to see:
- Service topology graph
- Request volumes between services
- Error rates on connections

## Trace Structure

### Example: Token Consumption Flow

```
Trace ID: abc123
├── Span: API Gateway (20ms)
│   ├── Tags: http.method=POST, http.url=/api/v1/tokens/consume
│   └── Logs: Received request
├── Span: Token Service (150ms)
│   ├── Tags: token.provider=openai, token.amount=50
│   ├── Span: Quota Check (20ms)
│   │   └── Tags: db.statement=SELECT...
│   ├── Span: Rate Limit Check (5ms)
│   │   └── Tags: redis.command=GET
│   ├── Span: Provider Call (100ms)
│   │   └── Tags: http.url=http://openai-stub:8093/...
│   └── Span: Event Publish (10ms)
│       └── Tags: kafka.topic=token-usage
└── Span: Response (1ms)
```

### Log Correlation

Traces are correlated with logs. Log entries include:
```
2026-02-05 14:30:45 INFO [token-service,abc123,def456] Token consumed successfully
                                 │         │      │
                                 │         │      └─ Span ID
                                 │         └─ Trace ID
                                 └─ Service Name
```

## Custom Spans

### Adding Custom Spans

```java
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;

@Service
public class TokenQuotaService {
    
    @NewSpan("quotaCheck")
    public QuotaReservation checkQuota(
            @SpanTag("userId") UUID userId,
            @SpanTag("provider") String provider) {
        // Business logic
    }
}
```

### Adding Tags to Current Span

```java
import io.micrometer.tracing.Tracer;

@Service
public class TokenService {
    private final Tracer tracer;
    
    public void process(UUID userId, String provider) {
        // Add custom tags
        tracer.currentSpan()
            .tag("user.id", userId.toString())
            .tag("provider", provider)
            .tag("tier", "PREMIUM");
        
        // Add events
        tracer.currentSpan()
            .event("quota_checked")
            .event("rate_limit_passed");
    }
}
```

### Creating Child Spans

```java
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@Service
public class AnalyticsService {
    private final Tracer tracer;
    
    public void processEvent(TokenUsageEvent event) {
        Span childSpan = tracer.nextSpan()
            .name("persistToCassandra")
            .tag("event.id", event.getId())
            .start();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(childSpan)) {
            // Cassandra write operation
            cassandraRepository.save(event);
        } finally {
            childSpan.end();
        }
    }
}
```

## Performance Considerations

### Overhead

Tracing adds minimal overhead:
- **CPU**: ~1-3% for 100% sampling
- **Memory**: ~100KB per active trace
- **Network**: ~1KB per span

### Production Tuning

1. **Reduce Sampling**:
   ```yaml
   management:
     tracing:
       sampling:
         probability: 0.01  # 1% sampling
   ```

2. **Filter Health Checks**:
   ```java
   @Bean
   publicSampler customSampler() {
       return request -> !request.getUri().startsWith("/actuator/health");
   }
   ```

3. **Limit Tag Size**:
   ```yaml
   management:
     tracing:
       baggage:
         remote-fields: userId,requestId
   ```

## Troubleshooting

### No Traces Appearing

1. **Check Jaeger is running**:
   ```bash
   docker ps | grep jaeger
   curl http://localhost:16686
   ```

2. **Verify OTLP endpoint**:
   ```bash
   curl -X POST http://localhost:4318/v1/traces \
     -H "Content-Type: application/x-protobuf" \
     --data-binary @/dev/null
   ```

3. **Check service logs** for trace IDs:
   ```bash
   docker logs token-service | grep "traceId"
   ```

### Missing Spans

- Ensure all services have tracing enabled
- Check sampling rate (might be too low)
- Verify network connectivity to Jaeger

### High Memory Usage

- Reduce sampling rate
- Enable span batching:
  ```yaml
  management:
    otlp:
      tracing:
        batch-size: 512
        queue-size: 2048
  ```

## Integration with Other Tools

### Prometheus

Traces complement metrics. Use both for full observability:
- **Metrics**: Aggregated data, trends, alerting
- **Traces**: Request details, distributed context

### Grafana

View traces in Grafana using the Jaeger datasource:
1. Add Jaeger datasource in Grafana
2. Use "Explore" to search traces
3. Link from metrics to traces

### Log Aggregation

All logs include trace context:
```json
{
  "timestamp": "2026-02-05T14:30:45Z",
  "level": "INFO",
  "message": "Token consumed",
  "traceId": "abc123",
  "spanId": "def456",
  "service": "token-service"
}
```

## Docker Compose Reference

### Services with Tracing

Jaeger is included in the main Docker Compose files:
- **`docker-compose.infra.yml`** - Jaeger + databases for local development
- **`docker-compose.full.yml`** - Jaeger + all microservices

All services automatically connect to Jaeger via environment variables.

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MANAGEMENT_OTLP_TRACING_ENDPOINT` | OTLP HTTP endpoint | http://jaeger:4318/v1/traces |
| `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` | Sampling rate (0-1) | 1.0 |
| `SPRING_PROFILES_ACTIVE` | Active profiles | docker,tracing |

### Ports

| Port | Service | Description |
|------|---------|-------------|
| 16686 | Jaeger UI | View traces |
| 4318 | Jaeger | OTLP HTTP receiver |
| 4317 | Jaeger | OTLP gRPC receiver |

## Best Practices

1. **Use Consistent Naming**: Name spans descriptively (e.g., `quotaCheck` not `check`)
2. **Add Contextual Tags**: Include userId, provider, operation type
3. **Don't Log Sensitive Data**: Never include passwords or PII in tags
4. **Use Standard Tags**: Stick to OpenTelemetry semantic conventions
5. **Monitor Trace Volume**: Watch for sampling overhead in production
6. **Correlate with Logs**: Include trace IDs in log messages
7. **Set Retention**: Configure Jaeger storage retention policy

## Additional Resources

- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)
- [Spring Boot Tracing](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.micrometer-tracing)

---

**Need Help?** Check the [Troubleshooting Guide](09-troubleshooting.md) or [Monitoring Documentation](07-monitoring.md)
