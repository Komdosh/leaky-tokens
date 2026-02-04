package com.leaky.tokens.perf

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class AuthLoginSimulation extends Simulation {
  private val baseUrl = System.getProperty("authBaseUrl", "http://localhost:8081")
  private val users = Integer.getInteger("users", 50).toInt
  private val rampSeconds = Integer.getInteger("rampSeconds", 10).toInt
  private val durationSeconds = Integer.getInteger("durationSeconds", 30).toInt
  private val username = System.getProperty("username", "user1")
  private val password = System.getProperty("password", "password")

  private val httpProtocol = http
    .baseUrl(baseUrl)
    .contentTypeHeader("application/json")

  private val payload =
    StringBody(s"""{"username":"$username","password":"$password"}""")

  private val scn = scenario("Auth Login Baseline")
    .during(durationSeconds) {
      exec(
        http("login")
          .post("/api/v1/auth/login")
          .body(payload)
          .check(status.in(200, 401))
      )
    }

  setUp(
    scn.inject(rampUsers(users).during(rampSeconds))
  ).protocols(httpProtocol)
}
