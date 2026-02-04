# Microservices Design - Leaky Tokens Project

## Service Overview

The Leaky Tokens project consists of 6 core microservices that work together to provide a comprehensive token-based API gateway system:

1. **API Gateway Service** - Entry point for all client requests
2. **Authorization Server** - Authentication and authorization
3. **Configuration Server** - Centralized configuration management
4. **Service Discovery** - Eureka-based service registry
5. **Token Service** - Core token management and rate limiting
6. **Analytics Service** - Metrics and analytics processing

## Service Specifications

### 1. API Gateway Service

#### Purpose
Acts as the central entry point for all client requests, providing routing, cross-cutting concerns, and security enforcement.

#### Technology Stack
- Spring Cloud Gateway
- Spring Security
- Resilience4j (Circuit Breaker)
- Spring Boot Actuator

#### Responsibilities
- Route requests to appropriate microservices
- Authenticate and authorize incoming requests
- Apply rate limiting and security policies
- Implement circuit breaker patterns
- Provide request/response logging
- Handle CORS and SSL termination

#### Endpoints
- `/api/v1/**` - Routes to backend services
- `/actuator/**` - Health and metrics endpoints
- `/oauth/**` - OAuth2 endpoints
- `/rate-limit/**` - Rate limiting configuration

#### Configuration Properties
```yaml
server:
  port: 8080
  
spring:
  cloud:
    gateway:
      routes:
        - id: token-service
          uri: lb://token-service
          predicates:
            - Path=/api/token/**
        - id: analytics-service
          uri: lb://analytics-service
          predicates:
            - Path=/api/analytics/**
  
resilience4j:
  circuitbreaker:
    instances:
      token-service:
        sliding-window-size: 100
        failure-rate-threshold: 50
```

#### Dependencies
- Service Discovery (Eureka)
- Configuration Server
- Token Service
- Analytics Service

#### Data Stores
- None (stateless service)

#### Health Indicators
- Service discovery connectivity
- Upstream service connectivity
- Circuit breaker status

---

### 2. Authorization Server

#### Purpose
Provides OAuth2/OIDC compliant authentication and authorization services for the entire system.

#### Technology Stack
- Spring Security OAuth2
- Spring Data JPA
- Redis (for token storage)
- BCrypt (password hashing)

#### Responsibilities
- Issue and validate JWT tokens
- Manage user authentication
- Handle client credentials
- Provide user registration/login
- Manage roles and permissions
- API key generation and validation

#### Endpoints
- `/oauth/token` - Token issuance
- `/oauth/revoke` - Token revocation
- `/api/users/**` - User management
- `/api/keys/**` - API key management
- `/actuator/**` - Health and metrics

#### Configuration Properties
```yaml
server:
  port: 8081
  
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/auth_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  redis:
    host: redis
    port: 6379
    
jwt:
  secret: ${JWT_SECRET}
  expiry: 3600
```

#### Dependencies
- PostgreSQL (for user data)
- Redis (for token caching)
- Service Discovery

#### Data Stores
- PostgreSQL (users, roles, clients)
- Redis (active tokens, sessions)

#### Health Indicators
- Database connectivity
- Redis connectivity
- Token validation service

---

### 3. Configuration Server

#### Purpose
Centralized configuration management for all microservices in the system.

#### Technology Stack
- Spring Cloud Config Server
- Git (configuration storage)
- Spring Security

#### Responsibilities
- Serve configuration properties to services
- Support encrypted properties
- Refresh configuration at runtime
- Validate configuration schemas

#### Endpoints
- `/{application}/{profile}[/{label}]` - Configuration retrieval
- `/{application}-{profile}.properties` - Properties format
- `/{application}-{profile}.yml` - YAML format
- `/actuator/refresh` - Configuration refresh

#### Configuration Properties
```yaml
server:
  port: 8888
  
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/company/config-repo
          searchPaths: '{application}'
          
encrypt:
  keyStore:
    location: classpath:keystore.jks
    alias: my-key
    password: ${KEYSTORE_PASSWORD}
    secret: ${KEY_PASSWORD}
```

#### Dependencies
- Git repository (configuration source)
- Service Discovery

#### Data Stores
- Git repository (configuration files)

#### Health Indicators
- Git repository connectivity
- Configuration validation status

---

### 4. Service Discovery (Eureka Server)

#### Purpose
Provides service registration and discovery capabilities for the microservices ecosystem.

#### Technology Stack
- Netflix Eureka Server
- Spring Boot

#### Responsibilities
- Register and deregister services
- Maintain service registry
- Provide service health monitoring
- Enable client-side load balancing

#### Endpoints
- `/eureka/apps` - Service registry
- `/eureka/status` - Server status
- `/actuator/**` - Health and metrics

#### Configuration Properties
```yaml
server:
  port: 8761
  
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 1000
```

#### Dependencies
- None (bootstrap service)

#### Data Stores
- In-memory registry (with peer replication capability)

#### Health Indicators
- Self-health status
- Peer synchronization status

---

### 5. Token Service

#### Purpose
Implements the core token management and rate limiting functionality using the leaky bucket algorithm.

#### Technology Stack
- Spring Boot
- Spring Data JPA
- Redis (for rate limiting counters)
- Apache Kafka
- Resilience4j

#### Responsibilities
- Implement leaky bucket algorithm
- Manage token pools for different API providers
- Track token consumption
- Enforce rate limiting policies
- Publish token usage events
- Handle token expiration

#### Endpoints
- `/api/v1/tokens/consume` - Consume tokens
- `/api/v1/tokens/status` - Check token status
- `/api/v1/providers/{provider}/quota` - Provider quota management
- `/api/v1/rate-limits/{user}` - User rate limit info
- `/actuator/**` - Health and metrics

#### Tier & Organization Quotas
- **Tiered quotas**: Derived from JWT roles (`USER`, `ADMIN`) and mapped to tier configs in
  `token.tiers` (capacity/leak multipliers + quota caps).
- **Organization quotas**: Optional `orgId` shared pool. If `orgId` is provided, org-level quotas
  are applied instead of per-user quotas.

**Examples**

```
POST /api/v1/tokens/consume
{
  "userId": "00000000-0000-0000-0000-000000000001",
  "orgId": "10000000-0000-0000-0000-000000000001",
  "provider": "openai",
  "tokens": 25,
  "prompt": "hello"
}
```

```
GET /api/v1/tokens/quota/org?orgId=10000000-0000-0000-0000-000000000001&provider=openai
```

#### Configuration Properties
```yaml
server:
  port: 8082
  
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/token_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    
  redis:
    host: redis
    port: 6379
    
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: token-service-group
      
bucket:
  capacity: 1000
  leak-rate: 10  # tokens per second
  refill-strategy: linear
  
providers:
  qwen:
    default-quota: 1000
    rate-limit: 10
  gemini:
    default-quota: 500
    rate-limit: 5
  openai:
    default-quota: 2000
    rate-limit: 20
```

#### Dependencies
- PostgreSQL (for token records)
- Redis (for rate limiting)
- Kafka (for event publishing)
- Service Discovery

#### Data Stores
- PostgreSQL (token records, user quotas)
- Redis (rate limiting counters, active sessions)

#### Health Indicators
- Database connectivity
- Redis connectivity
- Kafka producer status
- Token pool health

#### SAGA Implementation
- Token purchase workflow using choreography pattern
- Compensation for failed purchases
- Event-driven coordination

#### Transactional Outbox
- Store token usage events in database
- Publish events to Kafka asynchronously
- Ensure at-least-once delivery

---

### 6. Analytics Service

#### Purpose
Processes usage events, generates analytics, and provides insights into system performance and business metrics.

#### Technology Stack
- Spring Boot
- Apache Kafka
- Apache Cassandra
- Spring Data Cassandra

#### Responsibilities
- Consume token usage events from Kafka
- Process and aggregate metrics
- Generate usage reports
- Store time-series data in Cassandra
- Provide analytical endpoints

#### Endpoints
- `/api/v1/analytics/usage` - Usage statistics
- `/api/v1/analytics/providers` - Provider utilization
- `/api/v1/analytics/users/{userId}` - User analytics
- `/api/v1/analytics/trends` - Trend analysis
- `/actuator/**` - Health and metrics

#### Configuration Properties
```yaml
server:
  port: 8083
  
spring:
  cassandra:
    contact-points: cassandra:9042
    keyspace-name: analytics_keyspace
    schema-action: CREATE_IF_NOT_EXISTS
    
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: analytics-service-group
      auto-offset-reset: earliest
      
analytics:
  retention-period: 365  # days
  aggregation-intervals:
    - 1MINUTE
    - 5MINUTE
    - 1HOUR
    - 1DAY
```

#### Dependencies
- Apache Cassandra (for time-series data)
- Kafka (for consuming events)
- Service Discovery

#### Data Stores
- Apache Cassandra (time-series metrics, logs, analytics data)

#### Health Indicators
- Cassandra connectivity
- Kafka consumer status
- Event processing lag

#### Data Models in Cassandra
```sql
-- Time-series metrics table
CREATE TABLE usage_metrics (
    metric_type text,
    timestamp timestamp,
    provider text,
    user_id uuid,
    tokens_consumed int,
    PRIMARY KEY ((metric_type, provider), timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);

-- Daily aggregated metrics
CREATE TABLE daily_aggregates (
    date date,
    provider text,
    user_id uuid,
    total_tokens int,
    request_count int,
    PRIMARY KEY ((date, provider), user_id)
);
```

---

## Inter-Service Communication

### Synchronous Communication
- API Gateway → Token Service: Token consumption requests
- API Gateway → Analytics Service: Analytics queries
- Token Service → Authorization Server: Token validation

### Asynchronous Communication
- Token Service → Kafka: Token usage events
- Analytics Service ← Kafka: Token usage events (consumption)
- Various services → Kafka: System events for monitoring

### Data Flow
1. Client requests arrive at API Gateway
2. Gateway authenticates via Authorization Server
3. Gateway routes to Token Service for rate limiting
4. Token Service validates and consumes tokens
5. Token Service publishes usage events to Kafka
6. Analytics Service consumes events and stores in Cassandra
7. Analytics Service provides insights via API

## Security Considerations

### Service-to-Service Communication
- Mutual TLS (mTLS) for inter-service communication
- OAuth2 client credentials for service authentication
- JWT tokens for request authentication

### Data Protection
- Encryption at rest for sensitive data
- Encryption in transit for all communications
- Secure credential management

### API Security
- Rate limiting at gateway level
- Input validation and sanitization
- Proper authentication and authorization checks

## Scalability Considerations

### Horizontal Scaling
- Stateless services where possible
- Load balancing via service discovery
- Database connection pooling
- Caching strategies

### Performance Optimization
- Redis caching for frequently accessed data
- Asynchronous processing for heavy operations
- Connection pooling for databases
- Efficient data models in Cassandra

This microservices design provides a robust foundation for implementing the Leaky Tokens project with all required features and patterns.
