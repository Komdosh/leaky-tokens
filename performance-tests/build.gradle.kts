import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

plugins {
    id("io.gatling.gradle") version "3.11.5"
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
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    gatlingImplementation("io.gatling:gatling-core")
    gatlingImplementation("io.gatling:gatling-http")
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
    val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
    var lastError: String? = null
    while (System.nanoTime() < deadline) {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.discarding())
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

fun extractInt(json: String, key: String): Int? {
    val match = Regex("\"$key\"\\s*:\\s*(\\d+)").find(json)
    return match?.groupValues?.get(1)?.toIntOrNull()
}

fun extractString(json: String, key: String): String? {
    val match = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"").find(json)
    return match?.groupValues?.get(1)
}

fun extractIntByRegex(json: String, pattern: String): Int? {
    val match = Regex(pattern).find(json)
    return match?.groupValues?.get(1)?.toIntOrNull()
}

data class GatlingSummary(
    val simulation: String,
    val total: Int,
    val ok: Int,
    val ko: Int,
    val min: Int?,
    val max: Int?,
    val p50: Int?,
    val p75: Int?,
    val p95: Int?,
    val p99: Int?
)

fun parseSummary(statsJson: String, fallbackName: String): GatlingSummary? {
    val simulationName = extractString(statsJson, "name") ?: fallbackName
    val total = extractIntByRegex(statsJson, "\"numberOfRequests\"\\s*:\\s*\\{[^}]*\"total\"\\s*:\\s*(\\d+)") ?: return null
    val ok = extractIntByRegex(statsJson, "\"numberOfRequests\"\\s*:\\s*\\{[^}]*\"ok\"\\s*:\\s*(\\d+)") ?: 0
    val ko = extractIntByRegex(statsJson, "\"numberOfRequests\"\\s*:\\s*\\{[^}]*\"ko\"\\s*:\\s*(\\d+)") ?: 0
    val min = extractInt(statsJson, "minResponseTime")
    val max = extractInt(statsJson, "maxResponseTime")
    val p50 = extractInt(statsJson, "percentiles1")
    val p75 = extractInt(statsJson, "percentiles2")
    val p95 = extractInt(statsJson, "percentiles3")
    val p99 = extractInt(statsJson, "percentiles4")
    return GatlingSummary(simulationName, total, ok, ko, min, max, p50, p75, p95, p99)
}

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

tasks.register("summarizeGatlingReports") {
    group = "gatling"
    description = "Summarize latest Gatling report stats into a markdown file"
    doLast {
        val reportsDir = layout.buildDirectory.dir("reports/gatling").get().asFile
        if (!reportsDir.exists()) {
            throw GradleException("No Gatling reports found at ${reportsDir.absolutePath}")
        }
        val statsFiles = reportsDir.walkTopDown()
            .filter { it.isFile && it.name == "stats.json" }
            .toList()
        if (statsFiles.isEmpty()) {
            throw GradleException("No Gatling stats.json files found under ${reportsDir.absolutePath}")
        }

        val latestBySimulation = statsFiles
            .groupBy { file ->
                file.parentFile.name.substringBeforeLast("-")
            }
            .mapValues { (_, files) ->
                files.maxByOrNull { it.lastModified() }
            }
            .values
            .filterNotNull()

        val summaries = latestBySimulation.mapNotNull { file ->
            val json = file.readText()
            parseSummary(json, file.parentFile.name)
        }.sortedBy { it.simulation }

        val summaryDir = reportsDir.resolve("summary")
        summaryDir.mkdirs()
        val timestamp = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss'Z'"))
        val output = summaryDir.resolve("summary-$timestamp.md")

        val lines = mutableListOf<String>()
        lines += "# Gatling Summary"
        lines += ""
        lines += "Generated: $timestamp"
        lines += ""
        lines += "| Simulation | Total | OK | KO | Min (ms) | Max (ms) | P50 | P75 | P95 | P99 |"
        lines += "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"
        summaries.forEach { summary ->
            lines += "| ${summary.simulation} | ${summary.total} | ${summary.ok} | ${summary.ko} | ${summary.min ?: "-"} | ${summary.max ?: "-"} | ${summary.p50 ?: "-"} | ${summary.p75 ?: "-"} | ${summary.p95 ?: "-"} | ${summary.p99 ?: "-"} |"
        }

        output.writeText(lines.joinToString("\n"))
        logger.lifecycle("Wrote Gatling summary: ${output.absolutePath}")
    }
}

tasks.register("runGatlingAll") {
    group = "gatling"
    description = "Run all Gatling simulations sequentially"
    dependsOn("waitForReadiness", runAnalytics, runAuth, runConsume, runPurchase, runQuota, runUsage)
    finalizedBy("summarizeGatlingReports")
}
