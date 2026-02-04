# Leaky Tokens - Microservices Architecture Showcase

## WHY?

This is an auto-generated project by AI agents. I already have experience with all of these technologies, but nobody
cares if you don’t have a GitHub repo with a pet project—so here it is.

Why do I use AI agents to generate projects? Because it’s fun and I’m bored. Do I really work with all of these
technologies? Yep. Ten years in, that’s the answer.

But you still have to write code yourself, because AI agents are not perfect and can’t write everything for you. And
this project doesn’t show your skills.
Well, that’s the new world. I worked hard before. I wrote code day and night, but times have changed.

## Project Overview

Leaky Tokens is a comprehensive microservices-based demonstration project showcasing advanced Java/Spring Boot
capabilities for Senior+/Staff level developers. The project simulates a token-based API gateway system that connects to
various paid token-based services like Qwen API, Gemini API, and OpenAI API through custom stub implementations.

## Goals

- Demonstrate advanced microservices architecture patterns
- Showcase enterprise-grade infrastructure with monitoring and observability
- Implement complex distributed systems concepts (SAGA, Transactional Outbox, etc.)
- Provide a complete development environment using Docker Compose
- Show proficiency in modern Java 25, Spring Boot, and cloud-native technologies

## Key Features

- **Microservices Architecture**: 5+ interconnected services with service discovery
- **Token Management**: Leaky bucket algorithm implementation for rate limiting
- **Token Quotas**: Persistent per-user/provider token pools with reservation and release
- **Organization Quotas**: Optional shared org-level token pools for multi-tenant usage
- **Priority Tiers**: Role-based tiering (e.g., USER/ADMIN) for quota caps and rate limits
- **Advanced Rate Limiting**: Leaky bucket, fixed window, and token bucket strategies
- **Service Mesh**: Circuit breaker and resilience patterns
- **Data Storage**: PostgreSQL, Redis, and Apache Cassandra integration
- **Message Queuing**: Kafka for asynchronous communication
- **Security**: Authorization server for resource protection + API key management
- **Security Headers**: Common security headers across gateway, core services, stub providers, and infrastructure
  services
- **Observability**: Prometheus metrics, Grafana visualization, and distributed tracing
- **Configuration Management**: Centralized config server

## Technologies Stack

### Backend

- Java 25
- Spring Boot 4.x
- Spring Cloud Gateway
- Spring Security OAuth2
- Spring Data JPA
- Spring Kafka

### Infrastructure

- Docker & Docker Compose
- PostgreSQL
- Redis
- Apache Kafka
- Apache Cassandra
- Prometheus
- Grafana
- Zipkin/Jaeger (for distributed tracing)

### Development Tools

- Gradle Kotlin DSL
- OpenAPI (Swagger)
- Lombok
- MapStruct

## Architecture Components

- **API Gateway**: Entry point for all client requests
- **Authorization Server**: OAuth2/OIDC compliant authentication
- **Config Server**: Centralized configuration management
- **Service Discovery**: Eureka-based service registry
- **Token Service**: Core leaky bucket implementation
- **Analytics Service**: Metrics and analytics processing

## Getting Started

### Prerequisites

- Java 25
- Docker & Docker Compose
- Gradle

### Running the Application

```bash
# Clone the repository
git clone <repository-url>
cd leaky-tokens

# Start the infrastructure
docker-compose up -d

# Build and run the services
./gradlew bootRun
```

### Local Dev (Service-Only)

For quick local boot without databases/Kafka/Cassandra, use the included `docker-compose.yml`.
It runs each service with the `local` Spring profile where data/messaging autoconfig is disabled.

```bash
docker-compose up
```

### Local Dev (Infrastructure)

To boot infra dependencies plus Eureka service discovery for local profiles, use `docker-compose.infra.yml`.

```bash
docker-compose -f docker-compose.infra.yml up -d
```

This brings up Cassandra/Postgres/Redis/Kafka/ZooKeeper and the Eureka server on `http://localhost:8761`.

### Local Dev (Full Stack)

To run services plus Kafka/Redis/Postgres/Cassandra together (and publish token usage to Kafka), use:

```bash
docker-compose -f docker-compose.full.yml up
```

This uses `application-docker.yml` for `auth-server`, `token-service`, and `analytics-service`.

### Observability Stack

To run Prometheus + Grafana:

```bash
docker-compose -f docker-compose.observability.yml up -d
```

Prometheus: http://localhost:9090  
Grafana: http://localhost:3000 (admin/admin)

Grafana is provisioned with a starter dashboard: "Leaky Tokens Overview".

### Tracing (Jaeger)

To run Jaeger (OTLP):

```bash
docker-compose -f docker-compose.tracing.yml up -d
```

Jaeger UI: http://localhost:16686

### Stub Provider APIs

Once the full stack is running, the stub providers are available via the gateway:

```bash
curl -s -X POST http://localhost:8080/api/v1/qwen/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"hello"}' | jq

curl -s -X POST http://localhost:8080/api/v1/gemini/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"hello"}' | jq

curl -s -X POST http://localhost:8080/api/v1/openai/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"hello"}' | jq
```

### Analytics Smoke Test

With the full stack running, you can post a token usage event and read it back:

```bash
./scripts/analytics-smoke.sh openai 5
```

### Kafka Consumer Helper

To watch raw `token-usage` events on Kafka:

```bash
./scripts/kafka-consume-token-usage.sh
```

### Smoke Tests

Run the services (at least `service-discovery`, `config-server`, `api-gateway`, `token-service`, `auth-server`,
`analytics-service`) and then:

```bash
# Direct service calls
curl -s http://localhost:8082/api/v1/tokens/status | jq
curl -s http://localhost:8081/api/v1/auth/health | jq
curl -s http://localhost:8083/api/v1/analytics/health | jq

# Token consume (direct)
curl -s -X POST http://localhost:8082/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -d '{"userId":"00000000-0000-0000-0000-000000000001","provider":"openai","tokens":100}' | jq

# Token quota
curl -s "http://localhost:8082/api/v1/tokens/quota?userId=00000000-0000-0000-0000-000000000001&provider=openai" | jq

# Via API Gateway (routes)
curl -s http://localhost:8080/api/v1/tokens/status | jq
curl -s http://localhost:8080/api/v1/auth/health | jq
curl -s http://localhost:8080/api/v1/analytics/health | jq

# Token consume (gateway)
curl -s -X POST http://localhost:8080/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -d '{"userId":"00000000-0000-0000-0000-000000000001","provider":"openai","tokens":100}' | jq

# Actuator info
curl -s http://localhost:8082/actuator/info | jq
curl -s http://localhost:8081/actuator/info | jq
curl -s http://localhost:8083/actuator/info | jq
```

### API Key Management

Create, list, and revoke API keys using the auth server:

```bash
# Create API key
curl -s -X POST http://localhost:8081/api/v1/auth/api-keys \
  -H "Content-Type: application/json" \
  -d '{"userId":"00000000-0000-0000-0000-000000000001","name":"cli-key"}' | jq

# List API keys
curl -s "http://localhost:8081/api/v1/auth/api-keys?userId=00000000-0000-0000-0000-000000000001" | jq

# Revoke API key
curl -s -X DELETE "http://localhost:8081/api/v1/auth/api-keys?userId=00000000-0000-0000-0000-000000000001&apiKeyId=<api-key-id>"
```

### API Key Validation (Gateway)

The gateway can validate API keys for incoming requests. By default it expects `X-Api-Key` and calls
`/api/v1/auth/api-keys/validate` on the auth server, then forwards `X-User-Id` downstream when valid.
Validations are cached in-memory to reduce auth-server calls.

Example:

```bash
curl -s -X POST http://localhost:8080/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: <your-api-key>" \
  -d '{"userId":"00000000-0000-0000-0000-000000000001","provider":"openai","tokens":100}' | jq
```

### Tiered Quotas (USER/ADMIN)

Tiers are resolved from JWT roles and can override quota caps and rate limits.
Defaults are configured in `config-server/src/main/resources/config/token-service*.yml` under `token.tiers`.

Example (config excerpt):

```
token:
  tiers:
    default-tier: USER
    levels:
      USER:
        priority: 10
        bucket-capacity-multiplier: 1.0
        bucket-leak-rate-multiplier: 1.0
        quota-max-tokens: 100000
      ADMIN:
        priority: 100
        bucket-capacity-multiplier: 2.0
        bucket-leak-rate-multiplier: 2.0
        quota-max-tokens: 500000
```

#### Tier/Org Quota Usage (API Examples)

Obtain a JWT (roles default to `USER` on registration):

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"quota-user","email":"quota-user@example.com","password":"password"}' | jq -r .token)
```

Check user quota:

```bash
curl -s "http://localhost:8082/api/v1/tokens/quota?userId=00000000-0000-0000-0000-000000000001&provider=openai" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

Consume tokens (user quota):

```bash
curl -s -X POST http://localhost:8082/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"userId":"00000000-0000-0000-0000-000000000001","provider":"openai","tokens":25}' | jq
```

Consume tokens (org quota):

```bash
curl -s -X POST http://localhost:8082/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"userId":"00000000-0000-0000-0000-000000000001","orgId":"10000000-0000-0000-0000-000000000001","provider":"openai","tokens":25}' | jq
```

Org quota lookup:

```bash
curl -s "http://localhost:8082/api/v1/tokens/quota/org?orgId=10000000-0000-0000-0000-000000000001&provider=openai" \
  -H "Authorization: Bearer ${TOKEN}" | jq
```

### Organization Quotas

If `orgId` is provided, the token service applies org-level quotas instead of user-level quotas.
You can also query org quotas directly.

```bash
# Consume using org quota
curl -s -X POST http://localhost:8082/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -d '{"userId":"00000000-0000-0000-0000-000000000001","orgId":"10000000-0000-0000-0000-000000000001","provider":"openai","tokens":25}' | jq

# Org quota lookup
curl -s "http://localhost:8082/api/v1/tokens/quota/org?orgId=10000000-0000-0000-0000-000000000001&provider=openai" | jq

# Purchase tokens into an org pool (saga)
curl -s -X POST http://localhost:8082/api/v1/tokens/purchase \
  -H "Content-Type: application/json" \
  -d '{"userId":"00000000-0000-0000-0000-000000000001","orgId":"10000000-0000-0000-0000-000000000001","provider":"openai","tokens":1000}' | jq
```

### Rate Limiting Strategies

The token service supports multiple rate limiting strategies via configuration:

```
token.bucket.strategy=LEAKY_BUCKET
token.bucket.capacity=1000
token.bucket.leak-rate-per-second=10.0
```

```
token.bucket.strategy=FIXED_WINDOW
token.bucket.capacity=1000
token.bucket.window-seconds=60
```

```
token.bucket.strategy=TOKEN_BUCKET
token.bucket.capacity=1000
token.bucket.leak-rate-per-second=10.0
```

### Performance Baseline (Gatling)

The `performance-tests` module contains a baseline Gatling simulation for token consumption.

```bash
./gradlew :performance-tests:gatlingRun
```

You can override defaults:

```bash
./gradlew :performance-tests:gatlingRun \
  -DbaseUrl=http://localhost:8080 \
  -DapiKey=<your-api-key> \
  -Dusers=50 \
  -DrampSeconds=10 \
  -DdurationSeconds=30
```

Additional simulations:

```bash
# Auth login
./gradlew :performance-tests:gatlingRun -Dgatling.simulationClass=com.leaky.tokens.perf.AuthLoginSimulation \
  -DauthBaseUrl=http://localhost:8081 \
  -Dusername=user1 \
  -Dpassword=password

# Analytics query
./gradlew :performance-tests:gatlingRun -Dgatling.simulationClass=com.leaky.tokens.perf.AnalyticsQuerySimulation \
  -DanalyticsBaseUrl=http://localhost:8083 \
  -Dprovider=openai \
  -Dlimit=10 \
  -DbearerToken=<jwt>

# Token purchase saga
./gradlew :performance-tests:gatlingRun -Dgatling.simulationClass=com.leaky.tokens.perf.TokenPurchaseSagaSimulation \
  -DbaseUrl=http://localhost:8082 \
  -Dusers=20 \
  -DrampSeconds=10 \
  -DdurationSeconds=30

# Token quota check
./gradlew :performance-tests:gatlingRun -Dgatling.simulationClass=com.leaky.tokens.perf.TokenQuotaCheckSimulation \
  -DbaseUrl=http://localhost:8082 \
  -DuserId=00000000-0000-0000-0000-000000000001 \
  -Dprovider=openai

# Token usage publish (consume)
./gradlew :performance-tests:gatlingRun -Dgatling.simulationClass=com.leaky.tokens.perf.TokenUsagePublishSimulation \
  -DbaseUrl=http://localhost:8082 \
  -DapiKey=<your-api-key>
```

## Documentation

- [Feature List](feature_list.md) - Detailed features to implement
- [Architecture Overview](architecture_overview.md) - System design and architecture
- [Microservices Design](microservices_design.md) - Individual service specifications
- [Infrastructure Plan](infrastructure_plan.md) - Docker Compose and monitoring setup
- [Progress Tracker](implementation_progress_tracker.md) - Development progress tracking

## License

This project is licensed under the MIT License - see the LICENSE file for details.
