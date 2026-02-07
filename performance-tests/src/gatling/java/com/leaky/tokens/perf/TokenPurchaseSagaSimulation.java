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

public class TokenPurchaseSagaSimulation extends Simulation {
    private final String baseUrl = System.getProperty("baseUrl", "http://localhost:8082");
    private final int users = Integer.getInteger("users", 20);
    private final int rampSeconds = Integer.getInteger("rampSeconds", 10);
    private final int durationSeconds = Integer.getInteger("durationSeconds", 30);

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(baseUrl)
        .contentTypeHeader("application/json");

    private final io.gatling.javaapi.core.Body payload = StringBody(
        "{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":100}"
    );

    private final ScenarioBuilder scn = scenario("Token Purchase Saga Baseline")
        .during(Duration.ofSeconds(durationSeconds)).on(
            exec(
                http("purchase")
                    .post("/api/v1/tokens/purchase")
                    .body(payload)
                    .check(status().in(202, 400, 401, 403))
            )
        );

    {
        setUp(
            scn.injectOpen(rampUsers(users).during(Duration.ofSeconds(rampSeconds)))
        ).protocols(httpProtocol);
    }
}
