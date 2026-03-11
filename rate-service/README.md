# 📡 Rate Service

High-performance gRPC microservice for fetching real-time currency exchange rates from third-party providers.

## 📋 Overview

The Rate Service is responsible for the "Rate" domain of the ecosystem. It abstracts the complexity of third-party exchange rate APIs and provides a strictly-contracted gRPC interface for other services.

### Key Features

- 📡 **gRPC Server** - Low-latency binary communication (Port 9092).
- 🔄 **Reactive Logic** - Uses Spring WebFlux and Project Reactor for non-blocking API calls.
- 🧪 **High Test Coverage** - Strictly maintained at 85%+ instruction coverage.
- 🛡️ **Robust Error Mapping** - Transcodes internal exceptions into standard gRPC status codes.
- 📈 **Observability** - Integrated with Prometheus and OpenTelemetry.

## 🏗️ Architecture

```text
┌──────────────┐      (gRPC)      ┌──────────────┐
│ Main Service │─────────────────▶│ Rate Service │
└──────────────┘                  └──────┬───────┘
                                         │
                                         ▼
                               ┌─────────────────┐
                               │ Third-Party API │
                               │ (Exchangerates) │
                               └─────────────────┘
```

### gRPC Interface

| Method    | Request       | Response       | Description                               |
|-----------|---------------|----------------|-------------------------------------------|
| `GetRate` | `RateRequest` | `RateResponse` | Fetches exchange rate for a currency pair |

## 📡 API Endpoints (Legacy REST)

The service still maintains a REST interface for backward compatibility or direct health checks.

`GET /rate?from=USD&to=EUR`

## ⚙️ Configuration

### gRPC Settings

```properties
# gRPC Server Configuration
grpc.server.port=9092
```

## 🧪 Testing

The service adheres to strict quality standards with comprehensive unit and integration tests.

`mvn clean verify -pl rate-service`

**Coverage Requirements:**

- Minimum: 85% instruction coverage (JaCoCo).
