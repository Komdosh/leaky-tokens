# Architecture Documentation

This document describes the technical architecture of the Leaky Tokens system.

## Table of Contents
1. [System Overview](#system-overview)
2. [Service Architecture](#service-architecture)
3. [Data Flow](#data-flow)
4. [Communication Patterns](#communication-patterns)
5. [Data Storage](#data-storage)
6. [Resilience Patterns](#resilience-patterns)
7. [Security Architecture](#security-architecture)
8. [Deployment Architecture](#deployment-architecture)

---

## System Overview

Leaky Tokens follows a microservices architecture designed for scalability, resilience, and maintainability.

### High-Level Architecture

```
                    ┌─────────────────┐
                    │     Client      │
                    └────────┬────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway (8080)                      │
│  • Rate Limiting  • JWT/API Key Auth  • Load Balancing     │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
┌──────────────┐ ┌──────────┐ ┌────────────────┐
│ Auth Server  │ │  Token   │ │   Analytics    │
│   (8081)     │ │ Service  │ │    Service     │
│              │ │  (8082)  │ │    (8083)      │
│ • Register   │ │          │ │                │
│ • Login      │ │ • Quota  │ │ • Events       │
│ • API Keys   │ │ • Rate   │ │ • Reports      │
│ • JWKS       │ │ • SAGA   │ │ • Anomalies    │
└──────────────┘ └──────────┘ └────────────────┘
                     │
        ┌────────────┼────────────┬────────────┐
        │            │            │            │
        ▼            ▼            ▼            ▼
┌──────────┐  ┌──────────┐ ┌──────────┐ ┌──────────┐
│  Qwen    │  │  Gemini  │ │  OpenAI  │ │   SAGA   │
│  Stub    │  │   Stub   │ │   Stub   │ │  Store   │
│ (8091)   │  │  (8092)  │ │  (8093)  │ │ (DB)     │
└──────────┘  └──────────┘ └──────────┘ └──────────┘
```

### Infrastructure Services

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                      │
├──────────────┬──────────────┬──────────────┬────────────────┤
│  PostgreSQL  │    Redis     │    Kafka     │   Cassandra    │
│    (5432)    │   (6379)     │   (9092)     │    (9042)      │
│              │              │              │                │
│ • Users      │ • Rate Limit │ • Events     │ • Time Series  │
│ • Quotas     │ • Cache      │ • Outbox     │ • Analytics    │
│ • SAGA       │ • Sessions   │              │                │
└──────────────┴──────────────┴──────────────┴────────────────┘
```

---

## Service Architecture

### API Gateway

**Responsibilities:**
- Single entry point for all clients
- Request routing based on path patterns
- Rate limiting at the edge
- Authentication (API key validation via Auth Server)
- JWT validation using JWKS
- Circuit breaker pattern for downstream failures
- Request/response transformation

**Key Components:**
- `GatewayConfig`: Route definitions
- `RateLimitFilter`: Sliding window rate limiting
- `ApiKeyAuthFilter`: API key validation
- `JwtAuthFilter`: JWT token validation
- `CircuitBreakerConfig`: Resilience configuration

**Route Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: token-service
          uri: lb://token-service
          predicates:
            - Path=/api/v1/tokens/**
```

### Auth Server

**Responsibilities:**
- User registration and authentication
- JWT token issuance (RSA-signed)
- API key lifecycle management
- JWKS endpoint for public key distribution

**Key Components:**
- `AuthController`: REST endpoints
- `UserService`: User management
- `ApiKeyService`: API key generation and validation
- `JwtService`: Token creation and validation
- `SecurityConfig`: Spring Security configuration

**Database Schema:**
```sql
users (id, username, email, password_hash, created_at, updated_at)
roles (id, name, description)
user_roles (user_id, role_id)
api_keys (id, user_id, key_hash, name, created_at, expires_at)
```

### Token Service

**Responsibilities:**
- Token quota management
- Rate limiting implementation
- Token consumption workflow
- SAGA orchestration
- Event publishing

**Key Components:**

**Controllers:**
- `TokenController`: REST API endpoints
- `TokenPurchaseSagaController`: SAGA management

**Services:**
- `TokenQuotaService`: Quota CRUD operations
- `TokenBucketService`: Rate limiting logic
- `TokenPurchaseSagaService`: SAGA state machine
- `ProviderCallService`: AI provider integration

**Repositories:**
- `TokenPoolRepository`: User quotas (PostgreSQL)
- `OrgTokenPoolRepository`: Organization quotas (PostgreSQL)
- `TokenPurchaseSagaRepository`: SAGA persistence (PostgreSQL)
- `TokenOutboxRepository`: Outbox pattern (PostgreSQL)
- `TokenBucketStore`: Rate limiting data (Redis)

**Scheduled Jobs:**
- `TokenBucketCleanupJob`: Remove expired bucket entries
- `OutboxPublisherJob`: Publish outbox events to Kafka
- `TokenPurchaseSagaRecoveryJob`: Handle stuck SAGAs

### Analytics Service

**Responsibilities:**
- Consume token usage events from Kafka
- Persist events to Cassandra
- Provide aggregation queries
- Detect usage anomalies

**Key Components:**
- `TokenUsageListener`: Kafka consumer
- `AnalyticsService`: Query and aggregation logic
- `AnomalyDetectionService`: Statistical analysis
- `TokenUsageRepository`: Cassandra access

**Cassandra Schema:**
```sql
CREATE TABLE token_usage_events (
    id uuid PRIMARY KEY,
    user_id uuid,
    provider text,
    tokens bigint,
    allowed boolean,
    timestamp timestamp
);

CREATE TABLE token_usage_by_provider (
    provider text,
    timestamp timestamp,
    user_id uuid,
    tokens bigint,
    allowed boolean,
    PRIMARY KEY (provider, timestamp, user_id)
);
```

---

## Data Flow

### Token Consumption Flow

```
┌────────┐    ┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│ Client │───▶│ API Gateway │───▶│Token Service │───▶│   Quota     │
└────────┘    └─────────────┘    └──────────────┘    └─────────────┘
                                       │                      │
                                       │  1. Validate request │
                                       │  2. Check quota      │
                                       │  3. Check rate limit │
                                       │  4. Call provider    │
                                       │  5. Publish event    │
                                       │  6. Update quota     │
                                       ▼                      ▼
                               ┌──────────────┐      ┌─────────────┐
                               │    Kafka     │      │  PostgreSQL │
                               └──────────────┘      └─────────────┘
                                       │
                                       ▼
                               ┌──────────────┐
                               │   Analytics  │
                               │    Service   │
                               └──────────────┘
```

**Detailed Steps:**

1. **Request Validation**: Validate userId format, check required fields
2. **Quota Check**: Query PostgreSQL for user's token pool
3. **Quota Reservation**: Lock row and deduct tokens
4. **Rate Limit Check**: Check Redis token bucket
5. **Provider Call**: Call AI provider stub via HTTP
6. **Event Publishing**: Write to outbox table (transactional)
7. **Database Commit**: Commit quota deduction and outbox entry
8. **Async Event Processing**: Outbox publisher sends to Kafka
9. **Analytics Consumption**: Analytics service writes to Cassandra

### SAGA Purchase Flow

```
┌────────┐     ┌──────────────┐
│ Client │────▶│ Start SAGA   │
└────────┘     └──────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────┐
│ State: STARTED                               │
│ Action: Create SAGA record                   │
│ Event: TOKEN_PURCHASE_STARTED                │
└──────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────┐
│ State: PAYMENT_RESERVED                      │
│ Action: Simulate payment reservation         │
│ On Failure: Emit PAYMENT_RELEASE_REQUESTED   │
└──────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────┐
│ State: TOKENS_ALLOCATED                      │
│ Action: Add tokens to user quota             │
│ On Failure: Revert quota, emit compensation  │
└──────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────┐
│ State: COMPLETED                             │
│ Action: Mark SAGA complete                   │
│ Event: TOKEN_PURCHASE_COMPLETED              │
└──────────────────────────────────────────────┘
```

**SAGA States:**
- `STARTED`: Initial state, record created
- `PAYMENT_RESERVED`: Payment step succeeded
- `TOKENS_ALLOCATED`: Tokens added to quota
- `COMPLETED`: All steps succeeded
- `FAILED`: Step failed, compensation triggered

**Idempotency:**
- Optional `Idempotency-Key` header
- Stored with SAGA record
- Same key + same payload = return existing SAGA
- Same key + different payload = 409 Conflict

---

## Communication Patterns

### Synchronous (REST)

**Use Cases:**
- Client-to-service API calls
- Service-to-service calls (with circuit breaker)
- Authentication validation

**Example:**
```java
@GetMapping("/api/v1/tokens/quota")
public ResponseEntity<QuotaResponse> getQuota(...) {
    // Synchronous database query
    return quotaService.findByUserId(userId);
}
```

### Asynchronous (Events)

**Use Cases:**
- Token usage tracking
- SAGA state transitions
- Analytics data ingestion

**Event Flow:**
```
Token Service ──▶ Outbox Table ──▶ Kafka ──▶ Analytics Service
```

**Outbox Pattern:**
1. Write business data and event to database (same transaction)
2. Separate process polls outbox table
3. Publish events to Kafka
4. Mark events as published

**Benefits:**
- Exactly-once delivery guarantee
- No distributed transaction coordination
- Eventual consistency

### Service Discovery

**Eureka Integration:**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**Service Registration:**
- Services register on startup
- Heartbeat every 30 seconds
- Automatic deregistration on shutdown

**Load Balancing:**
- Ribbon/LoadBalancer for client-side
- `lb://service-name` URI scheme in Gateway

---

## Data Storage

### PostgreSQL (Primary Database)

**Use Cases:**
- User accounts and authentication
- Token quotas (users and organizations)
- SAGA state persistence
- Outbox events

**Key Tables:**
- `users`: User accounts
- `api_keys`: API key metadata
- `token_pools`: User token quotas
- `token_org_pools`: Organization quotas
- `token_purchase_saga`: SAGA state machine
- `token_outbox`: Outbox events

**Configuration:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/token_service
    username: leaky_user
    password: ${DB_PASSWORD}
```

### Redis (Cache & Rate Limiting)

**Use Cases:**
- Token bucket storage
- API key validation cache
- Session storage

**Token Bucket Data Structure:**
```
Key: bucket:{userId}:{provider}
Value: {
  "capacity": 1000,
  "used": 100,
  "lastLeak": 1707144000,
  "createdAt": 1707143000
}
TTL: 6 hours
```

**Configuration:**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
```

### Apache Kafka (Event Streaming)

**Use Cases:**
- Token usage events
- SAGA lifecycle events
- Outbox pattern implementation

**Topics:**
- `token-usage`: Token consumption events
- `token-purchase-events`: SAGA state changes

**Configuration:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: analytics-service
```

### Apache Cassandra (Time-Series)

**Use Cases:**
- Token usage history
- Analytics aggregations
- Time-series data

**Benefits:**
- High write throughput
- Time-based partitioning
- Linear scalability

**Configuration:**
```yaml
spring:
  cassandra:
    contact-points: localhost
    port: 9042
    keyspace-name: analytics
```

---

## Resilience Patterns

### Circuit Breaker

**Implementation:** Resilience4j

**States:**
- `CLOSED`: Normal operation, requests pass through
- `OPEN`: Failure threshold exceeded, requests fail fast
- `HALF_OPEN`: Testing if service recovered

**Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      token-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 5
```

**Usage in Gateway:**
```java
@CircuitBreaker(name = "token-service", fallbackMethod = "fallback")
public ResponseEntity<?> routeToTokenService(...) {
    // Route request
}
```

### Retry Pattern

**Configuration:**
```yaml
resilience4j:
  retry:
    instances:
      provider-call:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
```

### Timeout Pattern

**Configuration:**
```yaml
resilience4j:
  timelimiter:
    instances:
      provider-call:
        timeout-duration: 5s
```

### Bulkhead Pattern

**Purpose:** Isolate failures to prevent resource exhaustion

**Configuration:**
```yaml
resilience4j:
  bulkhead:
    instances:
      token-service:
        max-concurrent-calls: 100
        max-wait-duration: 500ms
```

---

## Security Architecture

### Authentication Flow

#### JWT Authentication

```
┌────────┐    ┌─────────────┐    ┌──────────────┐    ┌────────────┐
│ Client │───▶│ Login/Reg   │───▶│ Auth Server  │───▶│  JWT Token │
└────────┘    └─────────────┘    └──────────────┘    └────────────┘
                                                                       │
                                                                       ▼
┌────────┐    ┌─────────────┐    ┌──────────────┐    ┌────────────┐
│ Client │───▶│   Request   │───▶│ API Gateway  │───▶│  Validate  │
└────────┘    │  + JWT      │    │              │    │  JWKS      │
              └─────────────┘    └──────────────┘    └────────────┘
```

**Token Structure:**
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "username": "myuser",
  "roles": ["USER"],
  "iat": 1707144000,
  "exp": 1707147600
}
```

#### API Key Authentication

```
┌────────┐    ┌─────────────┐    ┌──────────────┐    ┌────────────┐
│ Client │───▶│ Request +   │───▶│ API Gateway  │───▶│  Validate  │
└────────┘    │ X-Api-Key   │    │              │    │  via Auth  │
              └─────────────┘    └──────────────┘    │  Server    │
                                                     └────────────┘
                                                            │
                                                            ▼
                                                     ┌────────────┐
                                                     │   Cache    │
                                                     │  (Redis)   │
                                                     └────────────┘
```

**API Key Format:**
```
leaky_{userId}_{randomHex}
leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4e5f6
```

### Authorization

**Role-Based Access Control (RBAC):**
- `ROLE_USER`: Standard user permissions
- `ROLE_ADMIN`: Administrative permissions

**Method-Level Security:**
```java
@PreAuthorize("hasRole('USER')")
public ResponseEntity<?> consumeTokens(...) { }

@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminOperation(...) { }
```

### Data Protection

**Passwords:**
- BCrypt hashing with strength 10
- Salt generated per password

**API Keys:**
- SHA-256 hash stored in database
- Raw key only returned on creation
- Configurable expiration

**Sensitive Data:**
- PostgreSQL SSL/TLS support
- Encrypted configuration properties
- No sensitive data in logs

---

## Deployment Architecture

### Docker Compose (Development)

```yaml
version: '3.8'
services:
  # Infrastructure
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: leaky_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
  
  redis:
    image: redis:7-alpine
  
  kafka:
    image: confluentinc/cp-kafka:latest
  
  # Services
  token-service:
    build: ./token-service
    depends_on:
      - postgres
      - redis
      - kafka
```

### Kubernetes (Production)

**Deployment Structure:**
```
namespace: leaky-tokens
├── deployment/token-service (3 replicas)
├── service/token-service (ClusterIP)
├── deployment/api-gateway (2 replicas)
├── service/api-gateway (LoadBalancer)
├── configmap/app-config
├── secret/db-credentials
└── hpa/token-service (auto-scaling)
```

**Scaling Strategy:**
- Horizontal Pod Autoscaler (HPA) based on CPU/memory
- Database read replicas for query scaling
- Redis Cluster for cache scaling
- Kafka partitions for event throughput

### High Availability

**Service Level:**
- Multiple replicas per service
- Health checks and readiness probes
- Graceful shutdown handling

**Database Level:**
- PostgreSQL streaming replication
- Automated failover (Patroni)
- Regular backups

**Infrastructure Level:**
- Multi-zone deployment
- Load balancer with health checks
- Circuit breakers prevent cascade failures

---

**Previous**: [User Guide](03-user-guide.md)  
**Next**: [Use Cases →](05-use-cases.md)
