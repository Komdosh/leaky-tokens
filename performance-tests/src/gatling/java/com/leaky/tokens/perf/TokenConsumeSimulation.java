package com.leaky.tokens.perf;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.during;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class TokenConsumeSimulation extends Simulation {
    private final String baseUrl = System.getProperty("baseUrl", "http://localhost:8080");
    private final String apiKey = System.getProperty("apiKey", "test-key");
    private final int users = Integer.getInteger("users", 50);
    private final int rampSeconds = Integer.getInteger("rampSeconds", 10);
    private final int durationSeconds = Integer.getInteger("durationSeconds", 30);

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(baseUrl)
        .contentTypeHeader("application/json")
        .header("X-Api-Key", apiKey);

    private final io.gatling.javaapi.core.Body payload = StringBody(
        "{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":10}"
    );

    private final ScenarioBuilder scn = scenario("Token Consume Baseline")
        .during(Duration.ofSeconds(durationSeconds)).on(
            exec(
                http("consume")
                    .post("/api/v1/tokens/consume")
                    .body(payload)
                    .check(status().in(200, 429, 402))
            )
        );

    {
        setUp(
            scn.injectOpen(rampUsers(users).during(Duration.ofSeconds(rampSeconds)))
        ).protocols(httpProtocol);
    }
}
