# Architecture Overview - Leaky Tokens Project

## System Architecture

The Leaky Tokens project follows a microservices architecture pattern with the following key components:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client Apps   │    │   External      │    │   Monitoring    │
│                 │    │   Services      │    │   Systems       │
│  (Web/Mobile)   │    │ (Qwen/Gemini/   │    │ (Prometheus/    │
│                 │    │   OpenAI Stubs) │    │   Grafana)      │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway                                │
│    (Spring Cloud Gateway)                                       │
└─────────────────┬───────────────────────────────────────────────┘
                  │
        ┌─────────▼─────────┐
        │ Service Discovery │
        │   (Eureka)        │
        └─────────┬─────────┘
                  │
    ┌─────────────┼─────────────┐
    │             │             │
┌───▼───┐    ┌────▼────┐   ┌───▼───┐
│ Auth  │    │ Config  │   │ Kafka │
│Server │    │ Server  │   │       │
└───────┘    └─────────┘   └───┬───┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
     ┌───▼───┐             ┌───▼─────┐             ┌───▼────┐
     │Token  │             │Analytics│             │Other   │
     │Service│             │Service  │             │Services│
     └───┬───┘             └───┬─────┘             └───┬────┘
         │                     │                       │
     ┌───▼─────────────────────▼───────────────────────▼───┐
     │                                                     │
     │                Data Layer                           │
     │  ┌──────────┐  ┌─────────┐ ┌──────────────────┐     │
     │  │PostgreSQL│  │  Redis  │ │ Apache Cassandra │     │
     │  │          │  │         │ │                  │     │
     │  └──────────┘  └─────────┘ └──────────────────┘     │
     └─────────────────────────────────────────────────────┘
```

## Component Descriptions

### 1. API Gateway
- **Technology**: Spring Cloud Gateway
- **Responsibilities**:
  - Request routing to appropriate microservices
  - Cross-cutting concerns (authentication, rate limiting, logging)
  - SSL termination
  - Request/response transformation
  - Circuit breaking

### 2. Service Discovery (Eureka Server)
- **Technology**: Netflix Eureka
- **Responsibilities**:
  - Service registration and discovery
  - Health monitoring of services
  - Load balancing between service instances

### 3. Configuration Server
- **Technology**: Spring Cloud Config
- **Responsibilities**:
  - Centralized configuration management
  - Dynamic configuration updates
  - Environment-specific configurations

### 4. Authorization Server
- **Technology**: Spring Security OAuth2
- **Responsibilities**:
  - User authentication
  - Token generation and validation
  - Role-based access control
  - API key management

### 5. Token Service
- **Technology**: Spring Boot
- **Responsibilities**:
  - Implements leaky bucket algorithm
  - Manages token pools for different API providers
  - Tracks token consumption
  - Enforces rate limiting
  - Applies tier-based quotas (USER/ADMIN) and org-level shared quotas

#### Tiered & Org Quotas
Tier selection is derived from JWT roles and mapped to `token.tiers`.
If an `orgId` is supplied, org-level quotas are used instead of per-user quotas.

Example requests:

```
POST /api/v1/tokens/consume
{
  "userId": "00000000-0000-0000-0000-000000000001",
  "orgId": "10000000-0000-0000-0000-000000000001",
  "provider": "openai",
  "tokens": 25
}
```

```
GET /api/v1/tokens/quota/org?orgId=10000000-0000-0000-0000-000000000001&provider=openai
```

### 6. Analytics Service
- **Technology**: Spring Boot + Kafka
- **Responsibilities**:
  - Processes usage events
  - Generates analytics and reports
  - Stores metrics in time-series database

### 7. Data Layer
#### PostgreSQL
- **Purpose**: Primary relational database
- **Use Cases**: User accounts, subscriptions, configurations, transactional data

#### Redis
- **Purpose**: In-memory caching and session store
- **Use Cases**: Session management, cache for frequently accessed data, rate limiting counters

#### Apache Cassandra
- **Purpose**: Distributed NoSQL database
- **Use Cases**: Time-series data, logs, metrics, audit trails

### 8. Messaging Layer (Kafka)
- **Technology**: Apache Kafka
- **Responsibilities**:
  - Event-driven communication between services
  - Transactional outbox implementation
  - Async processing of background tasks

## Communication Patterns

### Synchronous Communication
- REST APIs between services via API Gateway
- Service discovery lookup
- Direct database queries

### Asynchronous Communication
- Event-driven communication via Kafka
- Saga pattern coordination
- Background job processing

## Security Architecture

```
┌─────────────────┐
│   Client        │
└─────────┬───────┘
          │ HTTPS
          ▼
┌─────────────────┐
│   API Gateway   │ ← OAuth2/JWT Validation
└─────────┬───────┘
          │ Internal Communication
          ▼
┌─────────────────┐
│ Microservices   │ ← Service-to-Service Auth
└─────────┬───────┘
          │ Data Access
          ▼
┌─────────────────┐
│ Data Layer      │ ← Encrypted Connections
└─────────────────┘
```

## Deployment Architecture

### Container Orchestration
- **Docker Compose**: Single-file orchestration for local development
- **Service Dependencies**: Proper startup ordering with health checks
- **Resource Management**: Memory and CPU constraints per service

### Monitoring Stack
- **Prometheus**: Metrics collection
- **Grafana**: Visualization and dashboards
- **Jaeger/Zipkin**: Distributed tracing
- **ELK Stack**: Log aggregation and analysis

## Resilience Patterns

### Circuit Breaker
- Implemented using Spring Cloud Circuit Breaker
- Prevents cascade failures
- Provides fallback mechanisms

### Retry & Timeout
- Configurable retry policies
- Exponential backoff
- Timeout handling

### Bulkhead
- Resource isolation between services
- Thread pool separation
- Semaphore-based concurrency limits

## Data Consistency Patterns

### SAGA Pattern
- Choreography-based SAGA implementation
- Compensation transactions for rollback
- Event-driven coordination

### Transactional Outbox
- Events stored in database transactionally
- Separate process publishes events to Kafka
- Ensures at-least-once delivery

## Scaling Strategy

### Horizontal Scaling
- Statelessness of services
- Load balancing via service discovery
- Database sharding strategies

### Vertical Scaling
- Resource allocation per service based on requirements
- Caching strategies to reduce load
- Asynchronous processing for heavy operations

## Technology Stack Rationale

### Why Java 25?
- Latest language features and performance improvements
- Strong ecosystem for enterprise applications
- Excellent tooling and community support

### Why Spring Boot?
- Rapid development and prototyping
- Extensive integration with cloud-native technologies
- Mature ecosystem for microservices

### Why Docker Compose?
- Simple orchestration for local development
- Reproducible environments
- Easy to extend for production scenarios

This architecture provides a solid foundation for demonstrating advanced microservices concepts while maintaining scalability, reliability, and maintainability.
