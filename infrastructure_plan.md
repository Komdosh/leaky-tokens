# Infrastructure Plan - Leaky Tokens Project

## Overview

This document outlines the complete infrastructure setup for the Leaky Tokens project using Docker Compose. The infrastructure includes all necessary services for development, testing, and demonstration of the microservices architecture.

## Docker Compose Architecture

The infrastructure consists of the following components:

### Core Services
- **API Gateway** - Spring Cloud Gateway
- **Authorization Server** - OAuth2 server
- **Configuration Server** - Spring Cloud Config
- **Service Discovery** - Eureka Server
- **Token Service** - Core token management
- **Analytics Service** - Metrics and analytics

### Infrastructure Services
- **PostgreSQL** - Primary relational database
- **Redis** - Caching and session store
- **Apache Kafka** - Message broker
- **Apache Cassandra** - Time-series data store

### Monitoring Services
- **Prometheus** - Metrics collection
- **Grafana** - Visualization and dashboards
- **Jaeger** - Distributed tracing
- **Zipkin** - Alternative distributed tracing

### Supporting Services
- **Swagger UI** - API documentation
- **Admin UI** - Service administration

## Docker Compose Configuration

### docker-compose.yml

```yaml
version: '3.8'

networks:
  leaky-tokens-net:
    driver: bridge

volumes:
  postgres_data:
  cassandra_data:
  grafana_data:

services:
  # Service Discovery
  eureka-server:
    image: openjdk:25-jdk-slim
    container_name: eureka-server
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    networks:
      - leaky-tokens-net
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8761/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  # Configuration Server
  config-server:
    image: openjdk:25-jdk-slim
    container_name: config-server
    ports:
      - "8888:8888"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
    depends_on:
      eureka-server:
        condition: service_healthy
    networks:
      - leaky-tokens-net
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8888/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  # PostgreSQL Database
  postgres:
    image: postgres:17-alpine
    container_name: postgres-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: leakytokens
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-password}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    networks:
      - leaky-tokens-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: redis-cache
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    networks:
      - leaky-tokens-net
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3
    restart: unless-stopped

  # Apache Kafka
  kafka:
     image: confluentinc/cp-kafka:latest
     container_name: kafka-broker
     ports:
        - "9092:9092"
     environment:
        KAFKA_PROCESS_ROLES: "broker,controller"
        KAFKA_NODE_ID: 1
        KAFKA_LISTENERS: "PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093"
        KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://localhost:9092"
        KAFKA_CONTROLLER_QUORUM_VOTERS: "1@kafka:9093"
        KAFKA_CONTROLLER_LISTENER_NAMES: "CONTROLLER"
        KAFKA_LOG_DIRS: "/tmp/kraft-combined-logs"
     networks:
        - leaky-tokens-net
     restart: unless-stopped

  # Apache Cassandra
  cassandra-db:
    image: cassandra:5.0.1
    container_name: cassandra-db
    ports:
      - "9042:9042"
      - "9160:9160"
    environment:
      - MAX_HEAP_SIZE=512M
      - HEAP_NEWSIZE=100M
    volumes:
      - cassandra_data:/var/lib/cassandra
    networks:
      - leaky-tokens-net
    healthcheck:
      test: ["CMD-SHELL", "cqlsh -e 'DESCRIBE KEYSPACES' 2>/dev/null || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
    restart: unless-stopped

  # Authorization Server
  auth-server:
    image: openjdk:25-jdk-slim
    container_name: auth-server
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/auth_db
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
    depends_on:
      eureka-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - leaky-tokens-net
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  # Token Service
  token-service:
    image: openjdk:25-jdk-slim
    container_name: token-service
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/token_db
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      eureka-server:
        condition: service_healthy
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - leaky-tokens-net
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  # Analytics Service
  analytics-service:
    image: openjdk:25-jdk-slim
    container_name: analytics-service
    ports:
      - "8083:8083"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
      - SPRING_CASSANDRA_CONTACT_POINTS=cassandra
      - SPRING_CASSANDRA_KEYSPACE_NAME=analytics_keyspace
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      eureka-server:
        condition: service_healthy
      cassandra-db:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - leaky-tokens-net
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8083/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  # API Gateway
  api-gateway:
    image: openjdk:25-jdk-slim
    container_name: api-gateway
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
    depends_on:
      eureka-server:
        condition: service_healthy
      auth-server:
        condition: service_healthy
      token-service:
        condition: service_healthy
    networks:
      - leaky-tokens-net
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
    networks:
      - leaky-tokens-net
    restart: unless-stopped

  # Grafana
  grafana:
    image: grafana/grafana-enterprise
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    depends_on:
      - prometheus
    networks:
      - leaky-tokens-net
    restart: unless-stopped

  # Jaeger
  jaeger:
    image: jaegertracing/all-in-one:latest
    container_name: jaeger
    ports:
      - "16686:16686"  # UI
      - "14268:14268"  # Collector HTTP
      - "14250:14250"  # Collector gRPC
    environment:
      - COLLECTOR_ZIPKIN_HTTP_PORT=9411
    networks:
      - leaky-tokens-net
    restart: unless-stopped

volumes:
  redis_data:
  prometheus_data:
  grafana_data:
  cassandra_data:
  postgres_data:
```

## Monitoring Configuration

### Prometheus Configuration (`prometheus/prometheus.yml`)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']

  - job_name: 'auth-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['auth-server:8081']

  - job_name: 'token-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['token-service:8082']

  - job_name: 'analytics-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['analytics-service:8083']

  - job_name: 'config-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['config-server:8888']

  - job_name: 'eureka-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['eureka-server:8761']

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          # - "alertmanager:9093"
```

### Grafana Configuration

#### Provisioning Files

`grafana/provisioning/datasources/datasource.yml`:
```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    orgId: 1
    url: http://prometheus:9090
    password: ""
    user: ""
    database: ""
    basicAuth: false
    basicAuthUser: ""
    basicAuthPassword: ""
    withCredentials: false
    isDefault: true
    version: 1
    editable: true
```

`grafana/provisioning/dashboards/dashboard.yml`:
```yaml
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
```

### Predefined Dashboards

#### System Performance Dashboard (`grafana/provisioning/dashboards/system-performance.json`)
```json
{
  "dashboard": {
    "id": null,
    "title": "System Performance",
    "tags": ["leaky-tokens"],
    "style": "dark",
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "HTTP Requests",
        "type": "graph",
        "targets": [
          {
            "expr": "irate(http_server_requests_total{uri!~\"/actuator/.*|/error\"}[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ],
        "xaxis": {
          "mode": "time",
          "show": true
        },
        "yaxes": [
          {
            "format": "reqps",
            "show": true
          },
          {
            "format": "short",
            "show": true
          }
        ]
      },
      {
        "id": 2,
        "title": "JVM Memory",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes",
            "legendFormat": "{{area}} - {{id}}"
          }
        ],
        "xaxis": {
          "mode": "time",
          "show": true
        },
        "yaxes": [
          {
            "format": "bytes",
            "show": true
          },
          {
            "format": "short",
            "show": true
          }
        ]
      }
    ],
    "time": {
      "from": "now-6h",
      "to": "now"
    },
    "timepicker": {
      "time_options": ["5m", "15m", "1h", "6h", "12h", "24h", "2d", "7d", "30d"]
    },
    "templating": {
      "list": []
    },
    "annotations": {
      "list": []
    },
    "refresh": "5s",
    "schemaVersion": 12,
    "version": 0
  }
}
```

## Database Initialization Scripts

### PostgreSQL Init Script (`init-scripts/init.sql`)
```sql
-- Create databases for different services
CREATE DATABASE auth_db;
CREATE DATABASE token_db;

-- Connect to auth_db and create tables
\c auth_db;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id),
    role_id UUID REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    key_value VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Connect to token_db and create tables
\c token_db;

CREATE TABLE token_pools (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(50) NOT NULL,
    user_id UUID,
    total_tokens BIGINT NOT NULL,
    remaining_tokens BIGINT NOT NULL,
    reset_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE token_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    provider VARCHAR(50) NOT NULL,
    tokens_consumed INT NOT NULL,
    request_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_token_pool_user_provider ON token_pools(user_id, provider);
CREATE INDEX idx_token_usage_user_timestamp ON token_usage(user_id, request_timestamp);
```

## Environment Variables

### `.env` file
```bash
# Database
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password

# JWT
JWT_SECRET=your-super-secret-jwt-key-change-in-production

# Keystore
KEYSTORE_PASSWORD=changeit
KEY_PASSWORD=changeit

# Kafka
KAFKA_ADVERTISED_HOST_NAME=localhost

# Cassandra
CASSANDRA_CLUSTER_NAME=LeakyTokensCluster
CASSANDRA_DC=datacenter1
CASSANDRA_RACK=rack1
```

## Startup Sequence

The services will start in the following order to ensure proper dependencies:

1. **Infrastructure Services**:
   - PostgreSQL
   - Redis
   - Kafka
   - Cassandra

2. **Core Infrastructure**:
   - Eureka Server (Service Discovery)
   - Config Server

3. **Backend Services**:
   - Authorization Server
   - Token Service
   - Analytics Service

4. **Frontend Services**:
   - API Gateway

5. **Monitoring Services**:
   - Prometheus
   - Grafana
   - Jaeger

## Health Checks

Each service implements Spring Boot Actuator health checks:

- **API Gateway**: Checks connectivity to downstream services
- **Authorization Server**: Checks database and Redis connectivity
- **Token Service**: Checks database, Redis, and Kafka connectivity
- **Analytics Service**: Checks Cassandra and Kafka connectivity
- **Infrastructure Services**: Service-specific health indicators

## Resource Allocation

### Memory Limits
- API Gateway: 512MB
- Authorization Server: 512MB
- Token Service: 768MB
- Analytics Service: 768MB
- Configuration Server: 256MB
- Eureka Server: 256MB

### CPU Limits
- All services: 1.0 CPU share (can be adjusted based on load)

## Network Configuration

All services communicate through the `leaky-tokens-net` Docker network. External access is provided through mapped ports:

- API Gateway: Port 8080 (main application access)
- Eureka: Port 8761 (service registry)
- Config Server: Port 8888 (configuration access)
- Monitoring: Ports 3000 (Grafana), 9090 (Prometheus), 16686 (Jaeger)

## Backup and Recovery

### PostgreSQL Backup
```bash
# Backup command
docker exec postgres pg_dump -U postgres leakytokens > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore command
docker exec -i postgres psql -U postgres -d leakytokens < backup_file.sql
```

### Cassandra Backup
```bash
# Backup command
docker exec cassandra nodetool snapshot analytics_keyspace -t backup_$(date +%Y%m%d_%H%M%S)

# Restore procedure
# 1. Stop the analytics service
# 2. Copy snapshot files to Cassandra data directory
# 3. Run: docker exec cassandra nodetool refresh analytics_keyspace
# 4. Restart the analytics service
```

## Scaling Configuration

### Horizontal Scaling
To scale individual services, use Docker Compose scale command:
```bash
# Scale token service to 3 instances
docker-compose up --scale token-service=3

# Scale analytics service to 2 instances
docker-compose up --scale analytics-service=2
```

### Load Balancing
Service discovery handles load balancing automatically through client-side load balancing (Ribbon).

## Security Configuration

### Network Security
- All internal communication happens through isolated Docker network
- No external access to databases except through services
- API Gateway acts as single entry point

### Credential Management
- Database passwords stored in environment variables
- JWT secrets stored in environment variables
- Keystore passwords stored in environment variables

## Development Workflow

### Starting the Infrastructure
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Building Custom Images
```bash
# Build service images
./gradlew :api-gateway:build
./gradlew :auth-server:build
# ... build other services

# Tag and push images if needed
docker tag api-gateway:latest your-registry/api-gateway:latest
docker push your-registry/api-gateway:latest
```

This infrastructure plan provides a complete, production-ready setup for the Leaky Tokens microservices project with all necessary monitoring, security, and operational considerations.