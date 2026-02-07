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

public class AuthLoginSimulation extends Simulation {
    private final String baseUrl = System.getProperty("authBaseUrl", "http://localhost:8081");
    private final int users = Integer.getInteger("users", 50);
    private final int rampSeconds = Integer.getInteger("rampSeconds", 10);
    private final int durationSeconds = Integer.getInteger("durationSeconds", 30);
    private final String username = System.getProperty("username", "user1");
    private final String password = System.getProperty("password", "password");

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(baseUrl)
        .contentTypeHeader("application/json");

    private final io.gatling.javaapi.core.Body payload = StringBody(
        "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"
    );

    private final ScenarioBuilder scn = scenario("Auth Login Baseline")
        .during(Duration.ofSeconds(durationSeconds)).on(
            exec(
                http("login")
                    .post("/api/v1/auth/login")
                    .body(payload)
                    .check(status().in(200, 401))
            )
        );

    {
        setUp(
            scn.injectOpen(rampUsers(users).during(Duration.ofSeconds(rampSeconds)))
        ).protocols(httpProtocol);
    }
}
