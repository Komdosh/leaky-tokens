# Leaky Tokens - Manual Testing Plan

## Table of Contents
1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Test Environment Setup](#test-environment-setup)
4. [Authentication & Authorization Scenarios](#authentication--authorization-scenarios)
5. [Token Quota Management Scenarios](#token-quota-management-scenarios)
6. [Rate Limiting Scenarios](#rate-limiting-scenarios)
7. [Token Consumption Scenarios](#token-consumption-scenarios)
8. [SAGA Purchase Scenarios](#saga-purchase-scenarios)
9. [Analytics Scenarios](#analytics-scenarios)
10. [Provider Stub Scenarios](#provider-stub-scenarios)
11. [Error Handling Scenarios](#error-handling-scenarios)
12. [Performance & Load Testing](#performance--load-testing)

---

## Introduction

**Leaky Tokens** is a microservices-based token API gateway that provides:
- Token quota management for AI provider API consumption
- Rate limiting using leaky bucket algorithm
- SAGA-based token purchase workflows
- Usage analytics and anomaly detection
- Multi-provider support (OpenAI, Qwen, Gemini)

### Architecture Overview

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | Entry point, routing, rate limiting |
| Auth Server | 8081 | JWT & API Key authentication |
| Token Service | 8082 | Core token bucket & quota logic |
| Analytics Service | 8083 | Usage tracking & reporting |
| Service Discovery | 8761 | Eureka service registry |
| Config Server | 8888 | Configuration management |
| Qwen Stub | 8091 | Mock AI provider |
| Gemini Stub | 8092 | Mock AI provider |
| OpenAI Stub | 8093 | Mock AI provider |

---

## Getting Started

### Prerequisites
- Java 25
- Docker & Docker Compose
- cURL or HTTP client (Postman, Insomnia)

### Project Setup

```bash
# Clone and navigate to project
cd leaky-tokens

# Start infrastructure services
docker-compose -f docker-compose.infra.yml up -d

# Wait for services to be ready (PostgreSQL, Redis, Kafka, Cassandra)
sleep 30

# Start all microservices
./gradlew bootRun --parallel

# Or start individual services in separate terminals:
./gradlew :service-discovery:bootRun
./gradlew :config-server:bootRun
./gradlew :auth-server:bootRun
./gradlew :token-service:bootRun
./gradlew :analytics-service:bootRun
./gradlew :api-gateway:bootRun
./gradlew :qwen-stub:bootRun
./gradlew :gemini-stub:bootRun
./gradlew :openai-stub:bootRun
```

### Verify Services

```bash
# Check Eureka dashboard
curl http://localhost:8761

# Check Token Service health
curl http://localhost:8082/api/v1/tokens/status

# Check Auth Server JWKS
curl http://localhost:8081/oauth2/jwks
```

---

## Test Environment Setup

### Test Data Preparation

Before running tests, ensure you have:
1. Valid JWT tokens for authenticated requests
2. API keys for API key authentication
3. Test user IDs and organization IDs

### Helper Script Setup

Create these environment variables for easier testing:

```bash
# Base URLs
export AUTH_URL="http://localhost:8081"
export GATEWAY_URL="http://localhost:8080"
export TOKEN_URL="http://localhost:8082"
export ANALYTICS_URL="http://localhost:8083"

# Test data (will be populated during auth tests)
export USER_ID=""
export JWT_TOKEN=""
export API_KEY=""
```

---

## Authentication & Authorization Scenarios

### Scenario 1.1: User Registration

**Objective**: Verify user registration functionality

**Preconditions**: Auth Server is running

**Steps**:

1. **Register a new user**
```bash
curl -X POST "$AUTH_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "testuser@example.com",
    "password": "password123"
  }'
```

**Expected Result**:
- HTTP 201 Created
- Response contains userId, username, token, and roles
- User has ROLE_USER

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "testuser",
  "token": "eyJhbGciOiJSUzI1NiIs...",
  "roles": ["USER"]
}
```

2. **Attempt duplicate registration (same username)**
```bash
curl -X POST "$AUTH_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "different@example.com",
    "password": "password123"
  }'
```

**Expected Result**:
- HTTP 400 Bad Request
- Error message indicates username already exists

3. **Attempt duplicate registration (same email)**
```bash
curl -X POST "$AUTH_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "differentuser",
    "email": "testuser@example.com",
    "password": "password123"
  }'
```

**Expected Result**:
- HTTP 400 Bad Request
- Error message indicates email already exists

4. **Register with missing fields**
```bash
curl -X POST "$AUTH_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "incomplete"
  }'
```

**Expected Result**:
- HTTP 400 Bad Request
- Validation error for missing required fields

---

### Scenario 1.2: User Login

**Objective**: Verify user authentication and JWT token generation

**Preconditions**: User registered in Scenario 1.1

**Steps**:

1. **Login with valid credentials**
```bash
curl -X POST "$AUTH_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**Expected Result**:
- HTTP 200 OK
- Response contains valid JWT token
- Store token for subsequent tests

```bash
export JWT_TOKEN="eyJhbGciOiJSUzI1NiIs..."
export USER_ID="550e8400-e29b-41d4-a716-446655440000"
```

2. **Login with invalid password**
```bash
curl -X POST "$AUTH_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "wrongpassword"
  }'
```

**Expected Result**:
- HTTP 401 Unauthorized
- Error message indicates invalid credentials

3. **Login with non-existent user**
```bash
curl -X POST "$AUTH_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "nonexistent",
    "password": "password123"
  }'
```

**Expected Result**:
- HTTP 401 Unauthorized

---

### Scenario 1.3: API Key Management

**Objective**: Verify API key creation, listing, and revocation

**Preconditions**: Valid JWT token from Scenario 1.2

**Steps**:

1. **Create API key**
```bash
curl -X POST "$AUTH_URL/api/v1/auth/api-keys" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "'$USER_ID'",
    "name": "cli-access",
    "expiresAt": "2026-12-31T23:59:59Z"
  }'
```

**Expected Result**:
- HTTP 201 Created
- Response contains rawKey (displayed only once!)
- Store API key securely

```bash
export API_KEY="leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4"
```

2. **Create API key without authorization**
```bash
curl -X POST "$AUTH_URL/api/v1/auth/api-keys" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'$USER_ID'",
    "name": "unauthorized"
  }'
```

**Expected Result**:
- HTTP 401 Unauthorized

3. **List API keys**
```bash
curl "$AUTH_URL/api/v1/auth/api-keys?userId=$USER_ID" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 200 OK
- List of API keys (without raw key values)
- Contains metadata: id, name, createdAt, expiresAt

4. **List keys for different user (unauthorized)**
```bash
curl "$AUTH_URL/api/v1/auth/api-keys?userId=00000000-0000-0000-0000-000000000001" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 403 Forbidden (non-admin cannot access other users' keys)

5. **Validate API key via Auth Server**
```bash
curl "$AUTH_URL/api/v1/auth/api-keys/validate" \
  -H "X-Api-Key: $API_KEY"
```

**Expected Result**:
- HTTP 200 OK
- Returns userId, name, expiresAt, roles

6. **Validate invalid API key**
```bash
curl "$AUTH_URL/api/v1/auth/api-keys/validate" \
  -H "X-Api-Key: invalid_key_value"
```

**Expected Result**:
- HTTP 401 Unauthorized

7. **Revoke API key**
```bash
# First, get the key ID from list
curl "$AUTH_URL/api/v1/auth/api-keys?userId=$USER_ID" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Then revoke (replace KEY_ID with actual value)
curl -X DELETE "$AUTH_URL/api/v1/auth/api-keys?userId=$USER_ID&apiKeyId=KEY_ID" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 204 No Content

8. **Verify revoked key no longer works**
```bash
curl "$AUTH_URL/api/v1/auth/api-keys/validate" \
  -H "X-Api-Key: $API_KEY"
```

**Expected Result**:
- HTTP 401 Unauthorized

---

### Scenario 1.4: JWT Token Validation

**Objective**: Verify JWT token expiration and signature validation

**Preconditions**: Valid JWT token

**Steps**:

1. **Access protected endpoint with valid token**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 200 OK (or 404 if no quota yet)

2. **Access protected endpoint without token**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai"
```

**Expected Result**:
- HTTP 401 Unauthorized

3. **Access protected endpoint with malformed token**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer invalid.token.here"
```

**Expected Result**:
- HTTP 401 Unauthorized

---

## Token Quota Management Scenarios

### Scenario 2.1: User Quota Management

**Objective**: Verify user token quota creation and retrieval

**Preconditions**: Authenticated user, Token Service running

**Steps**:

1. **Check quota before creation**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 404 Not Found (no quota exists yet)

2. **Add tokens to quota (via purchase or direct DB insertion)**
   - For testing, you can insert directly into PostgreSQL:

```sql
-- Connect to PostgreSQL
-- docker exec -it postgres psql -U leaky_user -d token_service
INSERT INTO token_pools (id, user_id, provider, total_tokens, remaining_tokens, reset_time, created_at, updated_at)
VALUES (gen_random_uuid(), '$USER_ID', 'openai', 1000, 1000, NOW() + INTERVAL '24 hours', NOW(), NOW());
```

3. **Retrieve user quota**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 200 OK
- Response shows quota details

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "provider": "openai",
  "totalTokens": 1000,
  "remainingTokens": 1000,
  "updatedAt": "2026-02-05T10:00:00Z"
}
```

4. **Retrieve quota with invalid userId format**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=invalid-uuid&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 400 Bad Request
- Error indicates invalid userId format

5. **Retrieve quota without provider parameter**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 400 Bad Request

---

### Scenario 2.2: Organization Quota Management

**Objective**: Verify organization-level token quota management

**Preconditions**: Valid orgId, authenticated user

**Steps**:

1. **Create organization quota (direct DB insertion)**

```sql
-- Use a test org ID
INSERT INTO token_org_pools (id, org_id, provider, total_tokens, remaining_tokens, reset_time, created_at, updated_at)
VALUES (gen_random_uuid(), '10000000-0000-0000-0000-000000000001', 'openai', 5000, 5000, NOW() + INTERVAL '24 hours', NOW(), NOW());
```

2. **Retrieve organization quota**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota/org?orgId=10000000-0000-0000-0000-000000000001&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 200 OK
- Response shows org quota

```json
{
  "orgId": "10000000-0000-0000-0000-000000000001",
  "provider": "openai",
  "totalTokens": 5000,
  "remainingTokens": 5000,
  "updatedAt": "2026-02-05T10:00:00Z"
}
```

3. **Retrieve non-existent org quota**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota/org?orgId=99999999-9999-9999-9999-999999999999&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 404 Not Found

---

## Rate Limiting Scenarios

### Scenario 3.1: Gateway Rate Limiting

**Objective**: Verify API Gateway rate limiting functionality

**Preconditions**: API Gateway running, API key created

**Steps**:

1. **Make requests within rate limit**
```bash
# Make 5 requests quickly
for i in {1..5}; do
  curl -s "$GATEWAY_URL/api/v1/tokens/status" \
    -H "X-Api-Key: $API_KEY" \
    -w "\nHTTP Status: %{http_code}\n"
done
```

**Expected Result**:
- All requests return HTTP 200
- Response headers include rate limit info:
  - `X-RateLimit-Limit: 120`
  - `X-RateLimit-Remaining: 115` (decreasing)
  - `X-RateLimit-Reset: 1707144000`

2. **Check rate limit headers**
```bash
curl -v "$GATEWAY_URL/api/v1/tokens/status" \
  -H "X-Api-Key: $API_KEY" 2>&1 | grep -i "X-RateLimit"
```

**Expected Result**:
- Headers present showing current rate limit state

3. **Exceed rate limit**
```bash
# Make 150 requests rapidly (adjust based on your limit)
for i in {1..150}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    "$GATEWAY_URL/api/v1/tokens/status" \
    -H "X-Api-Key: $API_KEY"
done | sort | uniq -c
```

**Expected Result**:
- First ~120 requests return HTTP 200
- Subsequent requests return HTTP 429 Too Many Requests
- 429 responses include `Retry-After` header

4. **Wait and retry after rate limit**
```bash
# Wait for the time indicated in Retry-After header (e.g., 60 seconds)
sleep 60

# Retry request
curl "$GATEWAY_URL/api/v1/tokens/status" \
  -H "X-Api-Key: $API_KEY" \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected Result**:
- Request succeeds after waiting
- Rate limit headers reset

---

### Scenario 3.2: Token Bucket Rate Limiting

**Objective**: Verify token bucket rate limiting at Token Service level

**Preconditions**: User has quota, Token Service running

**Steps**:

1. **Consume tokens within rate limit**
```bash
for i in {1..10}; do
  curl -s -X POST "$TOKEN_URL/api/v1/tokens/consume" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d '{
      "userId": "'$USER_ID'",
      "provider": "openai",
      "tokens": 10,
      "prompt": "Test prompt"
    }' | jq '.allowed'
done
```

**Expected Result**:
- All requests return `"allowed": true`
- Response includes rate limit info in `providerResponse` (or headers)

2. **Rapid consumption to trigger rate limit**
```bash
# Make many rapid requests
for i in {1..200}; do
  curl -s -X POST "$TOKEN_URL/api/v1/tokens/consume" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d '{
      "userId": "'$USER_ID'",
      "provider": "openai",
      "tokens": 10,
      "prompt": "Test prompt"
    }' | jq -c '{allowed: .allowed, status: .status}'
done
```

**Expected Result**:
- Some requests return HTTP 429
- Response shows `allowed: false` and `waitSeconds > 0`

```json
{
  "allowed": false,
  "capacity": 1000,
  "used": 1000,
  "remaining": 0,
  "waitSeconds": 15,
  "timestamp": "2026-02-05T10:05:00Z",
  "providerResponse": {}
}
```

3. **Verify rate limit recovery**
```bash
# Wait for leak rate to replenish (e.g., 10 seconds)
sleep 10

# Try again
curl -X POST "$TOKEN_URL/api/v1/tokens/consume" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "openai",
    "tokens": 10,
    "prompt": "Test prompt"
  }' | jq '.allowed'
```

**Expected Result**:
- Request succeeds after waiting

---

## Token Consumption Scenarios

### Scenario 4.1: Successful Token Consumption

**Objective**: Verify successful token consumption flow

**Preconditions**: User has sufficient quota, rate limit not exceeded

**Steps**:

1. **Check initial quota**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.remainingTokens'
```

**Expected Result**:
- Returns remaining tokens (e.g., 1000)

2. **Consume tokens successfully**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/consume" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "openai",
    "tokens": 50,
    "prompt": "Hello, AI! Please respond."
  }'
```

**Expected Result**:
- HTTP 200 OK
- Response includes provider response and rate limit info

```json
{
  "allowed": true,
  "capacity": 1000,
  "used": 50,
  "remaining": 950,
  "waitSeconds": 0,
  "timestamp": "2026-02-05T10:10:00Z",
  "providerResponse": {
    "id": "resp-uuid",
    "provider": "openai",
    "message": "Stubbed OpenAI response"
  }
}
```

3. **Verify quota decreased**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.remainingTokens'
```

**Expected Result**:
- Remaining tokens decreased by 50 (e.g., 950)

---

### Scenario 4.2: Insufficient Quota

**Objective**: Verify behavior when quota is insufficient

**Preconditions**: User has low remaining quota

**Steps**:

1. **Set low quota (direct DB)**
```sql
UPDATE token_pools 
SET remaining_tokens = 10 
WHERE user_id = '$USER_ID' AND provider = 'openai';
```

2. **Attempt to consume more than available**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/consume" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "openai",
    "tokens": 50,
    "prompt": "This should fail"
  }' -w "\nHTTP Status: %{http_code}\n"
```

**Expected Result**:
- HTTP 402 Payment Required
- Error response indicates insufficient quota

```json
{
  "message": "insufficient token quota",
  "timestamp": "2026-02-05T10:15:00Z"
}
```

3. **Verify quota unchanged**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.remainingTokens'
```

**Expected Result**:
- Remaining tokens still 10 (no deduction)

---

### Scenario 4.3: Organization Quota Consumption

**Objective**: Verify token consumption using organization quota

**Preconditions**: Organization quota exists (Scenario 2.2)

**Steps**:

1. **Consume using orgId**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/consume" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "'$USER_ID'",
    "orgId": "10000000-0000-0000-0000-000000000001",
    "provider": "openai",
    "tokens": 100,
    "prompt": "Org quota test"
  }'
```

**Expected Result**:
- HTTP 200 OK
- Tokens deducted from organization quota

2. **Verify org quota decreased**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota/org?orgId=10000000-0000-0000-0000-000000000001&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.remainingTokens'
```

**Expected Result**:
- Remaining tokens decreased by 100

---

### Scenario 4.4: Provider Failure Handling

**Objective**: Verify behavior when AI provider fails

**Preconditions**: User has quota

**Steps**:

1. **Stop provider stub**
```bash
# If running standalone, stop the openai-stub service
# This simulates provider unavailability
```

2. **Attempt consumption**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/consume" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "openai",
    "tokens": 25,
    "prompt": "This will fail"
  }' -w "\nHTTP Status: %{http_code}\n"
```

**Expected Result**:
- HTTP 502 Bad Gateway
- Error message indicates provider failure
- Quota should be released (tokens returned)

```json
{
  "message": "provider call failed",
  "timestamp": "2026-02-05T10:20:00Z"
}
```

3. **Restart provider stub**
```bash
./gradlew :openai-stub:bootRun
```

4. **Verify quota was released**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.remainingTokens'
```

**Expected Result**:
- Remaining tokens restored to pre-consumption value

---

## SAGA Purchase Scenarios

### Scenario 5.1: Successful Token Purchase

**Objective**: Verify SAGA-based token purchase workflow

**Preconditions**: Authenticated user, Token Service running

**Steps**:

1. **Check initial quota**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.remainingTokens'
```

**Expected Result**:
- Returns current remaining tokens

2. **Start token purchase SAGA**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/purchase" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: purchase-test-001" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "openai",
    "tokens": 500
  }'
```

**Expected Result**:
- HTTP 202 Accepted
- Returns SAGA ID and status

```json
{
  "sagaId": "660e8400-e29b-41d4-a716-446655440000",
  "status": "STARTED",
  "createdAt": "2026-02-05T10:25:00Z"
}
```

3. **Query SAGA status**
```bash
export SAGA_ID="660e8400-e29b-41d4-a716-446655440000"

curl "$TOKEN_URL/api/v1/tokens/purchase/$SAGA_ID" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 200 OK
- Status progresses: STARTED → PAYMENT_RESERVED → TOKENS_ALLOCATED → COMPLETED

```json
{
  "id": "660e8400-e29b-41d4-a716-446655440000",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "orgId": null,
  "provider": "openai",
  "tokens": 500,
  "idempotencyKey": "purchase-test-001",
  "status": "COMPLETED",
  "createdAt": "2026-02-05T10:25:00Z",
  "updatedAt": "2026-02-05T10:25:05Z"
}
```

4. **Verify quota increased**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.remainingTokens'
```

**Expected Result**:
- Remaining tokens increased by 500

---

### Scenario 5.2: Idempotency Key Reuse

**Objective**: Verify idempotency behavior

**Preconditions**: Previous purchase with idempotency key "purchase-test-001"

**Steps**:

1. **Reuse same idempotency key with same payload**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/purchase" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: purchase-test-001" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "openai",
    "tokens": 500
  }'
```

**Expected Result**:
- HTTP 202 Accepted
- Returns same SAGA ID as before (not a new one)

2. **Reuse idempotency key with different payload**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/purchase" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: purchase-test-001" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "gemini",
    "tokens": 500
  }' -w "\nHTTP Status: %{http_code}\n"
```

**Expected Result**:
- HTTP 409 Conflict
- Error indicates idempotency key reuse with different payload

---

### Scenario 5.3: SAGA Failure and Compensation

**Objective**: Verify SAGA failure handling and compensation

**Preconditions**: Ability to configure feature flags

**Steps**:

1. **Enable failure simulation**
   - Set `token.saga.simulate-failure=true` in application properties
   - Or update config in Config Server
   - Restart Token Service if needed

2. **Start purchase that will fail**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/purchase" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: purchase-fail-001" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "openai",
    "tokens": 1000
  }'
```

**Expected Result**:
- HTTP 202 Accepted (SAGA starts)

3. **Query SAGA status after failure**
```bash
curl "$TOKEN_URL/api/v1/tokens/purchase/purchase-fail-001" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- Status shows FAILED
- Check database for compensation events in token_outbox table

4. **Disable failure simulation**
   - Set `token.saga.simulate-failure=false`
   - Restart Token Service

---

## Analytics Scenarios

### Scenario 6.1: Usage Tracking

**Objective**: Verify token usage events are tracked

**Preconditions**: Analytics Service running, Kafka operational, Cassandra accessible

**Steps**:

1. **Generate usage events**
```bash
# Make several token consumption requests
for i in {1..5}; do
  curl -s -X POST "$TOKEN_URL/api/v1/tokens/consume" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d '{
      "userId": "'$USER_ID'",
      "provider": "openai",
      "tokens": 20,
      "prompt": "Analytics test '$i'"
    }' > /dev/null
done
```

2. **Wait for event processing**
```bash
# Allow time for Kafka consumer to process events
sleep 5
```

3. **Query usage by provider**
```bash
curl "$ANALYTICS_URL/api/v1/analytics/usage?provider=openai&limit=10" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 200 OK
- Response shows recent usage events

```json
{
  "provider": "openai",
  "count": 5,
  "items": [
    {
      "key": {
        "provider": "openai",
        "timestamp": "2026-02-05T10:30:00Z"
      },
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "tokens": 20,
      "allowed": true
    }
  ]
}
```

---

### Scenario 6.2: Usage Report Generation

**Objective**: Verify usage report generation

**Preconditions**: Usage events exist in system

**Steps**:

1. **Generate usage report**
```bash
curl "$ANALYTICS_URL/api/v1/analytics/report?provider=openai&windowMinutes=60" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 200 OK
- Report contains aggregated statistics

```json
{
  "provider": "openai",
  "windowStart": "2026-02-05T09:30:00Z",
  "windowEnd": "2026-02-05T10:30:00Z",
  "totalEvents": 25,
  "allowedEvents": 23,
  "deniedEvents": 2,
  "totalTokens": 500,
  "averageTokens": 20.0,
  "uniqueUsers": 3,
  "sampleLimit": 1000,
  "topUsers": [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "totalTokens": 300,
      "events": 15
    }
  ]
}
```

---

### Scenario 6.3: Anomaly Detection

**Objective**: Verify anomaly detection functionality

**Preconditions**: Baseline usage pattern established

**Steps**:

1. **Check current anomaly status**
```bash
curl "$ANALYTICS_URL/api/v1/analytics/anomalies?provider=openai&windowMinutes=60" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 200 OK
- Shows baseline and current metrics

2. **Generate spike traffic**
```bash
# Rapidly generate many requests to create anomaly
for i in {1..100}; do
  curl -s -X POST "$TOKEN_URL/api/v1/tokens/consume" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d '{
      "userId": "'$USER_ID'",
      "provider": "openai",
      "tokens": 50,
      "prompt": "Spike test"
    }' > /dev/null &
done
wait
```

3. **Check for anomaly detection**
```bash
curl "$ANALYTICS_URL/api/v1/analytics/anomalies?provider=openai&windowMinutes=10" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- `anomaly: true` flag set
- `ratio` shows spike compared to baseline
- `currentTokens` significantly higher than `baselineAverage`

```json
{
  "provider": "openai",
  "currentWindowStart": "2026-02-05T10:25:00Z",
  "currentWindowEnd": "2026-02-05T10:35:00Z",
  "baselineWindows": 3,
  "currentTokens": 5000,
  "baselineAverage": 150.0,
  "ratio": 33.33,
  "threshold": 2.0,
  "anomaly": true,
  "sampleLimit": 1000
}
```

---

## Provider Stub Scenarios

### Scenario 7.1: Provider Stub Testing

**Objective**: Verify all AI provider stubs are functional

**Preconditions**: All stub services running

**Steps**:

1. **Test Qwen Stub**
```bash
curl -X POST "http://localhost:8091/api/v1/qwen/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

**Expected Result**:
- HTTP 200 OK
- Returns stub response

```json
{
  "id": "qwen-response-uuid",
  "provider": "qwen",
  "received": { ... },
  "created": "2026-02-05T10:40:00Z",
  "message": "Stubbed Qwen response"
}
```

2. **Test Gemini Stub**
```bash
curl -X POST "http://localhost:8092/api/v1/gemini/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "contents": [{"parts": [{"text": "Hello"}]}]
  }'
```

**Expected Result**:
- Similar structure with provider="gemini"

3. **Test OpenAI Stub**
```bash
curl -X POST "http://localhost:8093/api/v1/openai/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

**Expected Result**:
- Similar structure with provider="openai"

---

## Error Handling Scenarios

### Scenario 8.1: Input Validation

**Objective**: Verify input validation across endpoints

**Steps**:

1. **Invalid UUID format**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=not-a-uuid&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- HTTP 400 Bad Request

2. **Missing required fields**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/consume" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "provider": "openai"
  }'
```

**Expected Result**:
- HTTP 400 Bad Request
- Error indicates missing userId

3. **Negative token count**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/consume" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "openai",
    "tokens": -10,
    "prompt": "Test"
  }'
```

**Expected Result**:
- HTTP 400 Bad Request
- Error indicates tokens must be positive

4. **Oversized idempotency key**
```bash
curl -X POST "$TOKEN_URL/api/v1/tokens/purchase" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: $(python3 -c 'print("x"*150)')" \
  -d '{
    "userId": "'$USER_ID'",
    "provider": "openai",
    "tokens": 100
  }'
```

**Expected Result**:
- HTTP 400 Bad Request
- Error indicates idempotency key too long

---

### Scenario 8.2: Service Unavailability

**Objective**: Verify circuit breaker and fallback behavior

**Steps**:

1. **Stop Token Service**
```bash
# If running standalone, stop the service
```

2. **Attempt request through Gateway**
```bash
curl "$GATEWAY_URL/api/v1/tokens/status" \
  -H "X-Api-Key: $API_KEY" \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected Result**:
- After circuit breaker opens: HTTP 503 Service Unavailable
- Fallback response returned

```json
{
  "service": "token-service",
  "status": "unavailable",
  "message": "Circuit breaker open",
  "timestamp": "2026-02-05T10:45:00Z"
}
```

3. **Restart Token Service**
```bash
./gradlew :token-service:bootRun
```

4. **Wait for circuit breaker to close**
```bash
# Wait for half-open state (configurable, typically 30-60 seconds)
sleep 60

# Test again
curl "$GATEWAY_URL/api/v1/tokens/status" \
  -H "X-Api-Key: $API_KEY" \
  -w "\nHTTP Status: %{http_code}\n"
```

**Expected Result**:
- HTTP 200 OK (service recovered)

---

## Performance & Load Testing

### Scenario 9.1: Concurrent Token Consumption

**Objective**: Test system under concurrent load

**Preconditions**: High quota available, provider stubs running

**Steps**:

1. **Prepare high quota**
```sql
UPDATE token_pools 
SET remaining_tokens = 100000 
WHERE user_id = '$USER_ID' AND provider = 'openai';
```

2. **Run concurrent consumption**
```bash
# Run 100 parallel requests
for i in {1..100}; do
  curl -s -X POST "$TOKEN_URL/api/v1/tokens/consume" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d '{
      "userId": "'$USER_ID'",
      "provider": "openai",
      "tokens": 10,
      "prompt": "Concurrent test '$i'"
    }' | jq -c '{allowed: .allowed, status: .status}' &
done
wait
```

**Expected Result**:
- All requests complete
- Some may be rate limited (429)
- No data corruption (consistent quota tracking)

3. **Verify quota consistency**
```bash
curl "$TOKEN_URL/api/v1/tokens/quota?userId=$USER_ID&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Result**:
- Remaining tokens = initial - sum of successful consumptions
- No negative values
- No duplicate deductions

---

### Scenario 9.2: SAGA Load Test

**Objective**: Test SAGA orchestration under load

**Steps**:

1. **Start multiple SAGAs concurrently**
```bash
for i in {1..20}; do
  curl -s -X POST "$TOKEN_URL/api/v1/tokens/purchase" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -H "Idempotency-Key: load-test-$i" \
    -d '{
      "userId": "'$USER_ID'",
      "provider": "openai",
      "tokens": 100
    }' | jq '.sagaId' &
done
wait
```

2. **Monitor SAGA statuses**
```bash
# Query each SAGA
for i in {1..20}; do
  SAGA_ID=$(curl -s "$TOKEN_URL/api/v1/tokens/purchase" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -H "Idempotency-Key: load-test-$i" \
    -d '{
      "userId": "'$USER_ID'",
      "provider": "openai",
      "tokens": 100
    }' | jq -r '.sagaId')
  
  curl -s "$TOKEN_URL/api/v1/tokens/purchase/$SAGA_ID" \
    -H "Authorization: Bearer $JWT_TOKEN" | jq '.status'
done
```

**Expected Result**:
- All SAGAs complete successfully (COMPLETED)
- No duplicate token allocations
- No lost SAGA records

---

## Summary Checklist

### Authentication & Authorization
- [ ] User registration works
- [ ] Duplicate username/email rejected
- [ ] Login returns valid JWT
- [ ] Invalid credentials rejected
- [ ] API key creation works
- [ ] API key validation works
- [ ] API key revocation works
- [ ] JWT token validation works
- [ ] Role-based access control enforced

### Token Quota
- [ ] User quota retrieval works
- [ ] Org quota retrieval works
- [ ] Non-existent quota returns 404
- [ ] Invalid UUID returns 400

### Rate Limiting
- [ ] Gateway rate limiting works
- [ ] Rate limit headers present
- [ ] 429 returned when limit exceeded
- [ ] Token bucket rate limiting works
- [ ] Rate limit recovery works

### Token Consumption
- [ ] Successful consumption deducts quota
- [ ] Insufficient quota returns 402
- [ ] Rate limit exceeded returns 429
- [ ] Provider failure returns 502
- [ ] Quota released on failure
- [ ] Org quota consumption works

### SAGA Purchase
- [ ] SAGA starts successfully
- [ ] Status query works
- [ ] SAGA completes successfully
- [ ] Quota increases after completion
- [ ] Idempotency works (same key, same payload)
- [ ] Idempotency conflict returns 409
- [ ] Failure simulation works
- [ ] Compensation events created

### Analytics
- [ ] Usage events tracked
- [ ] Usage by provider query works
- [ ] Report generation works
- [ ] Anomaly detection works

### Provider Stubs
- [ ] Qwen stub responds
- [ ] Gemini stub responds
- [ ] OpenAI stub responds

### Error Handling
- [ ] Input validation works
- [ ] Circuit breaker triggers
- [ ] Fallback responses work
- [ ] Service recovery works

### Performance
- [ ] Concurrent consumption works
- [ ] Data consistency maintained
- [ ] SAGA load test passes

---

## Appendix: Quick Reference Commands

### Setup
```bash
# Start infrastructure
docker-compose -f docker-compose.infra.yml up -d

# Build all
cd leaky-tokens && ./gradlew build

# Run all services
./gradlew bootRun --parallel
```

### Authentication
```bash
# Register
curl -X POST "$AUTH_URL/api/v1/auth/register" -H "Content-Type: application/json" -d '{"username":"test","email":"test@test.com","password":"pass"}'

# Login
curl -X POST "$AUTH_URL/api/v1/auth/login" -H "Content-Type: application/json" -d '{"username":"test","password":"pass"}'

# Create API key
curl -X POST "$AUTH_URL/api/v1/auth/api-keys" -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"userId":"UUID","name":"key"}'
```

### Token Operations
```bash
# Check quota
curl "$TOKEN_URL/api/v1/tokens/quota?userId=UUID&provider=openai" -H "Authorization: Bearer TOKEN"

# Consume tokens
curl -X POST "$TOKEN_URL/api/v1/tokens/consume" -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d '{"userId":"UUID","provider":"openai","tokens":10,"prompt":"test"}'

# Purchase tokens
curl -X POST "$TOKEN_URL/api/v1/tokens/purchase" -H "Authorization: Bearer TOKEN" -H "Idempotency-Key: key" -H "Content-Type: application/json" -d '{"userId":"UUID","provider":"openai","tokens":100}'

# Check SAGA
curl "$TOKEN_URL/api/v1/tokens/purchase/SAGA_ID" -H "Authorization: Bearer TOKEN"
```

### Analytics
```bash
# Usage by provider
curl "$ANALYTICS_URL/api/v1/analytics/usage?provider=openai" -H "Authorization: Bearer TOKEN"

# Generate report
curl "$ANALYTICS_URL/api/v1/analytics/report?provider=openai" -H "Authorization: Bearer TOKEN"

# Anomaly detection
curl "$ANALYTICS_URL/api/v1/analytics/anomalies?provider=openai" -H "Authorization: Bearer TOKEN"
```

### Gateway Access
```bash
# Via API key
curl "$GATEWAY_URL/api/v1/tokens/status" -H "X-Api-Key: API_KEY"

# Via JWT
curl "$GATEWAY_URL/api/v1/tokens/quota?userId=UUID&provider=openai" -H "Authorization: Bearer TOKEN"
```

---

**Document Version**: 1.0  
**Last Updated**: 2026-02-05  
**Project**: Leaky Tokens Microservices
