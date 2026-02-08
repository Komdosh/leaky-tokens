# 🚀 Leaky Tokens

<div align="center">

[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?style=for-the-badge&logo=gradle)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)


[![CI](https://github.com/Komdosh/leaky-tokens/actions/workflows/ci.yml/badge.svg)](https://github.com/Komdosh/leaky-tokens/actions/workflows/ci.yml)
![Coverage](https://raw.githubusercontent.com/Komdosh/leaky-tokens/main/badges/jacoco.svg)

**Enterprise-Grade Token Management & Rate Limiting System**

<p align="center">
  <img src="https://img.shields.io/badge/Microservices-Ready-success?style=flat-square&logo=serverless" />
  <img src="https://img.shields.io/badge/Kafka-Events-informational?style=flat-square&logo=apache-kafka" />
  <img src="https://img.shields.io/badge/SAGA-Pattern-orange?style=flat-square" />
  <img src="https://img.shields.io/badge/Observability-Full-blueviolet?style=flat-square" />
</p>

[📖 Documentation](#documentation) • [🚀 Quick Start](#quick-start) • [📊 Architecture](#architecture) • [🔧 API](#api-reference)

</div>

---

## WHY?

This is an auto-generated project by AI agents. I already have experience with all of these technologies, but nobody
cares if you don't have a GitHub repo with a pet project—so here it is.

Why do I use AI agents to generate projects? Because it's fun and I'm bored. Do I really work with all of these
technologies? Yep. Ten years in, that's the answer.

- But you still have to write code yourself, because AI agents are not perfect and can't write everything for you. And
this project doesn't show your skills.
- Well, that's the new world. I worked hard before. I wrote code day and night, but times have changed.

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🎯 Core Capabilities
- **🪣 Leaky Bucket Rate Limiting** - Smooth traffic flow
- **💰 Token Quotas** - Per-user & org-level management
- **⚡ SAGA Orchestration** - Distributed transactions
- **🔑 Multi-Auth** - JWT + API Key support

</td>
<td width="50%">

### 🏗️ Architecture
- **☁️ Cloud-Native** - 12-Factor App ready
- **🔍 Service Discovery** - Eureka registry
- **⚖️ Load Balancing** - Gateway routing
- **🛡️ Circuit Breaker** - Resilience4j

</td>
</tr>
<tr>
<td width="50%">

### 📊 Observability
- **📈 Prometheus Metrics** - Real-time monitoring
- **📉 Grafana Dashboards** - Visual insights
- **🔎 Distributed Tracing** - Jaeger integration
- **📝 Centralized Logs** - Full audit trail

</td>
<td width="50%">

### 🗄️ Data Layer
- **🐘 PostgreSQL** - Primary persistence
- **⚡ Redis** - Caching & rate limits
- **📨 Kafka** - Event streaming
- **📦 Cassandra** - Time-series analytics

</td>
</tr>
</table>

---

## 🏛️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        🌐 API Gateway                           │
│                    (Port 8080 - Entry Point)                    │
└─────────────┬───────────────────────────────────────────────────┘
              │
    ┌─────────┼─────────┬─────────────┐
    │         │         │             │
    ▼         ▼         ▼             ▼
┌───────┐ ┌───────┐ ┌────────┐  ┌──────────┐
│  🔐   │ │  🪙    │ │  📊    │  │  📝      │
│ Auth  │ │ Token │ │Analytics│ │ Config   │
│ 8081  │ │ 8082  │ │  8083  │  │  8888    │
└───────┘ └───────┘ └────────┘  └──────────┘
                                              
🗄️ PostgreSQL  ⚡ Redis  📨 Kafka  📦 Cassandra
```

### 🎯 Service Registry

| Service | Port | Purpose | Status |
|---------|------|---------|--------|
| 🌐 API Gateway | 8080 | Entry point, routing, rate limiting | ✅ Ready |
| 🔐 Auth Server | 8081 | JWT & API Key authentication | ✅ Ready |
| 🪙 Token Service | 8082 | Core business logic | ✅ Ready |
| 📊 Analytics | 8083 | Usage tracking & reports | ✅ Ready |
| 📍 Eureka | 8761 | Service discovery | ✅ Ready |
| ⚙️ Config | 8888 | Centralized configuration | ✅ Ready |

---

## 🚀 Quick Start

### Prerequisites

- ☕ Java 25
- 🐳 Docker & Docker Compose
- 📦 Gradle (wrapper included)

### ⚡ One-Command Start

```bash
# Clone repository
git clone <repository-url> && cd leaky-tokens

# Start infrastructure
docker-compose -f docker-compose.infra.yml up -d

# Run all services
./gradlew bootRun --parallel
```

### 🧪 Test It Works

```bash
# Check service health
curl http://localhost:8082/api/v1/tokens/status | jq

# Register a user
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","email":"demo@example.com","password":"password"}' | jq

# Test token consumption
curl -X POST http://localhost:8082/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -d '{"userId":"YOUR_USER_ID","provider":"openai","tokens":50}' | jq
```

---

## 🧪 Postman

1. Import the collection `docs/postman/leaky-tokens.postman_collection.json`.
2. Import the environment `docs/postman/leaky-tokens.postman_environment.json`.
3. Select the `Leaky Tokens Local` environment.
4. Run `Auth/Login` to auto-populate `accessToken` and `userId`, then exercise the other requests.

---

## 📖 Documentation

<div align="center">

| 📚 Guide | 📝 Description |
|----------|---------------|
| [📘 Overview](docs/01-overview.md) | Project purpose & architecture |
| [🚀 Getting Started](docs/02-getting-started.md) | Installation & setup |
| [👤 User Guide](docs/03-user-guide.md) | How to use the API |
| [🏗️ Architecture](docs/04-architecture.md) | Technical deep-dive |
| [🎯 Use Cases](docs/05-use-cases.md) | Business scenarios |
| [⚙️ Configuration](docs/06-configuration.md) | All config options |
| [📊 Monitoring](docs/07-monitoring.md) | Metrics, logs, tracing |
| [📋 API Reference](docs/08-api-reference.md) | Complete API docs |
| [🔧 Troubleshooting](docs/09-troubleshooting.md) | Common issues |
| [💻 Development](docs/10-development.md) | Contributing guide |

</div>

---

## 🛠️ Technology Stack

### Backend
<p align="left">
  <img src="https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen?style=flat-square&logo=spring" />
  <img src="https://img.shields.io/badge/Spring%20Cloud-2025.1.1-blue?style=flat-square&logo=spring" />
  <img src="https://img.shields.io/badge/Lombok-1.18.40-red?style=flat-square" />
  <img src="https://img.shields.io/badge/MapStruct-1.6.2-yellow?style=flat-square" />
</p>

### Infrastructure
<p align="left">
  <img src="https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql" />
  <img src="https://img.shields.io/badge/Redis-7-red?style=flat-square&logo=redis" />
  <img src="https://img.shields.io/badge/Kafka-7.5-black?style=flat-square&logo=apache-kafka" />
  <img src="https://img.shields.io/badge/Cassandra-4.1-blue?style=flat-square&logo=apache-cassandra" />
</p>

### Observability
<p align="left">
  <img src="https://img.shields.io/badge/Prometheus-E6522C?style=flat-square&logo=prometheus" />
  <img src="https://img.shields.io/badge/Grafana-F46800?style=flat-square&logo=grafana" />
  <img src="https://img.shields.io/badge/Jaeger-66CFE3?style=flat-square" />
</p>

---

## 📊 Key Capabilities

### 🪣 Rate Limiting Strategies

<details>
<summary><b>Leaky Bucket (Default)</b></summary>

Tokens leak at a constant rate, smoothing out traffic bursts:

```yaml
token:
  bucket:
    capacity: 1000
    leakRatePerSecond: 10.0
    strategy: LEAKY_BUCKET
```
</details>

<details>
<summary><b>Token Bucket</b></summary>

Allows short bursts while maintaining average rate:

```yaml
token:
  bucket:
    capacity: 1000
    leakRatePerSecond: 10.0
    strategy: TOKEN_BUCKET
```
</details>

<details>
<summary><b>Fixed Window</b></summary>

Simple counter-based limiting:

```yaml
token:
  bucket:
    capacity: 1000
    windowSeconds: 60
    strategy: FIXED_WINDOW
```
</details>

### 💰 Quota Management

```bash
# Check quota
curl "http://localhost:8082/api/v1/tokens/quota?userId=...&provider=openai"

# Consume tokens
curl -X POST http://localhost:8082/api/v1/tokens/consume \
  -d '{"userId":"...","provider":"openai","tokens":100}'

# Purchase more tokens
curl -X POST http://localhost:8082/api/v1/tokens/purchase \
  -H "Idempotency-Key: purchase-001" \
  -d '{"userId":"...","provider":"openai","tokens":1000}'
```

---

## 🔍 Observability

### 📈 Monitoring URLs

| Tool | URL | Description |
|------|-----|-------------|
| 📊 Grafana | http://localhost:3000 | Dashboards (admin/admin) |
| 📈 Prometheus | http://localhost:9090 | Metrics collection |
| 🔎 Jaeger | http://localhost:16686 | Distributed tracing |
| 📍 Eureka | http://localhost:8761 | Service registry |

### 🕵️ Distributed Tracing

View end-to-end request flows across microservices:

```bash
# Start with full tracing stack (includes Jaeger)
docker-compose -f docker-compose.full.yml up -d

# Access Jaeger UI
open http://localhost:16686

# Make some requests and watch traces appear
curl -X POST http://localhost:8080/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -d '{"userId":"...","provider":"openai","tokens":50}'
```

**Trace Analysis:**
- See request paths through all services
- Identify latency bottlenecks
- Debug distributed issues
- 100% sampling in development mode

📚 **[Full Tracing Guide](docs/TRACING.md)**

### 📝 Quick Log Check

```bash
# Service logs
./gradlew :token-service:bootRun 2>&1 | tee service.log

# Docker logs
docker-compose logs -f token-service

# Search for errors
grep "ERROR" service.log
```

---

## 🎯 API Reference

### Authentication Methods

```bash
# JWT Token
curl -H "Authorization: Bearer $JWT_TOKEN" ...

# API Key
curl -H "X-Api-Key: leaky_userid_xxxx" ...
```

### Core Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/register` | Create user account | Public |
| POST | `/api/v1/auth/login` | Authenticate | Public |
| GET | `/api/v1/tokens/quota` | Check user quota | JWT |
| POST | `/api/v1/tokens/consume` | Consume tokens | JWT |
| POST | `/api/v1/tokens/purchase` | Buy tokens | JWT |
| GET | `/api/v1/analytics/report` | Usage report | JWT |

📚 **[Full API Documentation →](docs/08-api-reference.md)**

---

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport

# Performance tests
./gradlew :performance-tests:gatlingRun
```

---

## 🤝 Contributing

We welcome contributions! Please see our [Development Guide](docs/10-development.md) for:

- 🏗️ Project structure
- 📝 Code style guidelines
- 🧪 Testing best practices
- 🔧 Development setup

---

<div align="center">

**⭐ Star this repo if you find it helpful!**

Made with ❤️ and 🤖 AI assistance

</div>
