# 🚀 Main Service (API Gateway & Transcoder)

The edge service of the Currency Converter Ecosystem. It acts as a high-performance proxy that translates external REST/HTTP requests into optimized gRPC calls for internal microservices.

## 📋 Overview

The Main Service is the primary entry point for users. It handles the orchestration of currency conversion by communicating with the `rate-service` and ensuring user authorization via the `auth-service`.

### Key Features

- 🔗 **REST-to-gRPC Transcoding** - Seamlessly bridges the external REST API with internal gRPC infrastructure.
- 🛡️ **Zero-Trust Auth Interceptor** - Implements `GrpcAuthFilter` for real-time, asynchronous JWT validation via gRPC.
- ⚡ **Reactive Architecture** - Built on Spring WebFlux for maximum concurrency and non-blocking I/O.
- 📉 **Resilience4j Integration** - Built-in Circuit Breakers and Retries for all outgoing gRPC channels.
- 📊 **Transactional Persistence** - Records all successful conversions in PostgreSQL using R2DBC.
- 📈 **Full Observability** - Distributed tracing and metrics for both REST and gRPC layers.

## 🏗️ Architecture

```text
      (External)            (Internal - gRPC)
┌──────────────┐       ┌──────────────┐
│    Browser   │       │ Auth Service │
│    / Mobile  │──┐    │  (Port 9091) │
└──────┬───────┘  │    └──────▲───────┘
       │          │           │ (ValidateToken)
       │ (REST)   │    ┌──────┴───────┐
       ▼          └────┤ Main Service  │
┌──────────────┐       │ (Port 8080)  │
│  API Gateway │──────▶└──────┬───────┘
└──────────────┘              │ (GetRate)
                              ▼
                       ┌──────────────┐
                       │ Rate Service │
                       │  (Port 9092) │
                       └──────────────┘
```

## 📡 API Endpoints

### Currency Conversion

`POST /api/v1/convert`

Converts an amount from one currency to another.

**Request:**

```json
{
  "from": "USD",
  "to": "EUR",
  "amount": 100.0
}
```

**Workflow:**

1. `GrpcAuthFilter` extracts JWT and calls `AuthService.ValidateToken`.
2. `ConvertController` receives REST request.
3. `RateGrpcClient` calls `RateService.GetRate`.
4. Result is saved to DB and returned to user.

## ⚙️ Configuration

### gRPC Client Settings

```properties
# gRPC Client Configuration
grpc.client.auth-service.address=static://auth-service:9091
grpc.client.rate-service.address=static://rate-service:9092

# Connection Tuning
grpc.client.rate-service.enable-keep-alive=true
grpc.client.rate-service.keep-alive-timeout=20s
```

### Resilience4j

```properties
resilience4j.circuitbreaker.instances.rate-service.failure-rate-threshold=50
resilience4j.retry.instances.rate-service.max-attempts=3
```

## 🧪 Testing

The service includes comprehensive tests for the transcoding logic and gRPC client resilience.

`mvn test -pl main-service`
