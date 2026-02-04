package com.leaky.tokens.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class TokenQuotaCheckSimulation extends Simulation {
  private val baseUrl = System.getProperty("baseUrl", "http://localhost:8082")
  private val users = Integer.getInteger("users", 50).toInt
  private val rampSeconds = Integer.getInteger("rampSeconds", 10).toInt
  private val durationSeconds = Integer.getInteger("durationSeconds", 30).toInt
  private val userId = System.getProperty("userId", "00000000-0000-0000-0000-000000000001")
  private val provider = System.getProperty("provider", "openai")

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .contentTypeHeader("application/json")

  private val scn = scenario("Token Quota Check Baseline")
    .during(durationSeconds) {
      exec(
        http("quota")
          .get(s"/api/v1/tokens/quota?userId=$userId&provider=$provider")
          .check(status.in(200, 404, 401, 403))
      )
    }

  setUp(
    scn.inject(rampUsers(users).during(rampSeconds))
  ).protocols(httpProtocol)
}
