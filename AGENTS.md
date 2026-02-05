# Leaky Tokens - Agent Guide

## Project Overview
Microservices-based token API gateway using Java 25, Spring Boot 4.x, and Gradle Kotlin DSL.

## Build Commands

### Full Build
```bash
./gradlew build
```

### Clean Build
```bash
./gradlew clean build
```

### Run All Tests
```bash
./gradlew test
```

### Run Single Test Class
```bash
./gradlew :<module>:test --tests "com.leaky.tokens.<module>.<TestClass>"
# Example:
./gradlew :token-service:test --tests "com.leaky.tokens.tokenservice.quota.TokenQuotaServiceTest"
```

### Run Single Test Method
```bash
./gradlew :<module>:test --tests "com.leaky.tokens.<module>.<TestClass>.<methodName>"
# Example:
./gradlew :token-service:test --tests "com.leaky.tokens.tokenservice.quota.TokenQuotaServiceTest.reserveReturnsAllowedWhenFeatureDisabled"
```

### Code Coverage
```bash
./gradlew jacocoTestReport      # Generate HTML/XML reports
./gradlew jacocoTestCoverageVerification  # Verify 80% branch coverage
```

### Run Service
```bash
./gradlew :<module>:bootRun
# Example:
./gradlew :token-service:bootRun
```

## Code Style Guidelines

### General
- **Java Version**: 25
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: 120 characters max
- **Files**: UTF-8 encoding, LF line endings

### Imports
- Use explicit imports (no wildcards except `java.util.*`, `java.io.*`)
- Group order: java.*, jakarta.*, org.*, com.*, static imports
- Remove unused imports

### Naming Conventions
- **Classes**: PascalCase (e.g., `TokenController`, `TokenQuotaService`)
- **Methods**: camelCase, verbs (e.g., `reserveTokens()`, `getQuota()`)
- **Variables**: camelCase (e.g., `userId`, `tokenBucket`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_CAPACITY`)
- **Packages**: lowercase, reverse domain (e.g., `com.leaky.tokens.tokenservice`)

### Types & Classes
- Prefer `Optional<T>` over null returns
- Use `final` for method parameters and local variables when possible
- Use `var` only when type is obvious from RHS
- Use Lombok annotations: `@Data`, `@Builder`, `@RequiredArgsConstructor`

### Error Handling
- Use custom exceptions extending `RuntimeException`
- Exception names end with "Exception" (e.g., `ProviderCallException`)
- Include context in exception messages
- Use Spring's `@ControllerAdvice` for global exception handling

### Testing
- JUnit 5 (Jupiter) with `@Test` annotations
- AssertJ for assertions: `assertThat(result).isEqualTo(expected)`
- Mockito for mocking: `@Mock`, `@InjectMocks`
- Test class naming: `<ClassUnderTest>Test`
- Test method naming: descriptive lowercase (e.g., `reserveReturnsAllowedWhenFeatureDisabled`)

### Spring Conventions
- Constructor injection (no `@Autowired` on fields)
- Use `@RestController`, `@Service`, `@Repository` stereotypes
- Configuration properties classes use `@ConfigurationProperties` with records
- REST endpoints: `/api/v1/<resource>/<action>`

### Documentation
- OpenAPI annotations for controllers: `@Operation`, `@ApiResponse`
- Tag endpoints with `@Tag(name = "...")`
- JavaDoc for public APIs (optional but encouraged)

## Project Structure
```
leaky-tokens/
├── api-gateway/          # Spring Cloud Gateway
├── auth-server/          # OAuth2 authorization
├── config-server/        # Configuration management
├── service-discovery/    # Eureka server
├── token-service/        # Core leaky bucket logic
├── analytics-service/    # Metrics and events
├── qwen-stub/           # Stub provider
├── gemini-stub/         # Stub provider
├── openai-stub/         # Stub provider
└── performance-tests/   # Load tests
```

## Infrastructure
```bash
# Start infrastructure only
docker-compose -f docker-compose.infra.yml up -d

# Start all services with infrastructure
docker-compose up -d
```

## Dependencies
- Spring Boot 4.x, Spring Cloud 2025.1.1
- Lombok 1.18.40, MapStruct 1.6.2
- PostgreSQL, Redis, Kafka, Cassandra
- Prometheus, Grafana, Jaeger
