package com.leaky.tokens.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class TokenConsumeSimulation extends Simulation {
  private val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  private val apiKey = System.getProperty("apiKey", "test-key")
  private val users = Integer.getInteger("users", 50).toInt
  private val rampSeconds = Integer.getInteger("rampSeconds", 10).toInt
  private val durationSeconds = Integer.getInteger("durationSeconds", 30).toInt

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .contentTypeHeader("application/json")
    .header("X-Api-Key", apiKey)

  private val payload = StringBody("""{"userId":"00000000-0000-0000-0000-000000000001","provider":"openai","tokens":10}""")

  private val scn = scenario("Token Consume Baseline")
    .during(durationSeconds) {
      exec(
        http("consume")
          .post("/api/v1/tokens/consume")
          .body(payload)
          .check(status.in(200, 429, 402))
      )
    }

  setUp(
    scn.inject(rampUsers(users).during(rampSeconds))
  ).protocols(httpProtocol)
}
