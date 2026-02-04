plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    // Config Client
    implementation("org.springframework.cloud:spring-cloud-starter-config")

    // Spring Data Cassandra
    implementation("org.springframework.boot:spring-boot-starter-data-cassandra")

    // OAuth2 Resource Server (JWT)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    // Web (Servlet)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Actuator + Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    
    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    
    // Eureka Discovery Client
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    
    // Actuator for health checks
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Configuration Processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}
