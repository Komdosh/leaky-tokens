plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Spring Cloud Gateway (WebFlux)
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    // Config Client
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    
    // Eureka Discovery Client
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // Load Balancer for lb:// routes
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    // Load balancer cache (Caffeine)
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    
    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // OAuth2 Resource Server (JWT)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    
    // Actuator for health checks
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    
    // Resilience4j for circuit breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
    
    // Configuration Processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
