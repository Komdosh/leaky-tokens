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

    // Config Client
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    
    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // Spring Data JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Actuator + Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Flyway for migrations
    implementation("org.flywaydb:flyway-core")
    
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
    testImplementation("org.springframework.security:spring-security-test")
}
