# 📊 Leaky Tokens Grafana Dashboards

This directory contains comprehensive Grafana dashboards for monitoring the Leaky Tokens platform.

## 🎯 Available Dashboards

### 1. 🚀 Executive Overview (`leaky-tokens-overview.json`)

**Purpose:** High-level overview of the entire platform for executives and managers

**Key Metrics:**
- 🔥 Total requests per minute across all services
- 💰 Tokens consumed per minute
- ⏱️ Average latency (p95)
- ❌ Error rates
- 🚦 Rate limiting activity
- 👥 Active users

**Visualizations:**
- Request rate by service (time series)
- Response latency percentiles
- Token consumption distribution (donut chart)
- Consumption outcomes (bar gauge)
- JVM heap usage (gauge)
- Latency heatmap
- Top API endpoints table

**Use Case:** Daily health checks, executive reporting, capacity planning

---

### 2. 💼 Business Metrics (`leaky-tokens-business.json`)

**Purpose:** Business-focused metrics for product managers and business analysts

**Key Metrics:**
- 💰 Total tokens consumed (24h)
- ✅ Successful consumptions
- 🚫 Denied requests (quota insufficient)
- 🚦 Rate limited requests
- ❌ Provider failures
- 💵 SAGA purchase completions

**Visualizations:**
- Token consumption by provider (time series)
- Consumption outcomes over time (stacked area)
- Provider distribution (pie chart)
- Outcomes breakdown (bar gauge)
- Quota lookup activity
- Top users by consumption (table)
- SAGA purchase activity
- Provider success rates
- Hourly consumption patterns

**Use Case:** Product analytics, billing verification, user behavior analysis

**Key Queries:**
```promql
# Tokens consumed today
sum(increase(token_consume_total{outcome="allowed"}[24h]))

# Top users
sum by (userId) (increase(token_consume_total{outcome="allowed"}[24h]))
```

---

### 3. 🛡️ Gateway & Security (`leaky-tokens-gateway.json`)

**Purpose:** API Gateway, authentication, and security monitoring

**Key Metrics:**
- 🔑 API key validations per minute
- ✅ Validation success rate
- 💾 Cache hit rate
- 🚦 Rate limit blocks
- 🚪 Auth registrations
- 🔐 Auth logins

**Visualizations:**
- API key validation results (success/failure)
- Cache performance (hits/misses)
- Rate limiting activity
- Auth activity breakdown (donut)
- Auth failures by reason
- Gateway latency heatmap
- API key operations

**Use Case:** Security monitoring, API key management, rate limiting analysis

**Key Queries:**
```promql
# API key validation success rate
sum(rate(gateway_api_key_validation_total{outcome="success"}[5m])) 
  / sum(rate(gateway_api_key_validation_total[5m]))

# Cache hit rate
sum(rate(gateway_api_key_cache_total{outcome="hit"}[5m])) 
  / sum(rate(gateway_api_key_cache_total[5m]))
```

---

### 4. ⚙️ System Health (`leaky-tokens-system.json`)

**Purpose:** Infrastructure and system-level monitoring for DevOps

**Key Metrics:**
- 🖥️ Services up count
- 💾 Heap memory usage
- 🗄️ Database connections
- 🧵 Thread count
- 🗑️ Garbage collection time
- ⚡ CPU load

**Visualizations:**
- JVM heap memory by service
- Database connection pool (active/idle/max)
- JVM threads (live/peak)
- Garbage collection activity
- Heap usage gauges
- JVM classes loaded
- System load averages
- Service status overview table

**Use Case:** Infrastructure monitoring, capacity planning, troubleshooting

**Key Queries:**
```promql
# Average heap usage
avg(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"})

# GC time per minute
sum(rate(jvm_gc_pause_seconds_sum[1m]))
```

---

## 📈 Dashboard Variables

All dashboards include template variables for filtering:

- **service** - Filter by microservice name
- **provider** - Filter by AI provider (openai, qwen, gemini)
- **time_range** - Quick time range selection
- **auth_outcome** - Filter by authentication outcome

## 🚀 Accessing Dashboards

1. Start the observability stack:
   ```bash
   docker-compose -f docker-compose.observability.yml up -d
   ```

2. Open Grafana:
   ```
   http://localhost:3000
   ```

3. Login with default credentials:
   - Username: `admin`
   - Password: `admin`

4. Navigate to **Dashboards → Leaky Tokens** folder

## 🎨 Customization

### Modifying Dashboards

Dashboards support UI updates (`allowUiUpdates: true`). You can:
- Add new panels
- Modify queries
- Change visualization types
- Update thresholds
- Add annotations

Changes are persisted in Grafana's database.

### Creating New Dashboards

1. Create a new JSON file in this directory
2. Follow the naming convention: `leaky-tokens-{name}.json`
3. Set a unique UID
4. Add appropriate tags
5. Restart Grafana or wait for auto-refresh (10s)

### Exporting Dashboard Changes

If you make changes via UI and want to save them:

1. Go to Dashboard → Settings → JSON Model
2. Copy the JSON
3. Save to file in this directory
4. Commit to version control

## 📊 Prometheus Data Source

Dashboards expect a Prometheus data source named `Prometheus`.

**Configuration:**
- URL: `http://prometheus:9090`
- Access: Server

If using a different name, update the datasource in provisioning:
```yaml
# grafana/provisioning/datasources/datasource.yml
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
```

## 🎯 Recommended Alerting Thresholds

Based on the dashboards, recommended alerting rules:

| Metric | Warning | Critical |
|--------|---------|----------|
| Error Rate | > 1% | > 5% |
| P95 Latency | > 500ms | > 1s |
| Heap Usage | > 70% | > 85% |
| Auth Failures | > 10/min | > 50/min |
| Rate Limit Blocks | > 20/min | > 100/min |
| Provider Failures | > 5/5min | > 20/5min |

## 📚 Metrics Reference

All dashboards use metrics from:
- **Token Service** - `token_consume_total`, `token_quota_lookup_total`
- **Gateway** - `gateway_api_key_validation_total`, `gateway_rate_limit_total`
- **Auth** - `auth_login_total`, `auth_register_total`, `auth_api_key_*`
- **Analytics** - `analytics_*_total`
- **System** - `http_*`, `jvm_*`, `jdbc_*`, `system_*`

See [Monitoring Documentation](../../docs/07-monitoring.md) for full metrics reference.

---

## 🤝 Contributing

When adding new dashboards:

1. Use consistent naming and tagging
2. Include descriptive titles with emojis
3. Add helpful descriptions
4. Use appropriate units and thresholds
5. Include both instant and time series views where applicable
6. Add table panels for detailed data
7. Use color coding (green/yellow/red) for status
8. Include template variables for filtering

## 📝 Notes

- Dashboards auto-refresh every 10 seconds by default
- Timezone is set to browser local time
- All panels support tooltips with detailed information
- Use the time picker to adjust the time range
- Star dashboards for quick access from the home page

---

**Need Help?** Check the [Troubleshooting Guide](../../docs/09-troubleshooting.md) or [Monitoring Documentation](../../docs/07-monitoring.md)
