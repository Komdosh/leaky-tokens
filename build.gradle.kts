import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

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
    apply(plugin = "jacoco")
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
        implementation("net.logstash.logback:logstash-logback-encoder:7.4")
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

    val isPerformanceTests = name == "performance-tests"

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

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        if (!isPerformanceTests) {
            finalizedBy("jacocoTestReport")
        }
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = "0.8.14"
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        enabled = !isPerformanceTests
        dependsOn(tasks.test)
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude("com/leaky/tokens/perf/**")
                }
            })
        )
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        enabled = !isPerformanceTests
        dependsOn(tasks.test)
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude("com/leaky/tokens/perf/**")
                }
            })
        )
        violationRules {
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.80".toBigDecimal()
                }
            }
        }
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
