# Deployment & Ops Guide

This guide covers running the system locally with Docker Compose, basic operations, and troubleshooting.

## 1) Prerequisites
- Docker Desktop (or Docker Engine + Compose plugin)
- Java 25 (for local `./gradlew` runs)
- `jq` for JSON formatting (optional)

## 2) Compose Profiles

### Full stack (recommended)
Includes Postgres, Redis, Cassandra, Kafka, Eureka, Config Server, Gateway, Auth, Token, Analytics, and stubs.

```bash
docker compose -f docker-compose.full.yml up -d
```

### Infra only (local dev)
Just storage + discovery/config to run services locally in your IDE.

```bash
docker compose -f docker-compose.infra.yml up -d
```

## 3) Health & Readiness

```bash
curl -s http://localhost:8080/actuator/health | jq
curl -s http://localhost:8081/actuator/health | jq
curl -s http://localhost:8082/actuator/health | jq
curl -s http://localhost:8083/actuator/health | jq
```

## 4) OpenAPI / Swagger UI

```bash
open http://localhost:8080/swagger-ui/index.html
open http://localhost:8081/swagger-ui/index.html
open http://localhost:8082/swagger-ui/index.html
open http://localhost:8083/swagger-ui/index.html
```

## 5) Logs

```bash
docker compose -f docker-compose.full.yml logs -f api-gateway
docker compose -f docker-compose.full.yml logs -f token-service
```

## 6) Common Env Vars

You can override defaults via environment variables when starting compose:

- `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `SPRING_PROFILES_ACTIVE` (e.g., `docker` or `local`)
- `SPRING_CONFIG_IMPORT` (for config server)
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`

## 7) Backups

See `docs/backup_recovery.md` for Postgres/Cassandra/Redis backup scripts and restore steps.

## 8) Troubleshooting

- **JWT failures**: confirm JWKS endpoint is reachable at `/oauth2/jwks`.
- **Auth failures on gateway**: verify API key validation endpoint is available at `/api/v1/auth/api-keys/validate`.
- **Cassandra not ready**: analytics-service waits for port `9042` and may need extra startup time.
- **Kafka issues**: check `kafka` container logs; ensure `bootstrap-servers` points to `kafka:9092` (docker) or `localhost:9092` (local).

