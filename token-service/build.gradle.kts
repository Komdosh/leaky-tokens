plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Config Client
    implementation("org.springframework.cloud:spring-cloud-starter-config")

    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // OpenAPI (Swagger UI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")

    // WebClient for provider calls
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Resilience4j for retries/circuit breaker
    implementation("io.github.resilience4j:resilience4j-spring-boot3")

    // OAuth2 Resource Server (JWT)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // PostgreSQL Driver
    runtimeOnly("org.postgresql:postgresql")
    
    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Jackson 3 (tools.jackson) for Redis serialization and outbox payloads
    implementation("tools.jackson.core:jackson-databind")
    
    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Actuator + Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Flyway for migrations
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql:12.0.0")
    implementation("org.springframework.boot:spring-boot-starter-flyway")

    // Testcontainers for integration tests
    testImplementation("org.testcontainers:junit-jupiter:1.20.6")
    testImplementation("org.testcontainers:postgresql:1.20.6")
    
    // Eureka Discovery Client
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    
    // Actuator for health checks
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Configuration Processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
