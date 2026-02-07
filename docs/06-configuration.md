# Configuration Guide

This document covers all configuration options for the Leaky Tokens system.

## Table of Contents
1. [Configuration Overview](#configuration-overview)
2. [Feature Flags](#feature-flags)
3. [Service-Specific Configuration](#service-specific-configuration)
4. [Rate Limiting Configuration](#rate-limiting-configuration)
5. [Database Configuration](#database-configuration)
6. [Security Configuration](#security-configuration)
7. [Kafka Configuration](#kafka-configuration)
8. [Monitoring Configuration](#monitoring-configuration)
9. [Environment Profiles](#environment-profiles)

---

## Configuration Overview

Leaky Tokens uses Spring Boot's configuration system with externalized configuration via:
- `application.yml` files (per service)
- Spring Cloud Config Server (centralized)
- Environment variables
- Command-line arguments

**Configuration Hierarchy** (highest to lowest priority):
1. Command-line arguments
2. Environment variables
3. SPRING_APPLICATION_JSON
4. Profile-specific YAML files
5. Application YAML files
6. Config Server (if enabled)

---

## Feature Flags

Feature flags allow runtime toggling of functionality without code changes.

### Token Service Feature Flags

**Prefix:** `feature.flags.token-service`

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `quotaEnforcement` | Boolean | true | Enable/disable quota checking |
| `sagaPurchases` | Boolean | true | Enable/disable token purchase SAGA |

**Example:**
```yaml
feature:
  flags:
    token-service:
      quotaEnforcement: true
      sagaPurchases: true
```

**Usage:** Disable quota enforcement during maintenance:
```bash
curl -X POST http://localhost:8082/actuator/configprops \
  -H "Content-Type: application/json" \
  -d '{"feature.flags.token-service.quotaEnforcement": false}'
```

### Gateway Feature Flags

**Prefix:** `feature.flags.gateway`

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `rateLimiting` | Boolean | true | Enable/disable gateway rate limiting |
| `apiKeyValidation` | Boolean | true | Enable/disable API key authentication |

**Example:**
```yaml
feature:
  flags:
    gateway:
      rateLimiting: true
      apiKeyValidation: true
```

---

## Service-Specific Configuration

### Token Service

#### Token Bucket Configuration

**Prefix:** `token.bucket`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `capacity` | Integer | 1000 | Maximum tokens in bucket |
| `leakRatePerSecond` | Double | 10.0 | Tokens leaked per second |
| `windowSeconds` | Integer | 60 | Fixed window duration (seconds) |
| `strategy` | Enum | LEAKY_BUCKET | Algorithm: LEAKY_BUCKET, TOKEN_BUCKET, FIXED_WINDOW |
| `entryTtl` | Duration | 6h | Bucket entry time-to-live |
| `cleanupInterval` | Duration | 30m | Cleanup job interval |

**Example:**
```yaml
token:
  bucket:
    capacity: 1000
    leakRatePerSecond: 10.0
    windowSeconds: 60
    strategy: LEAKY_BUCKET
    entryTtl: 6h
    cleanupInterval: 30m
```

#### Token Quota Configuration

**Prefix:** `token.quota`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | true | Enable quota system |
| `window` | Duration | 24h | Quota reset window |

**Example:**
```yaml
token:
  quota:
    enabled: true
    window: 24h
```

#### Token Tier Configuration

**Prefix:** `token.tiers`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `defaultTier` | String | USER | Default tier name |
| `levels` | Map | - | Tier configurations |

**Tier Config Properties:**

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `priority` | Integer | 0 | Higher = better tier |
| `bucketCapacityMultiplier` | Double | 1.0 | Multiplier for bucket capacity |
| `bucketLeakRateMultiplier` | Double | 1.0 | Multiplier for leak rate |
| `quotaMaxTokens` | Long | null | Maximum quota cap |

**Example:**
```yaml
token:
  tiers:
    defaultTier: USER
    levels:
      USER:
        priority: 1
        bucketCapacityMultiplier: 1.0
        bucketLeakRateMultiplier: 1.0
        quotaMaxTokens: 10000
      PREMIUM:
        priority: 2
        bucketCapacityMultiplier: 2.0
        bucketLeakRateMultiplier: 2.0
        quotaMaxTokens: 100000
      ENTERPRISE:
        priority: 3
        bucketCapacityMultiplier: 5.0
        bucketLeakRateMultiplier: 5.0
        quotaMaxTokens: 1000000
```

#### SAGA Configuration

**Prefix:** `token.saga`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `simulate-failure` | Boolean | false | Simulate SAGA failures for testing |
| `stale-threshold` | Duration | 10m | Consider SAGA stale after |
| `recovery-interval` | Duration | 60s | Recovery job interval |

**Example:**
```yaml
token:
  saga:
    simulate-failure: false
    stale-threshold: 10m
    recovery-interval: 60s
```

### API Gateway

#### Rate Limit Configuration

**Prefix:** `gateway.rate-limit`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | true | Enable rate limiting |
| `capacity` | Integer | 120 | Requests per window |
| `windowSeconds` | Integer | 60 | Window duration |
| `keyStrategy` | Enum | IP | AUTO, IP, API_KEY_HEADER, USER_HEADER |
| `headerName` | String | X-Api-Key | API key header name |
| `userHeaderName` | String | X-User-Id | User ID header name |
| `counterTtl` | Duration | 6h | Counter TTL |
| `cleanupInterval` | Duration | 30m | Cleanup interval |

**Example:**
```yaml
gateway:
  rate-limit:
    enabled: true
    capacity: 120
    windowSeconds: 60
    keyStrategy: API_KEY_HEADER
    headerName: X-Api-Key
    userHeaderName: X-User-Id
    counterTtl: 6h
    cleanupInterval: 30m
```

#### Per-Route Rate Limits

```yaml
gateway:
  rate-limit:
    routes:
      token-service:
        capacity: 100
        windowSeconds: 60
      analytics-service:
        capacity: 50
        windowSeconds: 60
```

#### Whitelist Paths

```yaml
gateway:
  rate-limit:
    whitelistPaths:
      - /api/v1/tokens/status
      - /actuator/health
```

#### API Key Authentication

**Prefix:** `gateway.api-key`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | Boolean | true | Enable API key auth |
| `headerName` | String | X-Api-Key | Header name |
| `authServerUrl` | String | http://localhost:8081 | Auth server URL |
| `cacheTtlSeconds` | Integer | 300 | Validation cache TTL |
| `cacheMaxSize` | Integer | 10000 | Max cache entries |

**Example:**
```yaml
gateway:
  api-key:
    enabled: true
    headerName: X-Api-Key
    authServerUrl: http://localhost:8081
    cacheTtlSeconds: 300
    cacheMaxSize: 10000
```

---

## Database Configuration

### PostgreSQL

**Connection:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/token_db
    username: leaky_user
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
```

**Connection Pool (HikariCP):**
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

**JPA/Hibernate:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Use 'create' for dev, 'validate' for prod
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

### Redis

**Connection:**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

**Sentinel Configuration (HA):**
```yaml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes:
        - sentinel1:26379
        - sentinel2:26379
        - sentinel3:26379
```

### Cassandra

```yaml
spring:
  cassandra:
    contact-points: localhost
    port: 9042
    keyspace-name: analytics
    local-datacenter: datacenter1
    schema-action: create-if-not-exists
```

---

## Security Configuration

### JWT Configuration

**Auth Server:**
```yaml
jwt:
  private-key: ${JWT_PRIVATE_KEY}  # RSA private key (PEM format)
  public-key: ${JWT_PUBLIC_KEY}    # RSA public key (PEM format)
  issuer: leaky-tokens-auth-server
  expiration: 3600  # seconds (1 hour)
```

**Resource Servers (Token Service, Analytics):**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://localhost:8081/oauth2/jwks
```

### Password Policy

```yaml
auth:
  password:
    min-length: 8
    max-length: 128
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special: false
    bcrypt-strength: 10
```

### API Key Configuration

```yaml
api-key:
  prefix: leaky
  hash-algorithm: SHA-256
  key-length: 32  # hex characters after prefix
```

### CORS Configuration

```yaml
cors:
  allowed-origins:
    - http://localhost:3000
    - https://app.example.com
  allowed-methods:
    - GET
    - POST
    - PUT
    - DELETE
  allowed-headers:
    - Authorization
    - Content-Type
    - X-Api-Key
  allow-credentials: true
  max-age: 3600
```

---

## Kafka Configuration

### Producer (Token Service)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      batch-size: 16384
      buffer-memory: 33554432
```

### Consumer (Analytics Service)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: analytics-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.leaky.tokens
```

### Topic Configuration

```yaml
kafka:
  topics:
    token-usage: token-usage
    token-purchase-events: token-purchase-events
    partitions: 3
    replication-factor: 1
```

---

## Monitoring Configuration

### Actuator Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include:
          - health
          - info
          - metrics
          - prometheus
          - configprops
          - env
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
    metrics:
      enabled: true
    prometheus:
      enabled: true
```

### Prometheus Metrics

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 100ms,200ms,500ms,1s,5s
```

### Distributed Tracing (Jaeger)

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### Logging

```yaml
logging:
  level:
    root: INFO
    com.leaky.tokens: DEBUG
    org.springframework.security: INFO
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30
```

### Correlation ID

```yaml
correlation:
  header-name: X-Correlation-Id
  mdc-key: correlationId
```

---

## Environment Profiles

### Development Profile

**File:** `application-dev.yml`

```yaml
spring:
  config:
    activate:
      on-profile: dev
  
  datasource:
    url: jdbc:postgresql://localhost:5432/token_db
    
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

logging:
  level:
    com.leaky.tokens: DEBUG
    org.springframework.web: DEBUG
```

**Usage:**
```bash
./gradlew :token-service:bootRun --args='--spring.profiles.active=dev'
```

### Local Profile (No External Dependencies)

**File:** `application-local.yml`

```yaml
spring:
  config:
    activate:
      on-profile: local
  
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    
  jpa:
    hibernate:
      ddl-auto: create-drop
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  redis:
    host: localhost  # Use embedded if available
```

### Production Profile

**File:** `application-prod.yml`

```yaml
spring:
  config:
    activate:
      on-profile: prod
  
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 50
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

logging:
  level:
    root: WARN
    com.leaky.tokens: INFO
  file:
    name: /var/log/leaky-tokens/application.log
```

### Docker Profile

**File:** `application-docker.yml`

```yaml
spring:
  config:
    activate:
      on-profile: docker
  
  datasource:
    url: jdbc:postgresql://postgres:5432/token_db
  
  redis:
    host: redis
  
  kafka:
    bootstrap-servers: kafka:9092
  
  cassandra:
    contact-points: cassandra-db
```

---

## Configuration Best Practices

### 1. Use Environment Variables for Secrets

```yaml
# Bad - hardcoded password
spring.datasource.password: password123

# Good - use environment variable
spring.datasource.password: ${DB_PASSWORD}
```

### 2. Externalize Configuration

Use Spring Cloud Config for centralized configuration:

```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888
  cloud:
    config:
      name: token-service
      profile: ${SPRING_PROFILES_ACTIVE:default}
```

### 3. Validate Configuration on Startup

```java
@Configuration
@Validated
@ConfigurationProperties(prefix = "token.bucket")
public class TokenBucketProperties {
    
    @Min(1)
    @Max(100000)
    private int capacity = 1000;
    
    @Positive
    private double leakRatePerSecond = 10.0;
    
    // getters/setters
}
```

### 4. Feature Flags for Safe Deployment

```yaml
feature:
  flags:
    token-service:
      new-algorithm-enabled: false  # Start disabled
```

Enable gradually:
```bash
# After deployment verification
curl -X POST /actuator/configprops \
  -d '{"feature.flags.token-service.new-algorithm-enabled": true}'
```

---

**Previous**: [Use Cases](05-use-cases.md)  
**Next**: [Monitoring & Observability →](07-monitoring.md)
