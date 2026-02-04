plugins {
    id("io.gatling.gradle") version "3.11.5"
    scala
}

repositories {
    mavenCentral()
}

// This module is not a Spring Boot app; disable boot jar tasks if inherited.
tasks.matching { it.name == "bootJar" || it.name == "bootRun" }.configureEach {
    enabled = false
}

// Gradle 9 removed project.reportsDir; Gatling plugin still queries it.
extra["reportsDir"] = layout.buildDirectory.dir("reports").get().asFile

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    gatlingImplementation("io.gatling:gatling-core")
    gatlingImplementation("io.gatling:gatling-http")
}

tasks.withType<org.gradle.api.tasks.scala.ScalaCompile>().configureEach {
    scalaCompileOptions.additionalParameters = listOf("-target:jvm-17")
}

gatling {
    jvmArgs = listOf("-Xms512m", "-Xmx1024m")
}

val gatlingReportDir = layout.buildDirectory.dir("reports/gatling")
val gatlingJvmArgs = listOf(
    "-Xms512m",
    "-Xmx1024m",
    "--enable-native-access=ALL-UNNAMED"
)
val gatlingSystemProperties = System.getProperties().entries.associate { (key, value) ->
    key.toString() to value
}
val gatlingJavaLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}

fun registerGatlingRunTask(taskName: String, simulationClass: String) =
    tasks.register<JavaExec>(taskName) {
        group = "gatling"
        description = "Run Gatling simulation $simulationClass"
        dependsOn("gatlingClasses")
        classpath = configurations["gatlingRuntimeClasspath"]
        mainClass.set("io.gatling.app.Gatling")
        javaLauncher.set(gatlingJavaLauncher)
        jvmArgs = gatlingJvmArgs
        args(
            "-s", simulationClass,
            "-rf", gatlingReportDir.get().asFile.absolutePath
        )
        systemProperties(gatlingSystemProperties)
    }

val runAnalytics = registerGatlingRunTask(
    "runGatlingAnalytics",
    "com.leaky.tokens.perf.AnalyticsQuerySimulation"
)
val runAuth = registerGatlingRunTask(
    "runGatlingAuth",
    "com.leaky.tokens.perf.AuthLoginSimulation"
)
val runConsume = registerGatlingRunTask(
    "runGatlingConsume",
    "com.leaky.tokens.perf.TokenConsumeSimulation"
)
val runPurchase = registerGatlingRunTask(
    "runGatlingPurchaseSaga",
    "com.leaky.tokens.perf.TokenPurchaseSagaSimulation"
)
val runQuota = registerGatlingRunTask(
    "runGatlingQuota",
    "com.leaky.tokens.perf.TokenQuotaCheckSimulation"
)
val runUsage = registerGatlingRunTask(
    "runGatlingUsage",
    "com.leaky.tokens.perf.TokenUsagePublishSimulation"
)

fun waitForUrl(url: String, timeoutSeconds: Long, intervalMillis: Long) {
    val client = java.net.http.HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(5))
        .build()
    val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(timeoutSeconds)
    var lastError: String? = null
    while (System.nanoTime() < deadline) {
        try {
            val request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build()
            val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() in 200..299) {
                return
            }
            lastError = "status=${response.statusCode()}"
        } catch (ex: Exception) {
            lastError = ex.javaClass.simpleName + (ex.message?.let { ": $it" } ?: "")
        }
        Thread.sleep(intervalMillis)
    }
    throw GradleException("Timed out waiting for $url (${lastError ?: "unknown error"})")
}

val readinessUrls = listOf(
    System.getProperty("perf.readiness.auth", "http://localhost:8081/actuator/health/readiness"),
    System.getProperty("perf.readiness.token", "http://localhost:8082/actuator/health/readiness"),
    System.getProperty("perf.readiness.analytics", "http://localhost:8083/actuator/health/readiness"),
    System.getProperty("perf.readiness.gateway", "http://localhost:8080/actuator/health/readiness")
)

tasks.register("waitForReadiness") {
    group = "gatling"
    description = "Wait for dependent services to be ready before running Gatling"
    doLast {
        val timeoutSeconds = System.getProperty("perf.readiness.timeoutSeconds", "120").toLong()
        val intervalMillis = System.getProperty("perf.readiness.intervalMillis", "1000").toLong()
        readinessUrls.forEach { url ->
            logger.lifecycle("Waiting for readiness: $url")
            waitForUrl(url, timeoutSeconds, intervalMillis)
        }
    }
}

tasks.register("runGatlingAll") {
    group = "gatling"
    description = "Run all Gatling simulations sequentially"
    dependsOn("waitForReadiness", runAnalytics, runAuth, runConsume, runPurchase, runQuota, runUsage)
}
