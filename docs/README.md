# Leaky Tokens - Project Documentation

Welcome to the Leaky Tokens project documentation. This microservices-based token API gateway demonstrates advanced distributed systems patterns using Java 25, Spring Boot 4.x, and modern cloud-native technologies.

## Quick Links

- [Project Overview](01-overview.md) - Purpose, features, and architecture
- [Getting Started](02-getting-started.md) - Installation and setup guide
- [User Guide](03-user-guide.md) - How to use the system
- [Architecture](04-architecture.md) - Technical architecture details
- [Use Cases](05-use-cases.md) - Business use cases and workflows
- [Configuration](06-configuration.md) - Configuration options and feature flags
- [Monitoring & Observability](07-monitoring.md) - Metrics, logs, and tracing
- [API Reference](08-api-reference.md) - Complete API documentation
- [Troubleshooting](09-troubleshooting.md) - Common issues and solutions
- [Development Guide](10-development.md) - Contributing and development setup

## What is Leaky Tokens?

Leaky Tokens is an enterprise-grade token management system designed for AI/ML API consumption. It provides:

- **Token Quota Management** - Per-user and per-organization token pools
- **Rate Limiting** - Leaky bucket algorithm for fair resource allocation
- **SAGA Orchestration** - Distributed transaction management for token purchases
- **Multi-Provider Support** - OpenAI, Qwen, Gemini integration
- **Analytics & Monitoring** - Real-time usage tracking and anomaly detection

## Who Should Use This?

- **AI Platform Providers** - Manage API quotas for customers
- **Enterprise IT Teams** - Control AI service consumption
- **Developers** - Learn microservices patterns and best practices
- **DevOps Engineers** - Study observability and monitoring setups

## Quick Start

```bash
# Clone the repository
cd leaky-tokens

# Start infrastructure
docker compose -f docker-compose.infra.yml up -d

# Build and run services
./gradlew build
./gradlew bootRun --parallel

# Test the API
curl http://localhost:8082/api/v1/tokens/status
```

## System Requirements

- **Java**: 25 or higher
- **Docker**: 24.0+ with Docker Compose
- **Memory**: Minimum 8GB RAM
- **Disk**: 10GB free space

## Support

For issues and questions:
- Check the [Troubleshooting Guide](09-troubleshooting.md)
- Review [API Reference](08-api-reference.md)
- Examine logs using guidance in [Monitoring](07-monitoring.md)

---

**Version**: 1.0  
**Last Updated**: 2026-02-05  
**License**: MIT
