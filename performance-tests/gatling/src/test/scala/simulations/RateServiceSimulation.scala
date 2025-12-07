package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class RateServiceSimulation extends Simulation {

  // Configuration
  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val users = Integer.getInteger("users", 100).toInt
  val rampDuration = Integer.getInteger("rampDuration", 60).toInt
  val testDuration = Integer.getInteger("testDuration", 300).toInt

  // HTTP Protocol Configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .userAgentHeader("Gatling Performance Test")

  // Common currencies to test
  val currencies = List("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY")

  // Feeder for random currency selection
  val currencyFeeder = Iterator.continually(Map("currency" -> currencies(scala.util.Random.nextInt(currencies.length))))

  // Scenarios
  val exchangeRateScenario = scenario("Exchange Rate Lookup")
    .during(testDuration.seconds) {
      feed(currencyFeeder)
        .exec(
          http("GET /api/v1/rates/${currency}")
            .get("/api/v1/rates/${currency}")
            .check(status.is(200))
            .check(jsonPath("$.baseCurrency").exists)
            .check(jsonPath("$.rates").exists)
        )
        .pause(500.milliseconds)
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

  val spikeScenario = scenario("Spike Load")
    .pause(60.seconds) // Wait before spike
    .during(30.seconds) {
      feed(currencyFeeder)
        .exec(
          http("GET /api/v1/rates/${currency} - Spike")
            .get("/api/v1/rates/${currency}")
            .check(status.is(200))
        )
        .pause(100.milliseconds)
    }

  // Load Simulation
  setUp(
    exchangeRateScenario.inject(
      rampUsers(users).during(rampDuration.seconds)
    ),
    healthCheckScenario.inject(
      rampUsers(10).during(rampDuration.seconds)
    ),
    spikeScenario.inject(
      nothingFor(60.seconds),
      atOnceUsers(50)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(1000),
      global.responseTime.percentile3.lt(500),
      global.responseTime.percentile2.lt(300),
      global.successfulRequests.percent.gt(99.5)
    )
}
