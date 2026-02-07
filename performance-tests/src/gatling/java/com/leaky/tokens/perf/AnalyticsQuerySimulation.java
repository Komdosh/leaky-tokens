package com.leaky.tokens.perf;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.during;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class AnalyticsQuerySimulation extends Simulation {
    private final String baseUrl = System.getProperty("analyticsBaseUrl", "http://localhost:8083");
    private final int users = Integer.getInteger("users", 50);
    private final int rampSeconds = Integer.getInteger("rampSeconds", 10);
    private final int durationSeconds = Integer.getInteger("durationSeconds", 30);
    private final String provider = System.getProperty("provider", "openai");
    private final String limit = System.getProperty("limit", "10");
    private final String bearerToken = System.getProperty("bearerToken", "");
    private final String apiKey = System.getProperty("apiKey", "test-key");

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(baseUrl)
        .contentTypeHeader("application/json")
        .header("Authorization", bearerToken.isBlank() ? "Bearer dummy" : "Bearer " + bearerToken)
        .header("X-Api-Key", apiKey);

    private final ScenarioBuilder scn = scenario("Analytics Query Baseline")
        .during(Duration.ofSeconds(durationSeconds)).on(
            exec(
                http("query")
                    .get("/api/v1/analytics/usage?provider=" + provider + "&limit=" + limit)
                    .check(status().in(200, 401, 403))
            )
        );

    {
        setUp(
            scn.injectOpen(rampUsers(users).during(Duration.ofSeconds(rampSeconds)))
        ).protocols(httpProtocol);
    }
}
