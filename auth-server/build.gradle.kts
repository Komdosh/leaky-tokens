plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Spring Security OAuth2 Authorization Server
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")

    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // OpenAPI (Swagger UI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")

    // Config Client
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    
    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // Spring Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Load balancer cache (Caffeine)
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Actuator + Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Flyway for migrations
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql:12.0.0")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    
    // PostgreSQL Driver
    runtimeOnly("org.postgresql:postgresql")
    
    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // Eureka Discovery Client
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    
    // Actuator for health checks
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Configuration Processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.springframework.security:spring-security-test")
}
