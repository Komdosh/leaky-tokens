# Leaky Tokens Microservices Project

## Directory Overview

This directory contains the "Leaky Tokens" project, which is a comprehensive microservices-based demonstration project showcasing advanced Java/Spring Boot capabilities for Senior+/Staff level developers. The project simulates a token-based API gateway system that connects to various paid token-based services like Qwen API, Gemini API, and OpenAI API through custom stub implementations.

## Project Structure

The main project is located in the `leaky-tokens/` directory and contains complete documentation for a planned microservices architecture:

- `README.md` - Project overview and goals
- `feature_list.md` - Detailed features to implement across multiple phases
- `architecture_overview.md` - System architecture diagram and component descriptions
- `microservices_design.md` - Individual service specifications
- `infrastructure_plan.md` - Docker Compose setup and monitoring configuration
- `implementation_progress_tracker.md` - Progress tracking with milestones

## Project Purpose

The Leaky Tokens project is designed to demonstrate:

- Advanced microservices architecture patterns
- Enterprise-grade infrastructure with monitoring and observability
- Complex distributed systems concepts (SAGA, Transactional Outbox)
- Complete development environment using Docker Compose
- Proficiency in modern Java 25, Spring Boot, and cloud-native technologies

## Technology Stack

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

The system consists of 6 core microservices:

1. **API Gateway** - Entry point for all client requests (Spring Cloud Gateway)
2. **Authorization Server** - OAuth2/OIDC compliant authentication
3. **Configuration Server** - Centralized configuration management
4. **Service Discovery** - Eureka-based service registry
5. **Token Service** - Core leaky bucket implementation for rate limiting
6. **Analytics Service** - Metrics and analytics processing

## Key Features

- **Token Management**: Leaky bucket algorithm implementation for rate limiting
- **Token Quotas**: Persistent per-user/provider token pools with reservation and release
- **Service Mesh**: Circuit breaker and resilience patterns
- **Data Storage**: PostgreSQL, Redis, and Apache Cassandra integration
- **Message Queuing**: Kafka for asynchronous communication
- **Security**: Authorization server for resource protection
- **Observability**: Prometheus metrics, Grafana visualization, and distributed tracing
- **Configuration Management**: Centralized config server

## Implementation Status

The project is now in **active development** with the foundation and core services implemented. Current progress is roughly **~60%**, with production hardening and full test coverage still pending.

### Phase 1: Foundation Setup (Target: Week 1)
- [x] Create project documentation (README, feature_list, architecture_overview, etc.)
- [x] Initialize Gradle project with Kotlin DSL
- [x] Set up project structure with modules
- [x] Configure Java 25 compatibility
- [x] Add common dependencies (Spring Boot, Lombok, etc.)

### Planned Phases
- Phase 2: Core Services (Week 2)
- Phase 3: Advanced Features (Week 3) 
- Phase 4: Resilience & Observability (Week 4)
- Phase 5: Production Readiness (Week 5)

### Implemented Highlights
- Spring Cloud Config + Eureka service discovery in place.
- API Gateway routing + JWT validation.
- Token Service leaky-bucket rate limiting with Redis option.
- Transactional outbox + SAGA orchestration in Token Service for token purchase flow.
- Kafka consumer for token usage + Analytics persistence in Cassandra.
- Stub provider services (Qwen/Gemini/OpenAI) wired through gateway.
- Prometheus metrics + Grafana dashboard provisioning + Jaeger tracing support.

## Infrastructure Setup

The project includes a complete Docker Compose configuration that orchestrates:

- All 6 microservices
- PostgreSQL, Redis, Kafka, and Cassandra databases
- Monitoring stack (Prometheus, Grafana, Jaeger)
- Service discovery (Eureka) and configuration server

## Building and Running

Once implemented, the project will be runnable with:

```bash
# Start the infrastructure
docker-compose up -d

# Build and run the services
./gradlew bootRun
```

## Development Conventions

The project follows enterprise-level Java development practices:
- Clean architecture principles
- Proper separation of concerns
- Comprehensive testing strategy
- Security-first approach
- Observability and monitoring built-in
- Resilience patterns (circuit breakers, retries, bulkheads)

## Context

This project appears to be part of a broader learning initiative for senior Java backend development, as evidenced by the presence of a "senior_java_backend_developer_study_plan.md" file in the parent directory. The Leaky Tokens project serves as a practical implementation of the concepts covered in the study plan, particularly focusing on microservices architecture, distributed systems, and cloud-native technologies.

The project is designed to showcase advanced skills in:
- Microservices architecture patterns
- System design and scalability
- Distributed data management
- Resilience and fault tolerance
- Observability and monitoring
- Security implementation
- Container orchestration
