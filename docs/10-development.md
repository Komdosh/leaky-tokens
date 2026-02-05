# Development Guide

Guide for developers contributing to the Leaky Tokens project.

## Table of Contents
1. [Development Environment](#development-environment)
2. [Project Structure](#project-structure)
3. [Code Style](#code-style)
4. [Testing](#testing)
5. [Database Migrations](#database-migrations)
6. [Adding New Features](#adding-new-features)
7. [Debugging](#debugging)
8. [Pull Request Process](#pull-request-process)
9. [Best Practices](#best-practices)

---

## Development Environment

### IDE Setup

#### IntelliJ IDEA

1. **Import Project**
   - File → Open → Select `build.gradle.kts`
   - Wait for Gradle sync

2. **Configure Project**
   - Settings → Build → Compiler → Annotation Processors
   - Enable annotation processing
   - Obtain processors from project classpath

3. **Run Configurations**
   - Create Spring Boot run configurations for each service
   - Set active profile to `dev` or `local`

4. **Plugins Recommended**
   - Lombok
   - MapStruct Support
   - Spring Boot
   - Docker

#### VS Code

1. **Required Extensions**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Gradle for Java
   - Lombok Annotations Support

2. **Settings**
   ```json
   {
     "java.configuration.updateBuildConfiguration": "automatic",
     "java.compile.nullAnalysis.mode": "automatic"
   }
   ```

### Local Development Profile

Use the `local` profile to run without external dependencies:

```bash
# Uses H2 database instead of PostgreSQL
./gradlew :token-service:bootRun --args='--spring.profiles.active=local'
```

### Hot Reload

**Spring Boot DevTools:**
```groovy
// In build.gradle.kts
developmentOnly("org.springframework.boot:spring-boot-devtools")
```

Automatic restart on code changes (compile with Ctrl+F9 in IntelliJ).

---

## Project Structure

```
leaky-tokens/
├── api-gateway/              # Spring Cloud Gateway
│   ├── src/main/java/
│   │   └── com/leaky/tokens/apigateway/
│   │       ├── config/       # Configuration classes
│   │       ├── filter/       # Gateway filters
│   │       └── security/     # Security configuration
│   └── src/test/java/
│
├── auth-server/              # Authentication service
│   ├── src/main/java/
│   │   └── com/leaky/tokens/authserver/
│   │       ├── config/
│   │       ├── controller/   # REST controllers
│   │       ├── model/        # JPA entities
│   │       ├── repository/   # Spring Data repositories
│   │       └── service/      # Business logic
│   └── src/test/java/
│
├── token-service/            # Core token management
│   ├── src/main/java/
│   │   └── com/leaky/tokens/tokenservice/
│   │       ├── bucket/       # Token bucket implementation
│   │       ├── controller/
│   │       ├── dto/          # Data transfer objects
│   │       ├── events/       # Event publishing
│   │       ├── outbox/       # Outbox pattern
│   │       ├── quota/        # Quota management
│   │       ├── saga/         # SAGA orchestration
│   │       └── tier/         # Tier configuration
│   └── src/test/java/
│
├── analytics-service/        # Usage analytics
│   ├── src/main/java/
│   └── src/test/java/
│
├── service-discovery/        # Eureka server
├── config-server/            # Configuration server
├── qwen-stub/                # Mock AI provider
├── gemini-stub/              # Mock AI provider
├── openai-stub/              # Mock AI provider
└── performance-tests/        # Load testing

├── docs/                     # Documentation
├── scripts/                  # Utility scripts
├── build.gradle.kts          # Root build configuration
└── settings.gradle.kts       # Project settings
```

---

## Code Style

### Java Conventions

We follow standard Java conventions with these specifics:

#### Formatting
```java
// 4 spaces indentation
public class TokenController {
    // No tabs
    private final TokenService tokenService;
    
    // 120 character line limit
    public ResponseEntity<TokenConsumeResponse> consume(
            @RequestBody TokenConsumeRequest request) {
        // Implementation
    }
}
```

#### Naming
- **Classes**: `PascalCase` - `TokenController`, `TokenQuotaService`
- **Methods**: `camelCase` - `consumeTokens()`, `getQuota()`
- **Variables**: `camelCase` - `userId`, `tokenBucket`
- **Constants**: `UPPER_SNAKE_CASE` - `DEFAULT_CAPACITY`, `MAX_TOKENS`
- **Packages**: lowercase, reverse domain - `com.leaky.tokens.tokenservice`

#### Imports
```java
// Group order: java.*, jakarta.*, org.*, com.*, static imports
import java.time.Instant;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.leaky.tokens.tokenservice.dto.TokenConsumeRequest;

import static org.assertj.core.api.Assertions.assertThat;
```

#### Types & Null Safety
```java
// Prefer Optional over null returns
public Optional<TokenPool> findByUserId(UUID userId) {
    return repository.findByUserId(userId);
}

// Use final for parameters and local variables when possible
public void process(final UUID userId, final long tokens) {
    final TokenPool pool = findPool(userId);
    // ...
}

// Use var only when type is obvious
var response = new TokenConsumeResponse(true, 100, 50, 50, 0, Instant.now(), data);
```

### Lombok Usage

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenConsumeRequest {
    @NotNull
    private UUID userId;
    
    @NotBlank
    private String provider;
    
    @Min(1)
    private long tokens;
    
    private String prompt;
}
```

### Spring Conventions

#### Constructor Injection
```java
@Service
public class TokenQuotaService {
    private final TokenPoolRepository repository;
    private final TokenQuotaProperties properties;
    
    // Constructor injection (preferred)
    public TokenQuotaService(
            TokenPoolRepository repository,
            TokenQuotaProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }
}
```

#### REST Controllers
```java
@RestController
@RequestMapping("/api/v1/tokens")
@Tag(name = "Tokens", description = "Token management API")
public class TokenController {
    
    @GetMapping("/quota")
    @Operation(summary = "Get user quota")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "404", description = "Quota not found")
    })
    public ResponseEntity<QuotaResponse> getQuota(
            @RequestParam @Parameter(description = "User ID") UUID userId,
            @RequestParam @Parameter(description = "Provider") String provider) {
        // Implementation
    }
}
```

---

## Testing

### Test Structure

```
src/test/java/com/leaky/tokens/
├── unit/                    # Unit tests
│   ├── service/
│   └── util/
├── integration/             # Integration tests
│   ├── repository/
│   └── controller/
└── contract/                # Contract tests
```

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class TokenQuotaServiceTest {
    
    @Mock
    private TokenPoolRepository repository;
    
    @InjectMocks
    private TokenQuotaService service;
    
    @Test
    void reserveReturnsAllowedWhenFeatureDisabled() {
        // Given
        TokenServiceFeatureFlags flags = new TokenServiceFeatureFlags();
        flags.setQuotaEnforcement(false);
        
        TokenQuotaService service = new TokenQuotaService(
            repository, orgRepository, properties, flags);
        
        // When
        TokenQuotaReservation reservation = service.reserve(
            UUID.randomUUID(), "openai", 50, null);
        
        // Then
        assertThat(reservation.allowed()).isTrue();
        assertThat(reservation.remaining()).isEqualTo(Long.MAX_VALUE);
    }
    
    @Test
    void reserveRejectsWhenPoolMissing() {
        // Given
        when(repository.findForUpdate(any(), any()))
            .thenReturn(Optional.empty());
        
        // When
        TokenQuotaReservation reservation = service.reserve(
            UUID.randomUUID(), "openai", 50, null);
        
        // Then
        assertThat(reservation.allowed()).isFalse();
        verify(repository, never()).save(any());
    }
}
```

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class TokenControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private TokenPoolRepository repository;
    
    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }
    
    @Test
    void shouldReturnQuotaWhenExists() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        TokenPool pool = TokenPool.builder()
            .userId(userId)
            .provider("openai")
            .totalTokens(1000)
            .remainingTokens(850)
            .build();
        repository.save(pool);
        
        // When & Then
        mockMvc.perform(get("/api/v1/tokens/quota")
                .param("userId", userId.toString())
                .param("provider", "openai")
                .header("Authorization", "Bearer " + getJwtToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remainingTokens").value(850));
    }
}
```

### Test Naming Convention

```java
// Descriptive method names
@Test
void reserveReturnsAllowedWhenFeatureDisabled() { }

@Test
void reserveRejectsWhenPoolMissing() { }

@Test
void reserveAppliesQuotaCapAndPersists() { }

// Format: [method][ExpectedBehavior][WhenCondition]
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :token-service:test

# Run specific test class
./gradlew :token-service:test --tests "TokenQuotaServiceTest"

# Run specific test method
./gradlew :token-service:test --tests "TokenQuotaServiceTest.reserveReturnsAllowedWhenFeatureDisabled"

# Run with coverage
./gradlew jacocoTestReport

# Open coverage report
open token-service/build/reports/jacoco/test/html/index.html
```

---

## Database Migrations

### Flyway Migration Files

Location: `src/main/resources/db/migration/`

**Naming Convention:**
```
V1__Initial_schema.sql
V2__Add_token_pools.sql
V3__Add_saga_tables.sql
V4__Add_outbox_table.sql
```

**Example Migration:**
```sql
-- V1__Initial_schema.sql

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

### Running Migrations

```bash
# Run migrations
./gradlew flywayMigrate

# Clean and re-run (development only!)
./gradlew flywayClean flywayMigrate

# Validate migrations
./gradlew flywayValidate

# Get migration info
./gradlew flywayInfo
```

### Creating New Migrations

1. Create file: `src/main/resources/db/migration/V{version}__{description}.sql`
2. Write idempotent SQL (use IF NOT EXISTS)
3. Test locally
4. Commit with PR

---

## Adding New Features

### Feature Checklist

- [ ] Feature flag added (if needed)
- [ ] Tests written (unit + integration)
- [ ] API documentation updated (OpenAPI)
- [ ] Database migration (if needed)
- [ ] Configuration documented
- [ ] Metrics added (if needed)
- [ ] Logging added
- [ ] Error handling implemented

### Example: Adding New Provider

1. **Add Provider Stub**
   ```java
   // gemini-stub/src/.../GeminiController.java
   @RestController
   public class GeminiController {
       @PostMapping("/api/v1/gemini/chat")
       public ResponseEntity<ProviderResponse> chat(@RequestBody Object request) {
           return ResponseEntity.ok(ProviderResponse.builder()
               .provider("gemini")
               .message("Stubbed Gemini response")
               .build());
       }
   }
   ```

2. **Update Provider Registry**
   ```java
   // token-service/src/.../provider/ProviderRegistry.java
   public enum ProviderType {
       OPENAI("openai"),
       QWEN("qwen"),
       GEMINI("gemini");  // New
       
       // ...
   }
   ```

3. **Add Tests**
   ```java
   @Test
   void shouldSupportGeminiProvider() {
       // Test implementation
   }
   ```

4. **Update Documentation**
   - API reference
   - Configuration guide
   - Use cases

---

## Debugging

### Remote Debugging

1. **Enable Debug Mode**
   ```bash
   ./gradlew :token-service:bootRun \
     --args='--debug-jvm' \
     --jvm-args='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005'
   ```

2. **Connect from IDE**
   - IntelliJ: Run → Edit Configurations → Add Remote JVM Debug
   - Port: 5005

### Actuator Endpoints

```bash
# Health
curl http://localhost:8082/actuator/health

# Metrics
curl http://localhost:8082/actuator/metrics

# Specific metric
curl http://localhost:8082/actuator/metrics/http.server.requests

# Environment
curl http://localhost:8082/actuator/env

# Beans
curl http://localhost:8082/actuator/beans

# Config props
curl http://localhost:8082/actuator/configprops

# Thread dump
curl http://localhost:8082/actuator/threaddump

# Heap dump
curl http://localhost:8082/actuator/heapdump
```

### Log Levels at Runtime

```bash
# Change log level without restart
curl -X POST http://localhost:8082/actuator/loggers/com.leaky.tokens \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

# View current levels
curl http://localhost:8082/actuator/loggers/com.leaky.tokens
```

---

## Pull Request Process

### Before Creating PR

1. **Run Tests**
   ```bash
   ./gradlew clean test
   ```

2. **Check Code Coverage**
   ```bash
   ./gradlew jacocoTestReport
   # Ensure > 80% branch coverage
   ```

3. **Static Analysis**
   ```bash
   ./gradlew checkstyleMain
   ./gradlew spotbugsMain
   ```

4. **Format Code**
   ```bash
   ./gradlew spotlessApply
   ```

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Checklist
- [ ] Code follows style guidelines
- [ ] Tests pass locally
- [ ] Documentation updated
- [ ] Feature flags added (if applicable)

## Screenshots (if applicable)
```

### Code Review Guidelines

**As Author:**
- Keep PRs focused (one feature/fix per PR)
- Write clear commit messages
- Respond to feedback promptly
- Rebase on main before merging

**As Reviewer:**
- Check for test coverage
- Verify error handling
- Review security implications
- Test edge cases
- Approve only when satisfied

---

## Best Practices

### 1. Defensive Programming

```java
public TokenQuotaReservation reserve(UUID userId, String provider, long tokens) {
    // Validate inputs
    if (userId == null) {
        throw new IllegalArgumentException("userId cannot be null");
    }
    if (tokens <= 0) {
        throw new IllegalArgumentException("tokens must be positive");
    }
    
    // Check feature flag
    if (!featureFlags.isQuotaEnforcement()) {
        return TokenQuotaReservation.allowed(Long.MAX_VALUE);
    }
    
    // Business logic
    // ...
}
```

### 2. Proper Exception Handling

```java
@Service
public class ProviderCallService {
    private static final Logger log = LoggerFactory.getLogger(ProviderCallService.class);
    
    public ProviderResponse call(String provider, ProviderRequest request) {
        try {
            return webClient.post()
                .uri("/api/v1/" + provider + "/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ProviderResponse.class)
                .timeout(Duration.ofSeconds(5))
                .block();
        } catch (WebClientResponseException e) {
            log.error("Provider call failed: provider={}, status={}", 
                provider, e.getStatusCode(), e);
            throw new ProviderCallException(
                "Provider call failed: " + provider, e);
        } catch (TimeoutException e) {
            log.error("Provider call timeout: provider={}", provider, e);
            throw new ProviderCallException(
                "Provider call timeout: " + provider, e);
        }
    }
}
```

### 3. Transactional Boundaries

```java
@Service
@Transactional
public class TokenQuotaService {
    
    public TokenQuotaReservation reserve(UUID userId, String provider, long tokens) {
        // Find with pessimistic lock
        TokenPool pool = repository.findForUpdate(userId, provider)
            .orElseThrow(() -> new QuotaNotFoundException(userId, provider));
        
        // Check and deduct
        if (pool.getRemainingTokens() < tokens) {
            return TokenQuotaReservation.rejected(pool.getRemainingTokens());
        }
        
        pool.deduct(tokens);
        repository.save(pool);
        
        return TokenQuotaReservation.allowed(pool.getRemainingTokens());
    }
}
```

### 4. Event Publishing

```java
@Service
public class TokenUsagePublisher {
    
    private final TokenOutboxRepository outboxRepository;
    
    @Transactional
    public void publish(TokenUsageEvent event) {
        // Write to outbox (same transaction as business logic)
        TokenOutboxEntry entry = TokenOutboxEntry.builder()
            .aggregateType("TokenUsage")
            .aggregateId(event.getUserId().toString())
            .eventType("TOKEN_USAGE_RECORDED")
            .payload(toJson(event))
            .createdAt(Instant.now())
            .build();
        
        outboxRepository.save(entry);
    }
}
```

### 5. Configuration Properties

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "token.bucket")
@Validated
public class TokenBucketProperties {
    
    @Min(1)
    @Max(100000)
    private int capacity = 1000;
    
    @Positive
    private double leakRatePerSecond = 10.0;
    
    @NotNull
    private TokenBucketStrategy strategy = TokenBucketStrategy.LEAKY_BUCKET;
    
    @NotNull
    private Duration entryTtl = Duration.ofHours(6);
}
```

### 6. Testing Best Practices

```java
@Test
void shouldHandleConcurrentConsumption() throws InterruptedException {
    // Given
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    
    // When
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                TokenQuotaReservation reservation = service.reserve(
                    userId, "openai", 10, null);
                if (reservation.allowed()) {
                    successCount.incrementAndGet();
                }
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();
    
    // Then
    TokenPool pool = repository.findByUserIdAndProvider(userId, "openai")
        .orElseThrow();
    
    assertThat(successCount.get()).isEqualTo(10);
    assertThat(pool.getRemainingTokens()).isEqualTo(990);
}
```

---

## Resources

### Documentation
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud Gateway](https://cloud.spring.io/spring-cloud-gateway/reference/html/)
- [Resilience4j](https://resilience4j.readme.io/)
- [Project Lombok](https://projectlombok.org/features/all)

### Tools
- [MapStruct](https://mapstruct.org/)
- [AssertJ](https://assertj.github.io/doc/)
- [TestContainers](https://www.testcontainers.org/)

### Community
- GitHub Issues
- Stack Overflow (tag: leaky-tokens)
- Team Slack Channel

---

**Previous**: [Troubleshooting](09-troubleshooting.md)  
**Back to**: [Documentation Home](README.md)
