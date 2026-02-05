# API Reference

Complete reference for all REST APIs in the Leaky Tokens system.

## Table of Contents
1. [Base URLs](#base-urls)
2. [Authentication](#authentication)
3. [Auth Server API](#auth-server-api)
4. [Token Service API](#token-service-api)
5. [Analytics Service API](#analytics-service-api)
6. [API Gateway Routes](#api-gateway-routes)
7. [Error Responses](#error-responses)
8. [Rate Limiting](#rate-limiting)

---

## Base URLs

| Service | Development | Production |
|---------|-------------|------------|
| API Gateway | http://localhost:8080 | https://api.leaky-tokens.com |
| Auth Server | http://localhost:8081 | https://auth.leaky-tokens.com |
| Token Service | http://localhost:8082 | https://tokens.leaky-tokens.com |
| Analytics Service | http://localhost:8083 | https://analytics.leaky-tokens.com |

---

## Authentication

### JWT Authentication

Include JWT token in Authorization header:

```http
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

### API Key Authentication

Include API key in X-Api-Key header:

```http
X-Api-Key: leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4e5f6
```

---

## Auth Server API

### Register User

Create a new user account.

**Endpoint:** `POST /api/v1/auth/register`

**Request:**
```http
POST /api/v1/auth/register HTTP/1.1
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Response (201 Created):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "token": "eyJhbGciOiJSUzI1NiIs...",
  "roles": ["USER"],
  "createdAt": "2026-02-05T10:00:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid input or duplicate username/email
- `409 Conflict` - Username or email already exists

---

### Login

Authenticate user and obtain JWT token.

**Endpoint:** `POST /api/v1/auth/login`

**Request:**
```http
POST /api/v1/auth/login HTTP/1.1
Content-Type: application/json

{
  "username": "johndoe",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "token": "eyJhbGciOiJSUzI1NiIs...",
  "roles": ["USER"],
  "expiresAt": "2026-02-05T11:00:00Z"
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid credentials

---

### Create API Key

Create a new API key for programmatic access.

**Endpoint:** `POST /api/v1/auth/api-keys`

**Headers:**
- `Authorization: Bearer <jwt_token>` (required)
- `Content-Type: application/json` (required)

**Request:**
```http
POST /api/v1/auth/api-keys HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "production-key",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

**Response (201 Created):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "production-key",
  "createdAt": "2026-02-05T10:30:00Z",
  "expiresAt": "2026-12-31T23:59:59Z",
  "rawKey": "leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4e5f6"
}
```

**⚠️ Note:** The `rawKey` is only returned once. Store it securely.

**Error Responses:**
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing or invalid JWT
- `403 Forbidden` - Cannot create keys for other users (unless ADMIN)

---

### List API Keys

List all API keys for a user.

**Endpoint:** `GET /api/v1/auth/api-keys`

**Headers:**
- `Authorization: Bearer <jwt_token>` (required)

**Query Parameters:**
- `userId` (required) - UUID of the user

**Request:**
```http
GET /api/v1/auth/api-keys?userId=550e8400-e29b-41d4-a716-446655440000 HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response (200 OK):**
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "name": "production-key",
    "createdAt": "2026-02-05T10:30:00Z",
    "expiresAt": "2026-12-31T23:59:59Z"
  }
]
```

**Error Responses:**
- `400 Bad Request` - Missing userId
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - Cannot list keys for other users (unless ADMIN)

---

### Revoke API Key

Revoke an existing API key.

**Endpoint:** `DELETE /api/v1/auth/api-keys`

**Headers:**
- `Authorization: Bearer <jwt_token>` (required)

**Query Parameters:**
- `userId` (required) - UUID of the user
- `apiKeyId` (required) - UUID of the API key to revoke

**Request:**
```http
DELETE /api/v1/auth/api-keys?userId=550e8400-e29b-41d4-a716-446655440000&apiKeyId=660e8400-e29b-41d4-a716-446655440001 HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response:** `204 No Content`

**Error Responses:**
- `400 Bad Request` - Missing parameters
- `401 Unauthorized` - Not authenticated
- `403 Forbidden` - Cannot revoke keys for other users (unless ADMIN)
- `404 Not Found` - API key not found

---

### Validate API Key

Validate an API key (used internally by API Gateway).

**Endpoint:** `GET /api/v1/auth/api-keys/validate`

**Headers:**
- `X-Api-Key: <api_key>` (required)

**Request:**
```http
GET /api/v1/auth/api-keys/validate HTTP/1.1
X-Api-Key: leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4e5f6
```

**Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "production-key",
  "expiresAt": "2026-12-31T23:59:59Z",
  "roles": ["USER"]
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or expired API key

---

### Get JWKS

Retrieve JSON Web Key Set for JWT validation.

**Endpoint:** `GET /oauth2/jwks`

**Request:**
```http
GET /oauth2/jwks HTTP/1.1
```

**Response (200 OK):**
```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "key-1",
      "use": "sig",
      "n": "...",
      "e": "AQAB"
    }
  ]
}
```

---

## Token Service API

### Service Status

Check Token Service health.

**Endpoint:** `GET /api/v1/tokens/status`

**Security:** Public

**Request:**
```http
GET /api/v1/tokens/status HTTP/1.1
```

**Response (200 OK):**
```json
{
  "service": "token-service",
  "status": "ok",
  "timestamp": "2026-02-05T10:00:00Z",
  "version": "0.0.1-SNAPSHOT"
}
```

---

### Get User Quota

Retrieve token quota for a user.

**Endpoint:** `GET /api/v1/tokens/quota`

**Security:** Bearer JWT required, ROLE_USER

**Query Parameters:**
- `userId` (required) - User UUID
- `provider` (required) - Provider name (openai, qwen, gemini)

**Request:**
```http
GET /api/v1/tokens/quota?userId=550e8400-e29b-41d4-a716-446655440000&provider=openai HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response (200 OK):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "provider": "openai",
  "totalTokens": 1000,
  "remainingTokens": 850,
  "resetTime": "2026-02-06T10:00:00Z",
  "updatedAt": "2026-02-05T10:30:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid userId format or missing parameters
- `401 Unauthorized` - Missing or invalid JWT
- `404 Not Found` - Quota not found for user/provider

---

### Get Organization Quota

Retrieve token quota for an organization.

**Endpoint:** `GET /api/v1/tokens/quota/org`

**Security:** Bearer JWT required, ROLE_USER

**Query Parameters:**
- `orgId` (required) - Organization UUID
- `provider` (required) - Provider name

**Request:**
```http
GET /api/v1/tokens/quota/org?orgId=10000000-0000-0000-0000-000000000001&provider=openai HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response (200 OK):**
```json
{
  "orgId": "10000000-0000-0000-0000-000000000001",
  "provider": "openai",
  "totalTokens": 5000,
  "remainingTokens": 3200,
  "resetTime": "2026-02-06T10:00:00Z",
  "updatedAt": "2026-02-05T10:30:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid orgId format or missing parameters
- `401 Unauthorized` - Missing or invalid JWT
- `404 Not Found` - Quota not found for org/provider

---

### Consume Tokens

Consume tokens to make an AI provider call.

**Endpoint:** `POST /api/v1/tokens/consume`

**Security:** Bearer JWT required, ROLE_USER

**Headers:**
- `Authorization: Bearer <jwt_token>` (required)
- `Content-Type: application/json` (required)

**Request Body:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "orgId": "10000000-0000-0000-0000-000000000001",
  "provider": "openai",
  "tokens": 50,
  "prompt": "Explain quantum computing"
}
```

**Fields:**
- `userId` (required) - User UUID
- `orgId` (optional) - Organization UUID (uses org quota if provided)
- `provider` (required) - Provider name: `openai`, `qwen`, `gemini`
- `tokens` (required) - Number of tokens to consume (1-10000)
- `prompt` (optional) - Input text for AI provider

**Request:**
```http
POST /api/v1/tokens/consume HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "provider": "openai",
  "tokens": 50,
  "prompt": "Explain quantum computing"
}
```

**Response (200 OK) - Success:**
```json
{
  "allowed": true,
  "capacity": 1000,
  "used": 50,
  "remaining": 950,
  "waitSeconds": 0,
  "timestamp": "2026-02-05T10:35:00Z",
  "providerResponse": {
    "id": "resp-uuid-123",
    "provider": "openai",
    "message": "Stubbed OpenAI response"
  }
}
```

**Response (402 Payment Required) - Insufficient Quota:**
```json
{
  "message": "insufficient token quota",
  "timestamp": "2026-02-05T10:35:00Z"
}
```

**Response (429 Too Many Requests) - Rate Limited:**
```json
{
  "allowed": false,
  "capacity": 1000,
  "used": 1000,
  "remaining": 0,
  "waitSeconds": 15,
  "timestamp": "2026-02-05T10:35:00Z",
  "providerResponse": {}
}
```

**Response (502 Bad Gateway) - Provider Failure:**
```json
{
  "message": "provider call failed",
  "timestamp": "2026-02-05T10:35:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid input (missing fields, invalid UUID, negative tokens)
- `401 Unauthorized` - Missing or invalid JWT

---

### Start Token Purchase

Initiate a token purchase SAGA workflow.

**Endpoint:** `POST /api/v1/tokens/purchase`

**Security:** Bearer JWT required, ROLE_USER

**Headers:**
- `Authorization: Bearer <jwt_token>` (required)
- `Content-Type: application/json` (required)
- `Idempotency-Key: <key>` (optional) - Max 100 characters

**Request Body:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "orgId": "10000000-0000-0000-0000-000000000001",
  "provider": "openai",
  "tokens": 1000
}
```

**Fields:**
- `userId` (required) - User UUID
- `orgId` (optional) - Organization UUID
- `provider` (required) - Provider name
- `tokens` (required) - Number of tokens to purchase (1-1000000)

**Request:**
```http
POST /api/v1/tokens/purchase HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
Content-Type: application/json
Idempotency-Key: purchase-001

{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "provider": "openai",
  "tokens": 1000
}
```

**Response (202 Accepted):**
```json
{
  "sagaId": "770e8400-e29b-41d4-a716-446655440002",
  "status": "STARTED",
  "createdAt": "2026-02-05T10:45:00Z"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing or invalid JWT
- `409 Conflict` - Idempotency key reuse with different payload

---

### Get SAGA Status

Retrieve the status of a token purchase SAGA.

**Endpoint:** `GET /api/v1/tokens/purchase/{sagaId}`

**Security:** Bearer JWT required, ROLE_USER

**Path Parameters:**
- `sagaId` (required) - SAGA UUID

**Request:**
```http
GET /api/v1/tokens/purchase/770e8400-e29b-41d4-a716-446655440002 HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response (200 OK):**
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "orgId": null,
  "provider": "openai",
  "tokens": 1000,
  "idempotencyKey": "purchase-001",
  "status": "COMPLETED",
  "createdAt": "2026-02-05T10:45:00Z",
  "updatedAt": "2026-02-05T10:45:05Z"
}
```

**Status Values:**
- `STARTED` - Purchase initiated
- `PAYMENT_RESERVED` - Payment step completed
- `TOKENS_ALLOCATED` - Tokens added to quota
- `COMPLETED` - Purchase completed successfully
- `FAILED` - Purchase failed

**Error Responses:**
- `401 Unauthorized` - Missing or invalid JWT
- `404 Not Found` - SAGA not found

---

## Analytics Service API

### Service Health

Check Analytics Service health.

**Endpoint:** `GET /api/v1/analytics/health`

**Security:** Public

**Request:**
```http
GET /api/v1/analytics/health HTTP/1.1
```

**Response (200 OK):**
```json
{
  "service": "analytics-service",
  "status": "ok",
  "timestamp": "2026-02-05T10:00:00Z"
}
```

---

### Get Usage by Provider

Retrieve recent token usage events for a provider.

**Endpoint:** `GET /api/v1/analytics/usage`

**Security:** Bearer JWT required, ROLE_USER

**Query Parameters:**
- `provider` (required) - Provider name
- `limit` (optional) - Maximum events to return (default: 20, max: 200)

**Request:**
```http
GET /api/v1/analytics/usage?provider=openai&limit=20 HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response (200 OK):**
```json
{
  "provider": "openai",
  "count": 20,
  "items": [
    {
      "key": {
        "provider": "openai",
        "timestamp": "2026-02-05T14:30:00Z"
      },
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "tokens": 50,
      "allowed": true
    }
  ]
}
```

**Error Responses:**
- `400 Bad Request` - Missing provider parameter
- `401 Unauthorized` - Missing or invalid JWT

---

### Generate Usage Report

Generate an aggregated usage report for a time window.

**Endpoint:** `GET /api/v1/analytics/report`

**Security:** Bearer JWT required, ROLE_USER

**Query Parameters:**
- `provider` (required) - Provider name
- `windowMinutes` (optional) - Time window in minutes (default: 60)
- `limit` (optional) - Maximum events to sample (default: 1000)

**Request:**
```http
GET /api/v1/analytics/report?provider=openai&windowMinutes=60 HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response (200 OK):**
```json
{
  "provider": "openai",
  "windowStart": "2026-02-05T13:30:00Z",
  "windowEnd": "2026-02-05T14:30:00Z",
  "totalEvents": 150,
  "allowedEvents": 145,
  "deniedEvents": 5,
  "totalTokens": 3750,
  "averageTokens": 25.0,
  "uniqueUsers": 10,
  "sampleLimit": 1000,
  "topUsers": [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "totalTokens": 500,
      "events": 20
    }
  ]
}
```

**Error Responses:**
- `400 Bad Request` - Missing provider parameter
- `401 Unauthorized` - Missing or invalid JWT

---

### Detect Anomalies

Detect anomalies in token usage patterns.

**Endpoint:** `GET /api/v1/analytics/anomalies`

**Security:** Bearer JWT required, ROLE_USER

**Query Parameters:**
- `provider` (required) - Provider name
- `windowMinutes` (optional) - Current window size (default: 60)
- `baselineWindows` (optional) - Number of historical windows for baseline (default: 3)
- `thresholdMultiplier` (optional) - Anomaly threshold multiplier (default: 2.0)
- `limit` (optional) - Maximum events to sample (default: 1000)

**Request:**
```http
GET /api/v1/analytics/anomalies?provider=openai&windowMinutes=60&thresholdMultiplier=2.0 HTTP/1.1
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

**Response (200 OK):**
```json
{
  "provider": "openai",
  "currentWindowStart": "2026-02-05T13:30:00Z",
  "currentWindowEnd": "2026-02-05T14:30:00Z",
  "baselineWindows": 3,
  "currentTokens": 5000,
  "baselineAverage": 1500.0,
  "ratio": 3.33,
  "threshold": 2.0,
  "anomaly": true,
  "sampleLimit": 1000
}
```

**Error Responses:**
- `400 Bad Request` - Missing provider parameter
- `401 Unauthorized` - Missing or invalid JWT

---

## API Gateway Routes

The API Gateway (port 8080) proxies requests to backend services. It also provides rate limiting and authentication.

### Gateway Endpoints

All endpoints available on individual services are also available through the Gateway:

**Base URL:** `http://localhost:8080`

**Routes:**
- `/api/v1/tokens/**` → Token Service
- `/api/v1/auth/**` → Auth Server
- `/api/v1/analytics/**` → Analytics Service

### Authentication via Gateway

**Using API Key:**
```http
GET /api/v1/tokens/quota?userId=...&provider=openai HTTP/1.1
Host: localhost:8080
X-Api-Key: leaky_550e8400-e29b-41d4-a716-446655440000_a1b2c3d4e5f6
```

**Using JWT:**
```http
GET /api/v1/tokens/quota?userId=...&provider=openai HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
```

### Rate Limit Headers

When using the Gateway, rate limit information is included in response headers:

```http
HTTP/1.1 200 OK
X-RateLimit-Limit: 120
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1707144000
Content-Type: application/json

{
  "service": "token-service",
  "status": "ok"
}
```

**Headers:**
- `X-RateLimit-Limit` - Maximum requests allowed in window
- `X-RateLimit-Remaining` - Remaining requests in current window
- `X-RateLimit-Reset` - Unix timestamp when window resets

---

## Error Responses

### Standard Error Format

All errors follow a consistent format:

```json
{
  "message": "Human-readable error description",
  "timestamp": "2026-02-05T10:00:00Z",
  "path": "/api/v1/tokens/consume",
  "status": 400
}
```

### HTTP Status Codes

| Status | Description | Common Causes |
|--------|-------------|---------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 202 | Accepted | Request accepted for async processing |
| 204 | No Content | Success, no body (e.g., DELETE) |
| 400 | Bad Request | Invalid input, validation error |
| 401 | Unauthorized | Missing or invalid authentication |
| 402 | Payment Required | Insufficient quota |
| 403 | Forbidden | Authenticated but not authorized |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (e.g., idempotency) |
| 429 | Too Many Requests | Rate limit exceeded |
| 502 | Bad Gateway | Provider service error |
| 503 | Service Unavailable | Service temporarily unavailable |

### Validation Errors

Detailed validation errors include field-level information:

```json
{
  "message": "Validation failed",
  "timestamp": "2026-02-05T10:00:00Z",
  "errors": [
    {
      "field": "userId",
      "message": "Invalid UUID format"
    },
    {
      "field": "tokens",
      "message": "Must be positive"
    }
  ]
}
```

---

## Rate Limiting

### Gateway Rate Limits

Default limits (configurable):
- **Capacity**: 120 requests per window
- **Window**: 60 seconds
- **Key Strategy**: API_KEY_HEADER (or IP if no API key)

**Headers on 429 Response:**
```http
HTTP/1.1 429 Too Many Requests
Retry-After: 45
X-RateLimit-Limit: 120
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1707144045
```

### Token Service Rate Limits

Rate limiting at the Token Service level uses the leaky bucket algorithm:
- **Capacity**: 1000 tokens
- **Leak Rate**: 10 tokens per second
- **Storage**: Redis (production) or In-Memory (local)

---

## OpenAPI/Swagger

Interactive API documentation is available at:

```
http://localhost:8082/swagger-ui.html  # Token Service
http://localhost:8081/swagger-ui.html  # Auth Server
http://localhost:8083/swagger-ui.html  # Analytics Service
```

OpenAPI specification JSON:

```
http://localhost:8082/v3/api-docs
http://localhost:8081/v3/api-docs
http://localhost:8083/v3/api-docs
```

---

**Previous**: [Monitoring](07-monitoring.md)  
**Next**: [Troubleshooting →](09-troubleshooting.md)
