# Getting Started

This guide will help you set up and run the Leaky Tokens project locally.

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 25**
   ```bash
   # Verify installation
   java -version
   # Should show: openjdk version "25"
   ```

2. **Docker & Docker Compose**
   ```bash
   # Verify installation
   docker --version
   docker compose --version
   ```

3. **Gradle** (optional, wrapper included)
   ```bash
   # Project includes gradlew wrapper
   ./gradlew --version
   ```

### System Requirements

- **RAM**: 8GB minimum (16GB recommended)
- **Disk**: 10GB free space
- **CPU**: 4 cores recommended
- **Network**: Ports 8080-8093, 5432, 6379, 9092, 9042, 8761, 8888 must be available

## Installation Steps

### Step 1: Clone the Repository

```bash
cd /path/to/projects
cd leaky-tokens
```

### Step 2: Start Infrastructure Services

Infrastructure includes databases, message queue, and monitoring tools.

```bash
# Start all infrastructure services
docker compose -f docker-compose.infra.yml up -d

# Verify services are running
docker compose -f docker-compose.infra.yml ps
```

**Services started:**
- PostgreSQL (Port 5432)
- Redis (Port 6379)
- Kafka (Port 9092)
- Cassandra (Port 9042)

**Wait for services to initialize** (approximately 30-60 seconds):

```bash
# Check PostgreSQL
docker exec postgres-db pg_isready -U leaky_user

# Check Redis
docker exec redis-cache redis-cli ping

# Check Kafka
docker exec kafka-broker kafka-topics --bootstrap-server localhost:9092 --list
```

Connect to the postgres and create a DB if needed:

PostgreSQL:

```postgresql
create database auth_db;
create database token_db;
```

Cassandra:

```cassandraql
CREATE KEYSPACE analytics_keyspace
    WITH REPLICATION = {
        'class' : 'SimpleStrategy',
        'replication_factor' : 1
        };
```

Kafka

```
docker compose exec kafka kafka-topics --create --if-not-exists --partitions 1 --replication-factor 1 --topic token-usage --bootstrap-server localhost:9092
```

### Step 3: Build the Project

```bash
# Clean build
./gradlew clean build

# Or skip tests for faster build
./gradlew clean build -x test
```

**Build output:**
- Compiled classes in `build/classes/`
- JAR files in `*/build/libs/`
- Test reports in `*/build/reports/`

### Step 4: Start Microservices

You have two options:

#### Option A: Start All Services (Parallel)

```bash
./gradlew bootRun --parallel
```

**Note**: This starts all services in the same terminal. Services will log output together.

#### Option B: Start Services Individually (Recommended)

Open separate terminal windows for each service:

```bash
# Terminal 1: Service Discovery (start first)
./gradlew :service-discovery:bootRun

# Terminal 2: Config Server (start second)
./gradlew :config-server:bootRun

# Terminal 3: Auth Server
./gradlew :auth-server:bootRun

# Terminal 4: Provider Stubs
./gradlew :qwen-stub:bootRun
./gradlew :gemini-stub:bootRun
./gradlew :openai-stub:bootRun

# Terminal 5: Token Service
./gradlew :token-service:bootRun

# Terminal 6: Analytics Service
./gradlew :analytics-service:bootRun

# Terminal 7: API Gateway
./gradlew :api-gateway:bootRun
```

**Start order matters:**
1. Service Discovery (Eureka)
2. Config Server
3. All other services (can be started in parallel)

### Step 5: Verify Installation

Check that all services are healthy:

```bash
# Service Discovery
curl http://localhost:8761
echo "Eureka: $?"

# Config Server
curl http://localhost:8888/actuator/health
echo "Config Server: $?"

# Auth Server
curl http://localhost:8081/oauth2/jwks
echo "Auth Server: $?"

# Token Service
curl http://localhost:8082/api/v1/tokens/status
echo "Token Service: $?"

# Analytics Service
curl http://localhost:8083/api/v1/analytics/health
echo "Analytics Service: $?"

# API Gateway
curl http://localhost:8080/api/v1/tokens/status
echo "API Gateway: $?"
```

## First API Call

### 1. Register a Test User

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Expected response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "testuser",
  "token": "eyJhbGciOiJSUzI1NiIs...",
  "roles": ["USER"]
}
```

Save the token:
```bash
export JWT_TOKEN="eyJhbGciOiJSUzI1NiIs..."
```

### 2. Check Token Service Status

```bash
curl http://localhost:8082/api/v1/tokens/status
```

**Expected response:**
```json
{
  "service": "token-service",
  "status": "ok",
  "timestamp": "2026-02-05T12:00:00Z"
}
```

### 3. Query User Quota

```bash
curl "http://localhost:8082/api/v1/tokens/quota?userId=550e8400-e29b-41d4-a716-446655440000&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected:** HTTP 404 (no quota exists yet)

## Development Mode

### Running with Local Profile

Services can run with local configuration (in-memory databases):

```bash
./gradlew :token-service:bootRun --args='--spring.profiles.active=local'
```

**Local profile features:**
- In-memory token bucket storage (no Redis required)
- H2 database instead of PostgreSQL
- Simplified logging

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :token-service:test

# Run specific test class
./gradlew :token-service:test --tests "TokenQuotaServiceTest"

# Run with coverage
./gradlew jacocoTestReport
```

## Docker Compose (Full Stack)

For a complete production-like environment:

```bash
# Start everything including microservices
docker compose up -d

# View logs
docker compose logs -f

# Scale a service
docker compose up -d --scale token-service=3

# Stop everything
docker compose down

# Stop and remove volumes
docker compose down -v
```

## IDE Setup

### IntelliJ IDEA

1. Open `build.gradle.kts` as project
2. Wait for Gradle sync to complete
3. Enable annotation processing:
   - Settings → Build → Annotation Processors
   - Enable annotation processing
   - Obtain processors from project classpath

### VS Code

1. Install Extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Gradle for Java

2. Open project folder
3. Run individual services from Spring Boot Dashboard

## Common Issues

### Port Already in Use

```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Database Connection Failed

```bash
# Restart PostgreSQL
docker compose -f docker-compose.infra.yml restart postgres

# Check logs
docker logs postgres
```

### Service Registration Issues

Ensure services start in order:
1. Service Discovery first
2. Wait 10 seconds
3. Config Server
4. Wait 10 seconds
5. Other services

### Gradle Build Fails

```bash
# Clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies

# Check Java version
./gradlew --version
```

## Next Steps

Now that you have the system running:

1. **Learn the API**: [API Reference](08-api-reference.md)
2. **Try use cases**: [User Guide](03-user-guide.md)
3. **Understand architecture**: [Architecture](04-architecture.md)
4. **Configure the system**: [Configuration](06-configuration.md)

## Quick Commands Reference

```bash
# Start infrastructure
docker compose -f docker-compose.infra.yml up -d

# Build
./gradlew clean build

# Run all services
./gradlew bootRun --parallel

# Run single service
./gradlew :token-service:bootRun

# Run tests
./gradlew test

# Check service health
curl http://localhost:8761
curl http://localhost:8082/api/v1/tokens/status

# View logs
docker compose -f docker-compose.infra.yml logs -f

# Stop everything
docker compose -f docker-compose.infra.yml down
```

---

**Previous**: [Overview](01-overview.md)  
**Next**: [User Guide →](03-user-guide.md)
