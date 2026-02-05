# Project Overview

## Purpose

Leaky Tokens is a comprehensive token management and rate limiting system designed for modern AI/ML API platforms. It solves the challenge of managing API consumption quotas across multiple users, organizations, and AI providers while ensuring fair resource allocation and system stability.

## Why "Leaky Tokens"?

The name comes from the **Leaky Bucket Algorithm**, a classic rate limiting technique where:
- Requests (tokens) drip into a bucket at a fixed rate
- The bucket has a finite capacity
- Excess requests overflow when the bucket is full
- This ensures smooth traffic flow and prevents bursts

## Key Features

### 1. Multi-Tier Token Management

**User-Level Quotas**
- Individual users have dedicated token pools
- Per-provider quotas (OpenAI, Qwen, Gemini)
- Automatic quota reset windows (default: 24 hours)
- Tier-based capacity caps

**Organization-Level Quotas**
- Shared pools across organization members
- Flexible allocation strategies
- Consolidated billing and reporting

### 2. Advanced Rate Limiting

**Multiple Algorithms**
- **Leaky Bucket** (default): Smooth request flow, prevents bursts
- **Token Bucket**: Allows short bursts within limits
- **Fixed Window**: Simple counter-based limiting

**Flexible Configuration**
- Per-route rate limits
- Configurable key strategies (IP, API Key, User ID)
- Whitelist/blacklist paths
- Tier-based multipliers

### 3. SAGA-Based Purchase Flow

**Distributed Transactions**
- Token purchase workflow with compensation
- Idempotency support for safe retries
- State machine tracking (STARTED → PAYMENT_RESERVED → TOKENS_ALLOCATED → COMPLETED)
- Automatic recovery for failed transactions

### 4. Real-Time Analytics

**Usage Tracking**
- Per-user, per-provider consumption metrics
- Time-windowed aggregations
- Top consumers reporting
- Historical trend analysis

**Anomaly Detection**
- Statistical baseline comparison
- Configurable threshold multipliers
- Spike detection and alerting

### 5. Enterprise Security

**Authentication**
- JWT tokens with RSA signature
- API key authentication for service-to-service
- Configurable token expiration

**Authorization**
- Role-based access control (RBAC)
- User isolation enforcement
- Admin capabilities for key management

## Business Value

### For AI Platform Providers
- **Monetization**: Flexible quota-based pricing models
- **Fair Usage**: Prevent resource hogging by single users
- **Capacity Planning**: Real-time visibility into consumption patterns
- **Customer Satisfaction**: Graceful degradation during high load

### For Enterprise IT
- **Cost Control**: Prevent unexpected API billing spikes
- **Governance**: Track and audit AI service usage
- **Compliance**: Detailed logs and audit trails
- **Integration**: Seamless integration with existing identity systems

### For Development Teams
- **Learning Resource**: Production-grade microservices patterns
- **Best Practices**: SAGA, Outbox, Circuit Breaker implementations
- **Observability**: Complete monitoring stack setup
- **Scalability**: Horizontal scaling patterns

## Architecture Philosophy

### Cloud-Native Design
- **12-Factor App** methodology
- Stateless services with externalized configuration
- Container-ready with Docker support
- Kubernetes deployment ready

### Resilience Patterns
- **Circuit Breaker**: Fail fast when downstream services fail
- **Retry with Backoff**: Automatic recovery from transient failures
- **Bulkhead**: Isolate failures to prevent cascade
- **Timeout**: Prevent resource exhaustion

### Data Consistency
- **SAGA Pattern**: Manage distributed transactions
- **Outbox Pattern**: Ensure event delivery
- **Event Sourcing**: Complete audit trail
- **CQRS**: Separate read/write models

## Technology Stack

### Core Technologies
| Component | Technology |
|-----------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.x |
| Build Tool | Gradle Kotlin DSL |
| API Gateway | Spring Cloud Gateway |
| Service Discovery | Netflix Eureka |
| Configuration | Spring Cloud Config |

### Data Storage
| Service | Technology |
|---------|------------|
| Primary Database | PostgreSQL |
| Cache | Redis |
| Event Store | Apache Kafka |
| Time-Series | Apache Cassandra |

### Observability
| Component | Technology |
|-----------|------------|
| Metrics | Prometheus |
| Visualization | Grafana |
| Distributed Tracing | Jaeger |
| Logging | Centralized Log Aggregation |

### Development Tools
| Tool | Purpose |
|------|---------|
| Lombok | Boilerplate reduction |
| MapStruct | Object mapping |
| OpenAPI | API documentation |
| JUnit 5 | Testing framework |

## Service Overview

### API Gateway (Port 8080)
The entry point for all client requests. Handles:
- Request routing to backend services
- Rate limiting at the edge
- API key authentication
- JWT validation
- Circuit breaker pattern

### Auth Server (Port 8081)
Identity and access management service:
- User registration and authentication
- JWT token issuance
- API key management
- JWKS endpoint for public key distribution

### Token Service (Port 8082)
Core business logic service:
- Token quota management
- Leaky bucket rate limiting
- Token consumption workflow
- SAGA orchestration for purchases
- Outbox event publishing

### Analytics Service (Port 8083)
Usage tracking and reporting:
- Kafka event consumption
- Cassandra data persistence
- Usage aggregation queries
- Anomaly detection
- Report generation

### Service Discovery (Port 8761)
Eureka server for service registration:
- Service health monitoring
- Dynamic service discovery
- Load balancing support

### Config Server (Port 8888)
Centralized configuration management:
- Environment-specific configs
- Real-time configuration updates
- Encrypted property support

### Provider Stubs (Ports 8091-8093)
Mock AI providers for testing:
- Qwen Stub (8091)
- Gemini Stub (8092)
- OpenAI Stub (8093)

## Data Flow

### Token Consumption Flow

```
┌─────────────┐     ┌──────────────┐     ┌────────────────┐
│   Client    │────▶│ API Gateway  │────▶│ Token Service  │
└─────────────┘     └──────────────┘     └────────────────┘
                                                  │
                                                  ▼
                           ┌────────────────────────────────────┐
                           │  1. Validate request               │
                           │  2. Check quota (PostgreSQL)       │
                           │  3. Apply rate limit (Redis)       │
                           │  4. Call provider stub             │
                           │  5. Publish event (Kafka)          │
                           │  6. Update quota                   │
                           └────────────────────────────────────┘
                                                  │
                                                  ▼
                                        ┌──────────────────┐
                                        │ Return response  │
                                        └──────────────────┘
```

### SAGA Purchase Flow

```
┌─────────────┐     ┌─────────────────┐
│   Client    │────▶│  Start Purchase │
└─────────────┘     └─────────────────┘
                             │
                             ▼
                    ┌────────────────┐
                    │ SAGA: STARTED  │
                    └────────────────┘
                             │
                             ▼
                    ┌────────────────────┐
                    │ Reserve Payment    │
                    │ SAGA: PAYMENT_RESERVED│
                    └────────────────────┘
                             │
                             ▼
                    ┌────────────────────┐
                    │ Allocate Tokens    │
                    │ SAGA: TOKENS_ALLOCATED│
                    └────────────────────┘
                             │
                             ▼
                    ┌────────────────┐
                    │ SAGA: COMPLETED│
                    └────────────────┘
```

### Event Flow

```
┌────────────────┐     ┌─────────────┐     ┌──────────────────┐
│ Token Service  │────▶│   Kafka     │────▶│ Analytics Service│
│  (Publisher)   │     │  (Outbox)   │     │  (Consumer)      │
└────────────────┘     └─────────────┘     └──────────────────┘
                                                    │
                                                    ▼
                                           ┌────────────────┐
                                           │   Cassandra    │
                                           └────────────────┘
```

## Use Cases

### 1. AI Platform Subscription Management
A SaaS company provides AI services to customers. Leaky Tokens manages:
- Per-customer API quotas based on subscription tier
- Usage tracking for billing
- Rate limiting to ensure fair resource sharing
- Purchase workflows for additional tokens

### 2. Enterprise AI Governance
A large enterprise wants to control AI service usage:
- Department-level quotas
- User authentication via corporate SSO
- Audit trails for compliance
- Cost allocation by department

### 3. Multi-Tenant AI Platform
A platform provider serves multiple tenants:
- Tenant isolation
- Custom rate limits per tenant
- White-label API key management
- Tenant-specific analytics

## Getting Help

- **Installation Issues**: See [Getting Started](02-getting-started.md)
- **API Usage**: See [User Guide](03-user-guide.md) and [API Reference](08-api-reference.md)
- **Configuration**: See [Configuration Guide](06-configuration.md)
- **Monitoring**: See [Monitoring & Observability](07-monitoring.md)
- **Debugging**: See [Troubleshooting](09-troubleshooting.md)

---

**Next Steps**: [Getting Started Guide →](02-getting-started.md)
