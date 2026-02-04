plugins {
    java
    id("org.springframework.boot") version "4.0.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.spring") version "2.3.0" apply false
}

group = "com.leaky.tokens"
version = "0.0.1-SNAPSHOT"

val springCloudVersion = "2025.1.1"

subprojects {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-spring")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

//    plugins.withId("io.spring.dependency-management") {
//        dependencyManagement {
//            imports {
//                mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
//            }
//        }
//    }

    dependencies {
        implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        compileOnly("org.projectlombok:lombok:1.18.40")
        annotationProcessor("org.projectlombok:lombok:1.18.40")

        implementation("org.mapstruct:mapstruct:1.6.2")
        annotationProcessor("org.mapstruct:mapstruct-processor:1.6.2")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.springframework.security:spring-security-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    plugins.withId("org.springframework.boot") {
        extensions.configure<org.springframework.boot.gradle.dsl.SpringBootExtension> {
            val mainClassName = when (project.name) {
                "api-gateway" -> "com.leaky.tokens.apigateway.ApiGatewayApplication"
                "auth-server" -> "com.leaky.tokens.authserver.AuthServerApplication"
                "config-server" -> "com.leaky.tokens.configserver.ConfigServerApplication"
                "service-discovery" -> "com.leaky.tokens.servicediscovery.ServiceDiscoveryApplication"
                "token-service" -> "com.leaky.tokens.tokenservice.TokenServiceApplication"
                "analytics-service" -> "com.leaky.tokens.analyticsservice.AnalyticsServiceApplication"
                else -> null
            }
            if (mainClassName != null) {
                mainClass.set(mainClassName)
            }
        }
    }

//    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//        kotlinOptions {
//            freeCompilerArgs = listOf("-Xjsr305=strict")
//            jvmTarget = "24"
//        }
//    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }

    tasks.withType<JavaExec>().configureEach {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}

tasks.register("smokeReadiness") {
    group = "verification"
    description = "Ping service readiness endpoints before running performance tests"
    dependsOn(":performance-tests:waitForReadiness")
}
