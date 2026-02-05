# Troubleshooting Guide

Common issues and their solutions for the Leaky Tokens system.

## Table of Contents
1. [Installation Issues](#installation-issues)
2. [Service Startup Issues](#service-startup-issues)
3. [Database Issues](#database-issues)
4. [Authentication Issues](#authentication-issues)
5. [Rate Limiting Issues](#rate-limiting-issues)
6. [SAGA Issues](#saga-issues)
7. [Kafka Issues](#kafka-issues)
8. [Performance Issues](#performance-issues)
9. [Debugging Techniques](#debugging-techniques)
10. [Getting Help](#getting-help)

---

## Installation Issues

### Issue: Gradle Build Fails

**Symptoms:**
```
./gradlew build
> Task :compileJava FAILED
error: cannot find symbol
```

**Solutions:**

1. **Check Java Version**
   ```bash
   java -version
   # Should be Java 25
   ```
   
   If not Java 25, set JAVA_HOME:
   ```bash
   export JAVA_HOME=/path/to/java25
   export PATH=$JAVA_HOME/bin:$PATH
   ```

2. **Clean Build**
   ```bash
   ./gradlew clean
   ./gradlew build --refresh-dependencies
   ```

3. **Check Gradle Wrapper**
   ```bash
   ./gradlew --version
   # Should show Gradle 8.5+
   ```

4. **Enable Annotation Processing**
   - IntelliJ: Settings → Build → Annotation Processors → Enable
   - VS Code: Ensure Java extension pack installed

### Issue: Docker Compose Fails to Start

**Symptoms:**
```
ERROR: for postgres  Cannot start service postgres: port is already allocated
```

**Solutions:**

1. **Check Port Conflicts**
   ```bash
   # Find process using port
   lsof -i :5432
   
   # Kill conflicting process
   kill -9 <PID>
   ```

2. **Remove Existing Containers**
   ```bash
   docker compose -f docker-compose.infra.yml down -v
   docker compose -f docker-compose.infra.yml up -d
   ```

3. **Check Docker Resources**
   ```bash
   # Check available memory
   docker system info | grep "Total Memory"
   
   # Prune unused resources
   docker system prune -a
   ```

### Issue: Out of Memory During Build

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**

1. **Increase Gradle Memory**
   ```bash
   export GRADLE_OPTS="-Xmx4g -Xms1g"
   ./gradlew build
   ```

2. **Update gradle.properties**
   ```properties
   org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
   org.gradle.parallel=true
   org.gradle.configureondemand=true
   ```

---

## Service Startup Issues

### Issue: Service Discovery Not Found

**Symptoms:**
```
Connection refused: localhost/127.0.0.1:8761
```

**Solutions:**

1. **Start Order Matters**
   ```bash
   # Start in this order:
   1. Service Discovery
   2. Config Server
   3. Other services
   ```

2. **Wait Between Starts**
   ```bash
   ./gradlew :service-discovery:bootRun
   # Wait 10 seconds
   ./gradlew :config-server:bootRun
   # Wait 10 seconds
   ./gradlew :token-service:bootRun
   ```

3. **Check Eureka Dashboard**
   ```bash
   curl http://localhost:8761
   # Should show Eureka dashboard
   ```

### Issue: Service Fails to Register

**Symptoms:**
```
com.netflix.discovery.shared.transport.TransportException: Cannot execute request on any known server
```

**Solutions:**

1. **Check Eureka URL**
   ```yaml
   # In application.yml
   eureka:
     client:
       service-url:
         defaultZone: http://localhost:8761/eureka/
   ```

2. **Verify Network Connectivity**
   ```bash
   telnet localhost 8761
   ```

3. **Check Application Name**
   ```yaml
   spring:
     application:
       name: token-service  # Must be unique
   ```

### Issue: Config Server Connection Failed

**Symptoms:**
```
Could not locate PropertySource: I/O error on GET request
```

**Solutions:**

1. **Start Config Server First**
   ```bash
   ./gradlew :config-server:bootRun
   # Wait for it to be ready
   curl http://localhost:8888/actuator/health
   ```

2. **Check Bootstrap Configuration**
   ```yaml
   # In bootstrap.yml
   spring:
     cloud:
       config:
         uri: http://localhost:8888
         fail-fast: false
   ```

3. **Disable Config Client (Local Dev)**
   ```bash
   ./gradlew :token-service:bootRun --args='--spring.cloud.config.enabled=false'
   ```

---

## Database Issues

### Issue: PostgreSQL Connection Refused

**Symptoms:**
```
Connection to localhost:5432 refused
org.postgresql.util.PSQLException: Connection refused
```

**Solutions:**

1. **Verify PostgreSQL is Running**
   ```bash
   docker ps | grep postgres
   
   # Check logs
   docker logs postgres
   ```

2. **Wait for Database to Initialize**
   ```bash
   # PostgreSQL takes time to start
   sleep 30
   
   # Test connection
   docker exec -it postgres pg_isready -U leaky_user
   ```

3. **Check Credentials**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/token_service
       username: leaky_user
       password: ${DB_PASSWORD:password}
   ```

### Issue: Database Schema Validation Failed

**Symptoms:**
```
Schema-validation: missing table [token_pools]
```

**Solutions:**

1. **Create Schema Manually**
   ```bash
   # Connect to PostgreSQL
   docker exec -it postgres psql -U leaky_user -d token_service
   
   # Run schema creation scripts
   # Scripts located in: src/main/resources/schema.sql
   ```

2. **Enable DDL Auto (Development Only)**
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: create  # Or 'update' for dev
   ```

3. **Run Flyway Migrations**
   ```bash
   ./gradlew flywayMigrate
   ```

### Issue: Redis Connection Failed

**Symptoms:**
```
Unable to connect to Redis: Connection refused: localhost/127.0.0.1:6379
```

**Solutions:**

1. **Start Redis**
   ```bash
   docker compose -f docker-compose.infra.yml up -d redis
   ```

2. **Check Redis Configuration**
   ```yaml
   spring:
     redis:
       host: localhost
       port: 6379
   ```

3. **Use Local Profile (No Redis)**
   ```bash
   ./gradlew :token-service:bootRun --args='--spring.profiles.active=local'
   ```

### Issue: Cassandra Connection Failed

**Symptoms:**
```
All Cassandra nodes have failed: localhost/127.0.0.1:9042
```

**Solutions:**

1. **Start Cassandra**
   ```bash
   docker compose -f docker-compose.infra.yml up -d cassandra
   
   # Wait for initialization (can take 2-3 minutes)
   sleep 180
   ```

2. **Create Keyspace**
   ```bash
   docker exec -it cassandra cqlsh
   
   CREATE KEYSPACE IF NOT EXISTS analytics 
   WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
   ```

3. **Check Analytics Service Profile**
   ```bash
   # Skip Cassandra for testing
   ./gradlew :analytics-service:bootRun --args='--analytics.enabled=false'
   ```

---

## Authentication Issues

### Issue: JWT Token Expired

**Symptoms:**
```
An error occurred while attempting to decode the Jwt: Jwt expired at 2026-02-05T11:00:00Z
```

**Solutions:**

1. **Refresh Token**
   ```bash
   # Login again to get new token
   curl -X POST http://localhost:8081/api/v1/auth/login \
     -d '{"username":"user","password":"pass"}'
   ```

2. **Check Token Expiration**
   ```bash
   # Decode JWT (using jwt.io or command line)
   echo "eyJhbGciOiJSUzI1NiIs..." | base64 -d
   ```

3. **Increase Token TTL (Development)**
   ```yaml
   jwt:
     expiration: 86400  # 24 hours instead of 1 hour
   ```

### Issue: Invalid API Key

**Symptoms:**
```
Invalid API key: leaky_xxxx...
```

**Solutions:**

1. **Verify API Key Format**
   ```
   Format: leaky_{userId}_{randomHex}
   Example: leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4e5f6
   ```

2. **Check if Key is Expired**
   ```bash
   # List keys
   curl http://localhost:8081/api/v1/auth/api-keys?userId=...
   ```

3. **Create New Key**
   ```bash
   curl -X POST http://localhost:8081/api/v1/auth/api-keys \
     -H "Authorization: Bearer $JWT" \
     -d '{"userId":"...","name":"new-key"}'
   ```

### Issue: "Unauthorized" on Protected Endpoints

**Symptoms:**
```
HTTP 401 Unauthorized
Full authentication is required to access this resource
```

**Solutions:**

1. **Check Authorization Header**
   ```bash
   # Correct format
   curl -H "Authorization: Bearer $TOKEN" ...
   
   # Wrong format
   curl -H "Authorization: $TOKEN" ...  # Missing "Bearer"
   ```

2. **Verify Token is Valid**
   ```bash
   # Test with JWKS endpoint
curl http://localhost:8081/oauth2/jwks
   ```

3. **Check Token Hasn't Been Revoked**
   ```bash
   # If using API keys, verify not revoked
   curl http://localhost:8081/api/v1/auth/api-keys?userId=...
   ```

### Issue: "Forbidden" Despite Authentication

**Symptoms:**
```
HTTP 403 Forbidden
Access is denied
```

**Solutions:**

1. **Check User Roles**
   ```bash
   # Login and check roles
   curl -X POST http://localhost:8081/api/v1/auth/login \
     -d '{"username":"...","password":"..."}'
   ```

2. **Verify Resource Ownership**
   ```bash
   # User can only access their own data
   # Admin can access all data
   ```

3. **Check Role Assignment**
   ```sql
   -- Query in PostgreSQL
   SELECT r.name FROM roles r
   JOIN user_roles ur ON r.id = ur.role_id
   JOIN users u ON ur.user_id = u.id
   WHERE u.username = 'username';
   ```

---

## Rate Limiting Issues

### Issue: Unexpected 429 Too Many Requests

**Symptoms:**
```
HTTP 429 Too Many Requests
Retry-After: 45
```

**Solutions:**

1. **Check Current Rate Limit Status**
   ```bash
   curl -i http://localhost:8080/api/v1/tokens/status \
     -H "X-Api-Key: $API_KEY" | grep X-RateLimit
   ```

2. **Understand Rate Limit Window**
   - Default: 120 requests per 60 seconds
   - Window resets after 60 seconds
   - Wait time shown in `Retry-After` header

3. **Check Per-Route Limits**
   ```yaml
   gateway:
     rate-limit:
       routes:
         token-service:
           capacity: 100  # Lower than default
   ```

4. **Implement Retry with Backoff**
   ```python
   import time
   import requests
   
   def make_request_with_retry(url, headers, max_retries=3):
       for attempt in range(max_retries):
           response = requests.get(url, headers=headers)
           if response.status_code != 429:
               return response
           wait = int(response.headers.get('Retry-After', 60))
           time.sleep(wait)
       raise Exception("Rate limit exceeded")
   ```

### Issue: Token Bucket Always Full

**Symptoms:**
All requests return 429 even with low traffic

**Solutions:**

1. **Check Token Bucket Configuration**
   ```yaml
   token:
     bucket:
       capacity: 1000
       leakRatePerSecond: 10.0
   ```

2. **Clear Redis Cache**
   ```bash
   docker exec -it redis redis-cli
   FLUSHALL
   ```

3. **Check for Bucket Leak**
   ```bash
   # Verify cleanup job is running
   grep "TokenBucketCleanupJob" token-service.log
   ```

### Issue: Different Rate Limits for Different Users

**Symptoms:**
Some users have stricter limits than others

**Solutions:**

1. **Check Tier Configuration**
   ```yaml
   token:
     tiers:
       USER:
         bucketCapacityMultiplier: 1.0
       PREMIUM:
         bucketCapacityMultiplier: 2.0
   ```

2. **Verify User Tier Assignment**
   ```bash
   # Check user's role/tier
   curl http://localhost:8081/api/v1/auth/api-keys/validate \
     -H "X-Api-Key: $API_KEY"
   ```

---

## SAGA Issues

### Issue: SAGA Stuck in STARTED State

**Symptoms:**
```json
{
  "sagaId": "...",
  "status": "STARTED",
  "createdAt": "2026-02-05T10:00:00Z"
}
```

**Solutions:**

1. **Check Recovery Job**
   ```bash
   # Verify recovery job is running
   grep "TokenPurchaseSagaRecoveryJob" token-service.log
   ```

2. **Check for Exceptions**
   ```bash
   grep -A 10 "SAGA.*$SAGA_ID" token-service.log
   ```

3. **Manual Recovery**
   ```sql
   -- Check SAGA in database
   SELECT * FROM token_purchase_saga WHERE id = 'saga-id';
   
   -- If stuck, manually update (careful!)
   UPDATE token_purchase_saga 
   SET status = 'FAILED' 
   WHERE id = 'saga-id' AND status = 'STARTED';
   ```

### Issue: Duplicate Token Purchases

**Symptoms:**
User was charged twice for the same purchase

**Solutions:**

1. **Use Idempotency Keys**
   ```bash
   curl -X POST /api/v1/tokens/purchase \
     -H "Idempotency-Key: unique-key-001" \
     -d '{...}'
   ```

2. **Check Idempotency Logic**
   ```sql
   -- Verify idempotency key is stored
   SELECT idempotency_key FROM token_purchase_saga WHERE id = 'saga-id';
   ```

3. **Query SAGA Before Creating**
   ```bash
   # Check if SAGA already exists
   curl /api/v1/tokens/purchase/$SAGA_ID
   ```

### Issue: SAGA Compensation Not Triggered

**Symptoms:**
Failed SAGA didn't release payment reservation

**Solutions:**

1. **Check Outbox Events**
   ```sql
   -- Query outbox table
   SELECT * FROM token_outbox 
   WHERE aggregate_type = 'TokenPurchaseSaga' 
   AND event_type LIKE '%COMPENSATION%';
   ```

2. **Verify Outbox Publisher**
   ```bash
   # Check if publisher job is running
   grep "OutboxPublisherJob" token-service.log
   ```

3. **Check Kafka Connectivity**
   ```bash
   # Verify Kafka is reachable
   docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092
   ```

---

## Kafka Issues

### Issue: Events Not Being Published

**Symptoms:**
Analytics service not receiving events

**Solutions:**

1. **Check Kafka is Running**
   ```bash
   docker ps | grep kafka
   docker logs kafka
   ```

2. **Verify Topic Exists**
   ```bash
   docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092
   # Should show: token-usage
   ```

3. **Create Topic if Missing**
   ```bash
   docker exec kafka kafka-topics.sh \
     --create \
     --topic token-usage \
     --bootstrap-server localhost:9092 \
     --partitions 3 \
     --replication-factor 1
   ```

4. **Check Outbox Table**
   ```sql
   -- Check for unpublished events
   SELECT COUNT(*) FROM token_outbox WHERE published_at IS NULL;
   ```

### Issue: Consumer Lag

**Symptoms:**
Analytics data is delayed

**Solutions:**

1. **Check Consumer Group**
   ```bash
   docker exec kafka kafka-consumer-groups.sh \
     --bootstrap-server localhost:9092 \
     --group analytics-service \
     --describe
   ```

2. **Scale Consumers**
   ```yaml
   spring:
     kafka:
       listener:
         concurrency: 3  # Increase consumer threads
   ```

3. **Check Cassandra Write Performance**
   ```bash
   # Monitor write latency in Cassandra logs
   docker logs cassandra | grep "write"
   ```

---

## Performance Issues

### Issue: High Response Latency

**Symptoms:**
API responses taking > 1 second

**Solutions:**

1. **Check Database Performance**
   ```sql
   -- Find slow queries
   SELECT query, mean_exec_time, calls 
   FROM pg_stat_statements 
   ORDER BY mean_exec_time DESC 
   LIMIT 10;
   ```

2. **Monitor Connection Pool**
   ```promql
   # Check active connections
   hikaricp_connections_active
   
   # Check wait time
   hikaricp_connections_usage_seconds_max
   ```

3. **Enable Query Logging**
   ```yaml
   spring:
     jpa:
       show-sql: true
       properties:
         hibernate:
           format_sql: true
   ```

4. **Add Database Indexes**
   ```sql
   CREATE INDEX idx_token_pools_user_provider 
   ON token_pools(user_id, provider);
   ```

### Issue: High Memory Usage

**Symptoms:**
Service consuming excessive memory

**Solutions:**

1. **Check JVM Heap**
   ```bash
   # View heap usage
   jmap -heap <PID>
   
   # Generate heap dump
   jmap -dump:format=b,file=heap.hprof <PID>
   ```

2. **Analyze Heap Dump**
   - Use Eclipse MAT or VisualVM
   - Look for memory leaks
   - Check for large collections

3. **Adjust JVM Settings**
   ```bash
   export JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC"
   ```

4. **Check for Memory Leaks**
   ```bash
   # Monitor GC activity
   jstat -gcutil <PID> 1000
   ```

### Issue: Circuit Breaker Open

**Symptoms:**
```
HTTP 503 Service Unavailable
Circuit breaker is OPEN
```

**Solutions:**

1. **Check Downstream Service**
   ```bash
   curl http://localhost:8082/actuator/health
   ```

2. **Wait for Recovery**
   - Circuit breaker automatically tests after wait duration
   - Default: 30 seconds in OPEN state

3. **Force Close (Emergency)**
   ```bash
   # Via Actuator (if available)
   curl -X POST http://localhost:8080/actuator/circuitbreakers/token-service/close
   ```

4. **Adjust Circuit Breaker Settings**
   ```yaml
   resilience4j:
     circuitbreaker:
       instances:
         token-service:
           wait-duration-in-open-state: 30s
           failure-rate-threshold: 50
   ```

---

## Debugging Techniques

### Enable Debug Logging

```yaml
logging:
  level:
    com.leaky.tokens: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

### Trace a Request

1. **Add Correlation ID**
   ```bash
   curl -H "X-Correlation-Id: debug-001" ...
   ```

2. **Search Logs**
   ```bash
   grep -r "debug-001" /var/log/leaky-tokens/
   ```

3. **Follow Trace in Jaeger**
   - Open http://localhost:16686
   - Search by trace ID

### Database Debugging

```sql
-- Enable query logging
ALTER SYSTEM SET log_statement = 'all';

-- Check active connections
SELECT * FROM pg_stat_activity;

-- Check locks
SELECT * FROM pg_locks;

-- Check slow queries
SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC;
```

### Network Debugging

```bash
# Test service connectivity
telnet localhost 8082

# Monitor traffic
sudo tcpdump -i lo0 port 8082

# Check open connections
lsof -i :8082

# Test API endpoints
curl -v http://localhost:8082/api/v1/tokens/status
```

### Performance Profiling

```bash
# CPU profiling
async-profiler -d 30 -f profile.html <PID>

# Memory allocation profiling
async-profiler -e alloc -d 30 -f alloc.html <PID>
```

---

## Getting Help

### Check Logs First

```bash
# Service logs
./gradlew :token-service:bootRun 2>&1 | tee token-service.log

# Docker logs
docker compose logs -f token-service

# System logs
tail -f /var/log/leaky-tokens/*.log
```

### Gather Information

When reporting issues, include:

1. **Environment**
   - Java version: `java -version`
   - Docker version: `docker --version`
   - OS: `uname -a`

2. **Configuration**
   - Active profile
   - Relevant configuration values (sanitized)

3. **Logs**
   - Error stack traces
   - Correlation IDs
   - Timestamps

4. **Reproduction Steps**
   - Exact API calls
   - Request/response payloads
   - Sequence of events

### Common Commands Reference

```bash
# Health check
./scripts/health-check.sh

# Check all services
docker compose ps

# View service metrics
curl http://localhost:8082/actuator/metrics

# Check Kafka topics
docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Redis monitoring
docker exec -it redis redis-cli info

# PostgreSQL monitoring
docker exec -it postgres psql -U leaky_user -c "SELECT * FROM pg_stat_activity;"
```

---

**Previous**: [API Reference](08-api-reference.md)  
**Next**: [Development Guide →](10-development.md)
