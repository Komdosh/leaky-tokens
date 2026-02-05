# Monitoring & Observability

This guide covers monitoring, logging, metrics, and troubleshooting for the Leaky Tokens system.

## Table of Contents
1. [Overview](#overview)
2. [Health Checks](#health-checks)
3. [Metrics & Prometheus](#metrics--prometheus)
4. [Grafana Dashboards](#grafana-dashboards)
5. [Distributed Tracing](#distributed-tracing)
6. [Centralized Logging](#centralized-logging)
7. [Alerting](#alerting)
8. [Reading Logs](#reading-logs)

---

## Overview

Leaky Tokens provides comprehensive observability through:

- **Health Checks**: Service availability monitoring
- **Metrics**: Prometheus-based metrics collection
- **Tracing**: Jaeger distributed tracing
- **Logging**: Structured logging with correlation IDs
- **Dashboards**: Pre-configured Grafana dashboards

### Observability Stack

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Applications │───▶│ Prometheus   │───▶│   Grafana    │
│  (Metrics)   │    │  (Scrape)    │    │ (Visualize)  │
└──────────────┘    └──────────────┘    └──────────────┘
       │
       │           ┌──────────────┐    ┌──────────────┐
       └──────────▶│   Jaeger     │    │   Log        │
                   │  (Tracing)   │    │ Aggregation  │
                   └──────────────┘    └──────────────┘
```

### Access Points

| Service | URL | Description |
|---------|-----|-------------|
| Prometheus | http://localhost:9090 | Metrics collection and querying |
| Grafana | http://localhost:3000 | Visualization dashboards (admin/admin) |
| Jaeger | http://localhost:16686 | Distributed tracing UI |
| Eureka | http://localhost:8761 | Service registry |

---

## Health Checks

### Service Health Endpoints

Each service exposes health information via Actuator:

```bash
# Basic health
curl http://localhost:8082/actuator/health

# Detailed health (when authorized)
curl http://localhost:8082/actuator/health \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 345623511040,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.2.0"
      }
    }
  }
}
```

### Kubernetes Probes

**Liveness Probe:** Indicates if service is running
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

**Readiness Probe:** Indicates if service is ready to receive traffic
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

### Health Check Commands

```bash
#!/bin/bash
# health-check.sh

SERVICES=(
  "http://localhost:8080:API Gateway"
  "http://localhost:8081:Auth Server"
  "http://localhost:8082:Token Service"
  "http://localhost:8083:Analytics Service"
  "http://localhost:8761:Service Discovery"
)

for service in "${SERVICES[@]}"; do
  IFS=':' read -r url name <<< "$service"
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url/actuator/health")
  
  if [ "$status" == "200" ]; then
    echo "✓ $name: UP"
  else
    echo "✗ $name: DOWN (HTTP $status)"
  fi
done
```

---

## Metrics & Prometheus

### Available Metrics

#### HTTP Request Metrics

| Metric | Description | Labels |
|--------|-------------|--------|
| `http_server_requests_seconds_count` | Request count | uri, method, status |
| `http_server_requests_seconds_sum` | Request latency sum | uri, method, status |
| `http_server_requests_seconds_max` | Max request latency | uri, method, status |

**Example Query:**
```promql
# Request rate per endpoint
rate(http_server_requests_seconds_count[5m])

# 95th percentile latency
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
)

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) 
  / 
sum(rate(http_server_requests_seconds_count[5m]))
```

#### Business Metrics

**Token Service:**
```promql
# Token consumption rate
rate(token_consumption_total[5m])

# Quota insufficient errors
rate(token_consumption_errors_total{error="QUOTA_INSUFFICIENT"}[5m])

# Rate limited requests
rate(token_rate_limited_total[5m])
```

**SAGA Metrics:**
```promql
# SAGA completion rate
rate(saga_completed_total[5m])

# SAGA failure rate
rate(saga_failed_total[5m])

# SAGA duration
saga_duration_seconds{quantile="0.95"}
```

**JVM Metrics:**
```promql
# Memory usage
jvm_memory_used_bytes{area="heap"}

# GC pauses
rate(jvm_gc_pause_seconds_sum[5m])

# Thread count
jvm_threads_live_threads
```

### Custom Metrics

The system exposes custom business metrics:

```promql
# API key validation cache hits/misses
api_key_cache_hits_total
api_key_cache_misses_total

# Token bucket operations
bucket_consumptions_total
bucket_rejections_total

# Kafka events
kafka_consumer_records_consumed_total
kafka_producer_record_send_total
```

### Prometheus Queries

**Common Queries:**

```promql
# Service availability
up{job="token-service"}

# Request latency by percentile
histogram_quantile(0.99, 
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
)

# Error rate by service
sum by (service) (
  rate(http_server_requests_seconds_count{status=~"5.."}[5m])
)

# Active connections to database
hikaricp_connections_active{pool="HikariPool-1"}

# Redis operations per second
rate(redis_operations_total[5m])
```

---

## Grafana Dashboards

### Accessing Grafana

```
URL: http://localhost:3000
Default credentials: admin/admin
```

### Pre-configured Dashboards

#### 1. System Overview Dashboard

**Panels:**
- Service Health Status
- Request Rate (RPM)
- Error Rate (%)
- P95 Latency (ms)
- Active Connections
- JVM Memory Usage

#### 2. Token Service Dashboard

**Panels:**
- Token Consumption Rate
- Quota Status by User
- Rate Limit Hits
- SAGA State Distribution
- Outbox Queue Depth
- Provider Call Latency

**PromQL Examples:**
```promql
# Token consumption by provider
sum by (provider) (rate(token_consumption_total[5m]))

# SAGA state distribution
saga_states{status="COMPLETED"}
saga_states{status="FAILED"}
```

#### 3. Analytics Dashboard

**Panels:**
- Events Ingestion Rate
- Cassandra Query Latency
- Anomaly Detection Alerts
- Top Users by Token Consumption
- Usage Trends (24h)

#### 4. API Gateway Dashboard

**Panels:**
- Requests per Second
- Rate Limit Hits by Key
- Circuit Breaker State
- Authentication Success/Failure
- Response Time Distribution

### Creating Custom Dashboards

1. **Add Prometheus Data Source**
   - URL: http://prometheus:9090
   - Access: Server

2. **Import Dashboard JSON**
   ```json
   {
     "dashboard": {
       "title": "Custom Token Metrics",
       "panels": [
         {
           "title": "Token Consumption",
           "targets": [
             {
               "expr": "rate(token_consumption_total[5m])",
               "legendFormat": "{{provider}}"
             }
           ]
         }
       ]
     }
   }
   ```

---

## Distributed Tracing

### Jaeger UI

```
URL: http://localhost:16686
```

### Trace Structure

Each request generates a trace with spans:

```
Trace: Token Consumption Request
├── Span: Gateway Rate Limit Check (2ms)
├── Span: JWT Validation (5ms)
├── Span: Token Service Processing (150ms)
│   ├── Span: Quota Check (20ms)
│   ├── Span: Rate Limit Check (5ms)
│   ├── Span: Provider Call (100ms)
│   └── Span: Event Publish (10ms)
└── Span: Response (1ms)
```

### Viewing Traces

1. Open Jaeger UI
2. Select Service: "token-service"
3. Click "Find Traces"
4. Click on a trace to see details

### Trace Correlation

All logs include correlation IDs for trace linking:

```json
{
  "timestamp": "2026-02-05T15:30:00Z",
  "level": "INFO",
  "message": "Token consumed successfully",
  "correlationId": "abc123def456",
  "traceId": "4f7e2a8c9d1b",
  "spanId": "3e5f7a9c2d4e"
}
```

### Custom Spans

```java
@NewSpan("quotaCheck")
public QuotaReservation checkQuota(
    @SpanTag("userId") UUID userId,
    @SpanTag("provider") String provider) {
    // Business logic
}
```

---

## Centralized Logging

### Log Format

Structured JSON logging for easy parsing:

```json
{
  "@timestamp": "2026-02-05T15:30:45.123Z",
  "level": "INFO",
  "logger": "com.leaky.tokens.tokenservice.TokenController",
  "message": "Token consumed",
  "thread": "http-nio-8082-exec-1",
  "correlationId": "corr-123-abc",
  "traceId": "trace-456-def",
  "service": "token-service",
  "context": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "provider": "openai",
    "tokens": 50,
    "allowed": true
  }
}
```

### Log Aggregation Setup

#### Option 1: ELK Stack (Elasticsearch, Logstash, Kibana)

**Filebeat Configuration:**
```yaml
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/leaky-tokens/*.log
  fields:
    service: token-service
  fields_under_root: true

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "leaky-tokens-%{+yyyy.MM.dd}"
```

**Kibana Search Examples:**
```
# Find all errors
level:ERROR

# Find errors for specific user
context.userId:"550e8400-e29b-41d4-a716-446655440000" AND level:ERROR

# Find slow requests
message:"Token consumed" AND context.duration:>1000

# Find rate limiting events
message:"Rate limit exceeded"
```

#### Option 2: Grafana Loki

**Promtail Configuration:**
```yaml
server:
  http_listen_port: 9080

clients:
  - url: http://localhost:3100/loki/api/v1/push

scrape_configs:
  - job_name: leaky-tokens
    static_configs:
      - targets:
          - localhost
        labels:
          job: token-service
          __path__: /var/log/leaky-tokens/*.log
```

**LogQL Queries:**
```logql
# All logs for token-service
{job="token-service"}

# Error logs
{job="token-service"} |= "ERROR"

# Token consumption for specific user
{job="token-service"} 
  | json 
  | context_userId="550e8400-e29b-41d4-a716-446655440000"
  |~ "Token consumed"

# Rate limiting events
{job="token-service"} 
  | json 
  | message="Rate limit exceeded"
```

### Log Levels

| Level | Usage |
|-------|-------|
| ERROR | System errors, exceptions, failures |
| WARN | Recoverable issues, warnings |
| INFO | Business operations, state changes |
| DEBUG | Detailed debugging info |
| TRACE | Very detailed (request/response bodies) |

### Log Configuration

```yaml
logging:
  level:
    root: INFO
    com.leaky.tokens: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] [traceId=%X{traceId}] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/leaky-tokens/token-service.log
    max-size: 100MB
    max-history: 30
```

---

## Alerting

### Prometheus Alert Rules

**File:** `prometheus/alerts.yml`

```yaml
groups:
  - name: leaky-tokens
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) 
          / 
          sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is above 5% for 5 minutes"

      # Service down
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.instance }} is down"

      # High latency
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95, 
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
          ) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "P95 latency is above 1 second"

      # Low token quota
      - alert: LowTokenQuota
        expr: |
          token_quota_remaining / token_quota_total < 0.1
        for: 1h
        labels:
          severity: info
        annotations:
          summary: "Low token quota"
          description: "User {{ $labels.userId }} has less than 10% tokens remaining"

      # SAGA failures
      - alert: SagaFailures
        expr: |
          rate(saga_failed_total[5m]) > 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "SAGA failures detected"
```

### Alertmanager Configuration

```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@leaky-tokens.com'

route:
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty'
    - match:
        severity: warning
      receiver: 'slack'

receivers:
  - name: 'default'
    email_configs:
      - to: 'oncall@leaky-tokens.com'

  - name: 'slack'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/...'
        channel: '#alerts'

  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: '<key>'
```

### Grafana Alerting

Configure alerts directly in Grafana:

1. Go to Dashboard → Panel → Alert tab
2. Define condition: `WHEN avg() OF query(A, 5m, now) IS ABOVE 1000`
3. Set evaluation interval: `5m`
4. Configure notification channel (Email, Slack, PagerDuty)

---

## Reading Logs

### Docker Compose Logs

```bash
# View all service logs
docker compose logs -f

# View specific service
docker compose logs -f token-service

# View last 100 lines
docker compose logs --tail=100 token-service

# Filter by time
docker compose logs --since=10m token-service
```

### Gradle BootRun Logs

```bash
# Run with colored output
./gradlew :token-service:bootRun --console=plain

# Redirect to file
./gradlew :token-service:bootRun > token-service.log 2>&1

# Follow log file
tail -f token-service.log
```

### Log Analysis Commands

```bash
# Find all errors
grep "ERROR" /var/log/leaky-tokens/*.log

# Find slow requests (> 1 second)
grep "duration.*1[0-9][0-9][0-9]" token-service.log

# Count errors by type
grep "ERROR" token-service.log | cut -d':' -f4 | sort | uniq -c

# Find requests for specific user
grep "userId.*550e8400-e29b-41d4-a716-446655440000" token-service.log

# Real-time monitoring
tail -f token-service.log | grep "Token consumed"

# Extract JSON fields using jq
grep "Token consumed" token-service.log | jq -r '.context.tokens'

# Find correlation ID in all services
grep -r "corr-123-abc" /var/log/leaky-tokens/
```

### Common Log Patterns

**Successful Token Consumption:**
```
INFO  TokenController - Token consumed: userId=..., provider=openai, tokens=50, allowed=true
```

**Rate Limited:**
```
WARN  TokenBucketService - Rate limit exceeded: userId=..., provider=openai, waitSeconds=15
```

**Insufficient Quota:**
```
WARN  TokenQuotaService - Insufficient quota: userId=..., provider=openai, requested=100, remaining=10
```

**SAGA Started:**
```
INFO  TokenPurchaseSagaService - SAGA started: sagaId=..., userId=..., tokens=1000
```

**SAGA Completed:**
```
INFO  TokenPurchaseSagaService - SAGA completed: sagaId=..., durationMs=5234
```

**Provider Call Failed:**
```
ERROR ProviderCallService - Provider call failed: provider=openai, error=Connection timeout
```

---

## Troubleshooting with Observability

### Scenario: High Error Rate

1. **Check Grafana Dashboard**
   - Which endpoint has errors?
   - What's the error distribution?

2. **Query Logs**
   ```bash
   grep "ERROR" token-service.log | jq '.message' | sort | uniq -c
   ```

3. **Check Traces**
   - Find slow/failed traces in Jaeger
   - Identify the failing component

4. **Check Metrics**
   ```promql
   # Error rate by status code
   sum by (status) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
   ```

### Scenario: Slow Requests

1. **Check P95 Latency**
   ```promql
   histogram_quantile(0.95, 
     sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
   )
   ```

2. **Analyze Traces**
   - Find traces > 1 second
   - Identify slowest span

3. **Check Database Metrics**
   ```promql
   # Slow queries
   hikaricp_connections_usage_seconds_max
   ```

### Scenario: Service Unavailable

1. **Check Health Endpoint**
   ```bash
   curl http://localhost:8082/actuator/health
   ```

2. **Check Logs for Startup Errors**
   ```bash
   docker compose logs token-service | grep -i "error\|exception"
   ```

3. **Check Resource Usage**
   ```bash
   docker stats token-service
   ```

---

**Previous**: [Configuration](06-configuration.md)  
**Next**: [API Reference →](08-api-reference.md)
