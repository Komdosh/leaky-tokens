# User Guide

This guide explains how to use the Leaky Tokens system for common tasks.

## Table of Contents
1. [Authentication](#authentication)
2. [Managing API Keys](#managing-api-keys)
3. [Checking Token Quotas](#checking-token-quotas)
4. [Consuming Tokens](#consuming-tokens)
5. [Purchasing Additional Tokens](#purchasing-additional-tokens)
6. [Viewing Analytics](#viewing-analytics)
7. [Using the API Gateway](#using-the-api-gateway)
8. [Best Practices](#best-practices)

---

## Authentication

Leaky Tokens supports two authentication methods: JWT tokens and API keys.

### JWT Authentication

Use JWT tokens for interactive API usage and browser-based applications.

#### Register a New User

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "myuser",
    "email": "myuser@example.com",
    "password": "securepassword123"
  }'
```

**Response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "myuser",
  "token": "eyJhbGciOiJSUzI1NiIs...",
  "roles": ["USER"]
}
```

#### Login

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "myuser",
    "password": "securepassword123"
  }'
```

**Using the token:**
```bash
export JWT_TOKEN="eyJhbGciOiJSUzI1NiIs..."

# Use in subsequent requests
curl http://localhost:8082/api/v1/tokens/quota \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### API Key Authentication

API keys are ideal for service-to-service communication and automated scripts.

#### Create an API Key

```bash
curl -X POST http://localhost:8081/api/v1/auth/api-keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "production-api-key",
    "expiresAt": "2026-12-31T23:59:59Z"
  }'
```

**Response:**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "production-api-key",
  "createdAt": "2026-02-05T12:00:00Z",
  "expiresAt": "2026-12-31T23:59:59Z",
  "rawKey": "leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4e5f6"
}
```

**⚠️ Important**: The `rawKey` is only returned once. Store it securely.

**Using the API key:**
```bash
export API_KEY="leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4e5f6"

# Use in requests
curl http://localhost:8080/api/v1/tokens/status \
  -H "X-Api-Key: $API_KEY"
```

---

## Managing API Keys

### List Your API Keys

```bash
curl "http://localhost:8081/api/v1/auth/api-keys?userId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response:**
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "name": "production-api-key",
    "createdAt": "2026-02-05T12:00:00Z",
    "expiresAt": "2026-12-31T23:59:59Z"
  }
]
```

**Note**: Raw key values are never returned in list operations for security.

### Revoke an API Key

```bash
curl -X DELETE "http://localhost:8081/api/v1/auth/api-keys?userId=550e8400-e29b-41d4-a716-446655440000&apiKeyId=660e8400-e29b-41d4-a716-446655440001" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response:** HTTP 204 No Content

**Verification**: Try using the revoked key:
```bash
curl http://localhost:8080/api/v1/tokens/status \
  -H "X-Api-Key: $API_KEY"
# Should return 401 Unauthorized
```

---

## Checking Token Quotas

### Check User Quota

```bash
curl "http://localhost:8082/api/v1/tokens/quota?userId=550e8400-e29b-41d4-a716-446655440000&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "provider": "openai",
  "totalTokens": 1000,
  "remainingTokens": 850,
  "updatedAt": "2026-02-05T12:30:00Z"
}
```

### Check Organization Quota

```bash
curl "http://localhost:8082/api/v1/tokens/quota/org?orgId=10000000-0000-0000-0000-000000000001&provider=openai" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response:**
```json
{
  "orgId": "10000000-0000-0000-0000-000000000001",
  "provider": "openai",
  "totalTokens": 5000,
  "remainingTokens": 3200,
  "updatedAt": "2026-02-05T12:30:00Z"
}
```

### Supported Providers

- `openai` - OpenAI GPT models
- `qwen` - Qwen models
- `gemini` - Google Gemini models

---

## Consuming Tokens

The consume endpoint deducts tokens from quota and calls the AI provider.

### Basic Consumption

```bash
curl -X POST http://localhost:8082/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "provider": "openai",
    "tokens": 50,
    "prompt": "Explain quantum computing in simple terms"
  }'
```

**Response (Success):**
```json
{
  "allowed": true,
  "capacity": 1000,
  "used": 50,
  "remaining": 950,
  "waitSeconds": 0,
  "timestamp": "2026-02-05T12:35:00Z",
  "providerResponse": {
    "id": "resp-uuid-123",
    "provider": "openai",
    "message": "Stubbed OpenAI response"
  }
}
```

### Using Organization Quota

Include the `orgId` field to consume from organization pool:

```bash
curl -X POST http://localhost:8082/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "orgId": "10000000-0000-0000-0000-000000000001",
    "provider": "openai",
    "tokens": 100,
    "prompt": "Generate a summary"
  }'
```

### Error Responses

**Insufficient Quota (HTTP 402):**
```json
{
  "message": "insufficient token quota",
  "timestamp": "2026-02-05T12:40:00Z"
}
```

**Rate Limited (HTTP 429):**
```json
{
  "allowed": false,
  "capacity": 1000,
  "used": 1000,
  "remaining": 0,
  "waitSeconds": 15,
  "timestamp": "2026-02-05T12:40:00Z",
  "providerResponse": {}
}
```

**Provider Failure (HTTP 502):**
```json
{
  "message": "provider call failed",
  "timestamp": "2026-02-05T12:40:00Z"
}
```

### Rate Limit Headers

When using the API Gateway, rate limit headers are included:

```bash
curl -i http://localhost:8080/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: $API_KEY" \
  -d '{...}'
```

**Headers:**
```
X-RateLimit-Limit: 120
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1707144000
```

---

## Purchasing Additional Tokens

When quotas are depleted, users can purchase additional tokens through the SAGA workflow.

### Start a Purchase

```bash
curl -X POST http://localhost:8082/api/v1/tokens/purchase \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Idempotency-Key: purchase-001" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "provider": "openai",
    "tokens": 1000
  }'
```

**Response:**
```json
{
  "sagaId": "770e8400-e29b-41d4-a716-446655440002",
  "status": "STARTED",
  "createdAt": "2026-02-05T12:45:00Z"
}
```

### Check Purchase Status

```bash
curl http://localhost:8082/api/v1/tokens/purchase/770e8400-e29b-41d4-a716-446655440002 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response:**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "orgId": null,
  "provider": "openai",
  "tokens": 1000,
  "idempotencyKey": "purchase-001",
  "status": "COMPLETED",
  "createdAt": "2026-02-05T12:45:00Z",
  "updatedAt": "2026-02-05T12:45:05Z"
}
```

**Status Values:**
- `STARTED` - Purchase initiated
- `PAYMENT_RESERVED` - Payment step completed
- `TOKENS_ALLOCATED` - Tokens added to quota
- `COMPLETED` - Purchase finished successfully
- `FAILED` - Purchase failed (compensation triggered)

### Idempotency

The `Idempotency-Key` header ensures safe retries:

```bash
# Same key + same payload = same saga (no duplicate purchase)
curl -X POST http://localhost:8082/api/v1/tokens/purchase \
  -H "Idempotency-Key: purchase-001" \
  ...
```

---

## Viewing Analytics

### Recent Usage

```bash
curl "http://localhost:8083/api/v1/analytics/usage?provider=openai&limit=20" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response:**
```json
{
  "provider": "openai",
  "count": 20,
  "items": [
    {
      "key": {
        "provider": "openai",
        "timestamp": "2026-02-05T13:00:00Z"
      },
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "tokens": 50,
      "allowed": true
    }
  ]
}
```

### Usage Report

```bash
curl "http://localhost:8083/api/v1/analytics/report?provider=openai&windowMinutes=60" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response:**
```json
{
  "provider": "openai",
  "windowStart": "2026-02-05T12:00:00Z",
  "windowEnd": "2026-02-05T13:00:00Z",
  "totalEvents": 150,
  "allowedEvents": 145,
  "deniedEvents": 5,
  "totalTokens": 3750,
  "averageTokens": 25.0,
  "uniqueUsers": 10,
  "topUsers": [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "totalTokens": 500,
      "events": 20
    }
  ]
}
```

### Anomaly Detection

```bash
curl "http://localhost:8083/api/v1/analytics/anomalies?provider=openai&windowMinutes=60" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Response:**
```json
{
  "provider": "openai",
  "currentWindowStart": "2026-02-05T12:00:00Z",
  "currentWindowEnd": "2026-02-05T13:00:00Z",
  "currentTokens": 5000,
  "baselineAverage": 1500.0,
  "ratio": 3.33,
  "threshold": 2.0,
  "anomaly": true
}
```

---

## Using the API Gateway

The API Gateway (port 8080) is the recommended entry point for production use.

### Gateway Features

- **Rate Limiting**: Configurable per-route limits
- **Authentication**: API key or JWT validation
- **Load Balancing**: Service discovery integration
- **Circuit Breaker**: Automatic failover

### Example: Using API Key

```bash
export API_KEY="leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4e5f6"

# Check status
curl http://localhost:8080/api/v1/tokens/status \
  -H "X-Api-Key: $API_KEY"

# Check quota
curl "http://localhost:8080/api/v1/tokens/quota?userId=550e8400-e29b-41d4-a716-446655440000&provider=openai" \
  -H "X-Api-Key: $API_KEY"

# Consume tokens
curl -X POST http://localhost:8080/api/v1/tokens/consume \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: $API_KEY" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "provider": "openai",
    "tokens": 25,
    "prompt": "Hello"
  }'
```

### Example: Using JWT

```bash
export JWT_TOKEN="eyJhbGciOiJSUzI1NiIs..."

curl http://localhost:8080/api/v1/tokens/quota \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -G -d "userId=550e8400-e29b-41d4-a716-446655440000" \
  -d "provider=openai"
```

---

## Best Practices

### 1. Handle Rate Limits Gracefully

```python
import time
import requests

def consume_with_retry(url, headers, data, max_retries=3):
    for attempt in range(max_retries):
        response = requests.post(url, headers=headers, json=data)
        
        if response.status_code == 200:
            return response.json()
        elif response.status_code == 429:
            wait = response.json().get('waitSeconds', 60)
            time.sleep(wait)
        else:
            response.raise_for_status()
    
    raise Exception("Max retries exceeded")
```

### 2. Use Idempotency Keys

Always include idempotency keys for purchase operations to prevent duplicate charges:

```python
import uuid

idempotency_key = str(uuid.uuid4())
headers['Idempotency-Key'] = idempotency_key
```

### 3. Monitor Quota Before Requests

Check quota availability before making consumption requests:

```python
def check_quota(user_id, provider, token):
    response = requests.get(
        f"http://localhost:8080/api/v1/tokens/quota",
        headers={"Authorization": f"Bearer {token}"},
        params={"userId": user_id, "provider": provider}
    )
    if response.status_code == 200:
        return response.json()['remainingTokens']
    return 0
```

### 4. Handle Token Expiration

Implement token refresh logic:

```python
def get_valid_token():
    if is_token_expired(current_token):
        response = requests.post(
            "http://localhost:8081/api/v1/auth/login",
            json={"username": username, "password": password}
        )
        return response.json()['token']
    return current_token
```

### 5. Use Appropriate Authentication

- **Interactive apps**: JWT tokens (short-lived, user-specific)
- **Server-to-server**: API keys (long-lived, service-specific)
- **Scripts/CLI**: API keys with limited scope

### 6. Error Handling

Always handle these HTTP status codes:
- `400` - Invalid request (check parameters)
- `401` - Authentication failed (refresh token)
- `402` - Insufficient quota (prompt user to purchase)
- `429` - Rate limited (back off and retry)
- `502` - Provider error (retry with backoff)

---

**Previous**: [Getting Started](02-getting-started.md)  
**Next**: [Architecture →](04-architecture.md)
