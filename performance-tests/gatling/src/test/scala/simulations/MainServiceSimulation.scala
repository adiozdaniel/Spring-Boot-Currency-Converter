package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class MainServiceSimulation extends Simulation {

  // Configuration
  val baseUrl = System.getProperty("baseUrl", "http://localhost:8000")
  val users = Integer.getInteger("users", 50).toInt
  val rampDuration = Integer.getInteger("rampDuration", 30).toInt
  val testDuration = Integer.getInteger("testDuration", 300).toInt

  // HTTP Protocol Configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test")

  // Scenarios
  val conversionScenario = scenario("Currency Conversion")
    .during(testDuration.seconds) {
      exec(
        http("POST /api/v1/convert")
          .post("/api/v1/convert")
          .body(StringBody("""{
            "fromCurrency": "USD",
            "toCurrency": "EUR",
            "amount": 100.00
          }"""))
          .check(status.is(200))
          .check(jsonPath("$.convertedAmount").exists)
      )
      .pause(1.second)
    }

  val historyScenario = scenario("Conversion History")
    .during(testDuration.seconds) {
      exec(
        http("GET /api/v1/conversions")
          .get("/api/v1/conversions")
          .check(status.is(200))
      )
      .pause(2.seconds)
    }

  val healthCheckScenario = scenario("Health Check")
    .during(testDuration.seconds) {
      exec(
        http("GET /actuator/health")
          .get("/actuator/health")
          .check(status.is(200))
          .check(jsonPath("$.status").is("UP"))
      )
      .pause(5.seconds)
    }

  // Load Simulation
  setUp(
    conversionScenario.inject(
      rampUsers(users).during(rampDuration.seconds)
    ),
    historyScenario.inject(
      rampUsers(users / 2).during(rampDuration.seconds)
    ),
    healthCheckScenario.inject(
      rampUsers(10).during(rampDuration.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(2000),
      global.responseTime.percentile3.lt(1000),
      global.responseTime.percentile2.lt(500),
      global.successfulRequests.percent.gt(99)
    )
}
