package com.leaky.tokens.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class TokenPurchaseSagaSimulation extends Simulation {
  private val baseUrl = System.getProperty("baseUrl", "http://localhost:8082")
  private val users = Integer.getInteger("users", 20).toInt
  private val rampSeconds = Integer.getInteger("rampSeconds", 10).toInt
  private val durationSeconds = Integer.getInteger("durationSeconds", 30).toInt

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .contentTypeHeader("application/json")

  private val payload =
    StringBody("""{"userId":"00000000-0000-0000-0000-000000000001","provider":"openai","tokens":100}""")

  private val scn = scenario("Token Purchase Saga Baseline")
    .during(durationSeconds) {
      exec(
        http("purchase")
          .post("/api/v1/tokens/purchase")
          .body(payload)
          .check(status.in(202, 400, 401, 403))
      )
    }

  setUp(
    scn.inject(rampUsers(users).during(rampSeconds))
  ).protocols(httpProtocol)
}
