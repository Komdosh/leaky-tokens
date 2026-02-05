# Use Cases & Specifications

This document describes the business use cases supported by Leaky Tokens and their technical specifications.

## Table of Contents
1. [Use Case Overview](#use-case-overview)
2. [AI Platform Subscription Management](#use-case-1-ai-platform-subscription-management)
3. [Enterprise AI Governance](#use-case-2-enterprise-ai-governance)
4. [Multi-Tenant SaaS Platform](#use-case-3-multi-tenant-saas-platform)
5. [Developer API Platform](#use-case-4-developer-api-platform)
6. [Usage Specifications](#usage-specifications)
7. [Business Rules](#business-rules)

---

## Use Case Overview

Leaky Tokens supports four primary business scenarios, each with specific requirements and workflows.

| Use Case | Target User | Key Features | Complexity |
|----------|-------------|--------------|------------|
| AI Platform Subscription | SaaS Providers | Quota tiers, billing integration | High |
| Enterprise AI Governance | IT Departments | RBAC, audit trails, cost allocation | Medium |
| Multi-Tenant SaaS | Platform Providers | Tenant isolation, white-label | High |
| Developer API Platform | API Providers | Rate limits, usage tracking | Medium |

---

## Use Case 1: AI Platform Subscription Management

### Business Context

A SaaS company provides AI services to customers and needs to:
- Enforce API quotas based on subscription tiers
- Track usage for billing purposes
- Allow customers to purchase additional tokens
- Prevent resource hogging by single users

### Actors

- **Platform Administrator**: Manages system configuration
- **Customer**: End user with subscription
- **Billing System**: External billing integration
- **AI Providers**: OpenAI, Qwen, Gemini

### User Stories

#### Story 1.1: Tier-Based Quotas
```
As a platform administrator
I want to assign different token quotas based on subscription tiers
So that customers pay for what they use
```

**Acceptance Criteria:**
- Bronze tier: 1,000 tokens/day
- Silver tier: 10,000 tokens/day
- Gold tier: 100,000 tokens/day
- Enterprise tier: Custom quotas

**Implementation:**
```yaml
token:
  tiers:
    defaultTier: BRONZE
    levels:
      BRONZE:
        priority: 1
        quotaMaxTokens: 1000
      SILVER:
        priority: 2
        quotaMaxTokens: 10000
      GOLD:
        priority: 3
        quotaMaxTokens: 100000
```

#### Story 1.2: Quota Purchase
```
As a customer
I want to purchase additional tokens when I run out
So that I can continue using the service
```

**Workflow:**
1. Customer attempts to use service with depleted quota
2. System returns HTTP 402 (Payment Required)
3. Customer initiates purchase via UI
4. System starts SAGA purchase workflow
5. Payment is processed
6. Tokens are added to quota
7. Customer can resume usage

**API Flow:**
```bash
# Attempt consumption with depleted quota
curl -X POST /api/v1/tokens/consume \
  -d '{"userId": "...", "provider": "openai", "tokens": 100}'
# Returns: HTTP 402

# Initiate purchase
curl -X POST /api/v1/tokens/purchase \
  -H "Idempotency-Key: purchase-001" \
  -d '{"userId": "...", "provider": "openai", "tokens": 5000}'
# Returns: sagaId and status

# Check purchase status
curl /api/v1/tokens/purchase/{sagaId}
# Returns: SAGA status
```

#### Story 1.3: Usage-Based Billing
```
As a platform administrator
I want to track exact token consumption per customer
So that I can generate accurate invoices
```

**Analytics Query:**
```bash
# Get usage report for billing period
curl "/api/v1/analytics/report?provider=openai&windowMinutes=1440" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Response:**
```json
{
  "provider": "openai",
  "windowStart": "2026-02-01T00:00:00Z",
  "windowEnd": "2026-02-01T23:59:59Z",
  "totalTokens": 15000,
  "uniqueUsers": 25,
  "topUsers": [
    {"userId": "...", "totalTokens": 5000, "events": 50}
  ]
}
```

---

## Use Case 2: Enterprise AI Governance

### Business Context

A large enterprise wants to control AI service usage across departments:
- Department-level quotas
- Integration with corporate SSO
- Audit trails for compliance
- Cost allocation by department

### Actors

- **IT Administrator**: Configures policies
- **Department Manager**: Manages team quotas
- **Employee**: Uses AI services
- **Compliance Officer**: Reviews audit logs

### User Stories

#### Story 2.1: Organization Quotas
```
As an IT administrator
I want to allocate token quotas by department
So that I can control costs and ensure fair usage
```

**Acceptance Criteria:**
- HR Department: 50,000 tokens/month
- Engineering: 200,000 tokens/month
- Marketing: 30,000 tokens/month

**Implementation:**
```bash
# Create organization quota
curl -X POST /api/v1/tokens/quota/org \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "orgId": "dept-engineering-uuid",
    "provider": "openai",
    "totalTokens": 200000
  }'
```

**Usage:**
```bash
# Employee consumes from department quota
curl -X POST /api/v1/tokens/consume \
  -H "Authorization: Bearer $EMPLOYEE_TOKEN" \
  -d '{
    "userId": "employee-uuid",
    "orgId": "dept-engineering-uuid",
    "provider": "openai",
    "tokens": 100
  }'
```

#### Story 2.2: Audit Trail
```
As a compliance officer
I want to review all AI service usage
So that I can ensure regulatory compliance
```

**Audit Log Access:**
```bash
# Query all usage for user
curl "/api/v1/analytics/usage?provider=openai&limit=1000" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Log Entry Structure:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "provider": "openai",
  "tokens": 50,
  "allowed": true,
  "timestamp": "2026-02-05T14:30:00Z",
  "requestId": "req-uuid-123",
  "ipAddress": "192.168.1.100"
}
```

#### Story 2.3: Cost Allocation
```
As a finance manager
I want to allocate AI service costs by department
So that I can charge departments appropriately
```

**Monthly Report:**
```bash
# Generate monthly usage by organization
curl "/api/v1/analytics/report?provider=openai&windowMinutes=43200" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Cost Calculation:**
```
Engineering Department:
- Tokens consumed: 180,000
- Rate: $0.002 per token
- Total cost: $360
```

---

## Use Case 3: Multi-Tenant SaaS Platform

### Business Context

A platform provider serves multiple tenants (companies) with complete isolation:
- Tenant-specific quotas
- White-label API keys
- Tenant-specific analytics
- Custom rate limits per tenant

### Actors

- **Platform Provider**: Manages the platform
- **Tenant Admin**: Manages their company's usage
- **Tenant Users**: End users within a tenant

### User Stories

#### Story 3.1: Tenant Isolation
```
As a platform provider
I want complete data isolation between tenants
So that tenants cannot see each other's data
```

**Implementation Strategy:**
- User IDs are globally unique (UUID)
- Organization IDs represent tenants
- All queries filter by user/org ID
- JWT tokens include tenant context

**Security Enforcement:**
```java
@PreAuthorize("hasRole('USER') && #userId == authentication.principal")
public ResponseEntity<?> getQuota(String userId, ...) {
    // User can only access their own data
}
```

#### Story 3.2: Tenant-Specific Rate Limits
```
As a tenant admin
I want custom rate limits for my users
So that I can control internal usage
```

**Configuration per Tenant:**
```yaml
gateway:
  rate-limit:
    routes:
      tenant-acme:
        capacity: 500
        windowSeconds: 60
      tenant-globex:
        capacity: 1000
        windowSeconds: 60
```

#### Story 3.3: White-Label API Keys
```
As a platform provider
I want tenants to issue their own API keys
So that they can integrate with their systems
```

**API Key Creation:**
```bash
# Tenant admin creates API key for their user
curl -X POST /api/v1/auth/api-keys \
  -H "Authorization: Bearer $TENANT_ADMIN_TOKEN" \
  -d '{
    "userId": "tenant-user-uuid",
    "name": "production-integration",
    "tenantId": "tenant-acme-uuid"
  }'
```

---

## Use Case 4: Developer API Platform

### Business Context

An API provider wants to offer AI services to developers:
- Free tier for development
- Rate limits to prevent abuse
- Usage analytics
- Self-service API key management

### Actors

- **Developer**: Registers and uses the API
- **API Provider**: Manages the platform
- **Support Team**: Helps developers

### User Stories

#### Story 4.1: Self-Service Registration
```
As a developer
I want to register and get an API key without human intervention
So that I can start developing immediately
```

**Self-Service Flow:**
```bash
# 1. Register account
curl -X POST /api/v1/auth/register \
  -d '{"username": "dev123", "email": "dev@example.com", "password": "..."}'

# 2. Get JWT token (automatic on registration)
# 3. Create API key
curl -X POST /api/v1/auth/api-keys \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"userId": "...", "name": "dev-key"}'

# 4. Start using API with API key
curl http://localhost:8080/api/v1/tokens/status \
  -H "X-Api-Key: $API_KEY"
```

#### Story 4.2: Free Tier Management
```
As an API provider
I want to offer a free tier with limited usage
So that developers can try the service
```

**Free Tier Configuration:**
```yaml
token:
  tiers:
    FREE:
      priority: 0
      quotaMaxTokens: 100
      bucketCapacityMultiplier: 0.5
      bucketLeakRateMultiplier: 0.5
```

**Upgrade Prompt:**
```bash
# Developer hits free tier limit
curl /api/v1/tokens/consume \
  -H "X-Api-Key: $FREE_TIER_KEY" \
  -d '{"tokens": 150}'

# Response: HTTP 402
{
  "message": "insufficient token quota",
  "upgradeUrl": "https://platform.example.com/upgrade"
}
```

#### Story 4.3: Developer Dashboard
```
As a developer
I want to see my usage statistics
So that I can optimize my application
```

**Usage Analytics:**
```bash
# Get personal usage
curl /api/v1/analytics/usage?provider=openai \
  -H "Authorization: Bearer $JWT_TOKEN"

# Get rate limit status
curl -i /api/v1/tokens/status \
  -H "X-Api-Key: $API_KEY"
# Response headers:
# X-RateLimit-Limit: 120
# X-RateLimit-Remaining: 45
```

---

## Usage Specifications

### Token Consumption Specification

#### Request Format
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "orgId": "10000000-0000-0000-0000-000000000001",
  "provider": "openai",
  "tokens": 50,
  "prompt": "User prompt text"
}
```

**Field Specifications:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userId | UUID | Yes | User identifier |
| orgId | UUID | No | Organization identifier (if using org quota) |
| provider | String | Yes | AI provider: openai, qwen, gemini |
| tokens | Integer | Yes | Number of tokens to consume (1-10000) |
| prompt | String | No | Input text for AI provider |

#### Response Format

**Success (HTTP 200):**
```json
{
  "allowed": true,
  "capacity": 1000,
  "used": 50,
  "remaining": 950,
  "waitSeconds": 0,
  "timestamp": "2026-02-05T15:00:00Z",
  "providerResponse": {
    "id": "resp-uuid",
    "provider": "openai",
    "message": "AI response text"
  }
}
```

**Insufficient Quota (HTTP 402):**
```json
{
  "message": "insufficient token quota",
  "timestamp": "2026-02-05T15:00:00Z"
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
  "timestamp": "2026-02-05T15:00:00Z",
  "providerResponse": {}
}
```

### Purchase Specification

#### Request Format
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "orgId": "10000000-0000-0000-0000-000000000001",
  "provider": "openai",
  "tokens": 5000
}
```

**Headers:**
| Header | Required | Description |
|--------|----------|-------------|
| Idempotency-Key | No | Max 100 chars, ensures safe retries |
| Authorization | Yes | Bearer JWT token |

#### SAGA States

| State | Description | Terminal |
|-------|-------------|----------|
| STARTED | SAGA initiated | No |
| PAYMENT_RESERVED | Payment step complete | No |
| TOKENS_ALLOCATED | Tokens added to quota | No |
| COMPLETED | SAGA finished successfully | Yes |
| FAILED | SAGA failed, compensation triggered | Yes |

**State Transitions:**
```
STARTED → PAYMENT_RESERVED → TOKENS_ALLOCATED → COMPLETED
   ↓
FAILED
```

---

## Business Rules

### Quota Management Rules

1. **Quota Deduction**: Tokens are deducted from quota BEFORE calling provider
2. **Quota Release**: If provider call fails, reserved tokens are released
3. **Quota Reset**: Quotas reset automatically based on window configuration (default: 24 hours)
4. **Tier Cap**: Quota cannot exceed tier's `quotaMaxTokens` limit

### Rate Limiting Rules

1. **Leaky Bucket**: Tokens leak at constant rate (default: 10/second)
2. **Capacity**: Bucket has maximum capacity (default: 1000)
3. **Overflow**: Requests exceeding capacity are rejected with wait time
4. **Recovery**: Bucket refills automatically over time

### SAGA Rules

1. **Idempotency**: Same idempotency key + same payload = same SAGA
2. **Compensation**: Failed SAGAs trigger compensation events
3. **Timeout**: SAGAs timeout after 10 minutes (configurable)
4. **Recovery**: Stuck SAGAs are recovered by scheduled job

### Security Rules

1. **User Isolation**: Users can only access their own data
2. **Role Enforcement**: ADMIN role required for cross-user operations
3. **API Key Scope**: API keys are bound to specific users
4. **Token Expiration**: JWT expires after 1 hour, API keys expire based on configuration

### Analytics Rules

1. **Event Publishing**: All consumption attempts publish events (success and failure)
2. **Retention**: Events retained based on Cassandra TTL (default: 90 days)
3. **Aggregation**: Reports calculated on-demand from raw events
4. **Anomaly Detection**: Baseline calculated from historical windows

---

**Previous**: [Architecture](04-architecture.md)  
**Next**: [Configuration →](06-configuration.md)
