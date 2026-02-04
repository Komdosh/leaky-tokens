package com.leaky.tokens.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class AnalyticsQuerySimulation extends Simulation {
  private val baseUrl = System.getProperty("analyticsBaseUrl", "http://localhost:8083")
  private val users = Integer.getInteger("users", 50).toInt
  private val rampSeconds = Integer.getInteger("rampSeconds", 10).toInt
  private val durationSeconds = Integer.getInteger("durationSeconds", 30).toInt
  private val provider = System.getProperty("provider", "openai")
  private val limit = System.getProperty("limit", "10")
  private val bearerToken = System.getProperty("bearerToken", "")

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .contentTypeHeader("application/json")
    .header("Authorization", if (bearerToken.isBlank) "Bearer dummy" else s"Bearer $bearerToken")

  private val scn = scenario("Analytics Query Baseline")
    .during(durationSeconds) {
      exec(
        http("query")
          .get(s"/api/v1/analytics/usage?provider=$provider&limit=$limit")
          .check(status.in(200, 401, 403))
      )
    }

  setUp(
    scn.inject(rampUsers(users).during(rampSeconds))
  ).protocols(httpProtocol)
}
