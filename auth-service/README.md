# 🔐 Authentication Service

Reactive JWT authentication microservice built with Spring WebFlux for high-performance token management and API security.

## 📋 Overview

The Authentication Service provides secure, scalable JWT-based authentication for the Currency Converter platform. Built using
reactive programming paradigms, it handles token generation, validation, refresh, and revocation with non-blocking I/O for
optimal throughput.

### Key Features

- 🔄 **Fully Reactive** - Spring WebFlux with Project Reactor (Mono/Flux)
- 📡 **gRPC Server** - High-performance server for inter-service authentication (Port 9091)
- 🔒 **JWT Token Management** - Access & refresh tokens with configurable expiration
- 🛡️ **API Key Validation** - Client authentication via API keys
- ⚡ **Rate Limiting** - Bucket4j + Resilience4j for request throttling
- 📊 **Kafka Event Streaming** - Reactive auth event publishing (reactor-kafka)
- 🗄️ **Redis Integration** - Token revocation with reactive Redis
- 📈 **Observability** - Prometheus metrics, distributed tracing, gRPC interceptors
- 🔐 **Spring Security WebFlux** - Reactive security filter chains
- 🧪 **Comprehensive Tests** - WebFluxTest, gRPC Unit Tests, 85%+ coverage

## 🏗️ Architecture

┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Client    │────▶│ Auth Service │────▶│    Redis    │
│  (API Key)  │     │ (REST/gRPC)  │     │  (Revoked)  │
└─────────────┘     └──────────────┘     └─────────────┘
                           │
           ┌───────────────┴───────────────┐
           ▼                               │
    Config Server                  Kafka (Auth Events)
    (JWT Secret)

### gRPC Interface

The service exposes a gRPC interface for low-latency token validation used by other microservices.

| Method          | Request           | Response           | Description                      |
|-----------------|-------------------|--------------------|----------------------------------|
| `Authenticate`  | `AuthGrpcRequest` | `AuthGrpcResponse` | Internal service authentication  |
| `ValidateToken` | `ValidateRequest` | `ValidateResponse` | Real-time JWT claims validation  |

### Tech Stack

| Component     | Technology                 | Version        |
| ------------- | -------------------------- | -------------- |
| Framework     | Spring Boot                | 3.5.9          |
| Reactive      | Spring WebFlux             | 3.5.9          |
| Security      | Spring Security WebFlux    | 3.5.9          |
| JWT           | JJWT                       | 0.12.7         |
| Messaging     | Reactor Kafka              | 1.3.20         |
| Cache         | Spring Data Redis Reactive | 3.5.9          |
| Rate Limiting | Bucket4j + Resilience4j    | 8.10.1 / 2.2.0 |
| Metrics       | Micrometer Prometheus      | Latest         |
| Documentation | SpringDoc OpenAPI          | 2.8.15         |
| Testing       | Reactor Test               | Latest         |

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- Redis 7.0+ (for token revocation)
- Kafka 3.x (optional, for events)
- Config Server (for centralized configuration)

### Environment Variables

| Variable                  | Description                  | Default             |
| ------------------------- | ---------------------------- | ------------------- |
| `AUTH_SERVICE_PORT`       | Service port                 | `8081`              |
| `ENCRYPT_KEY`             | Config server encryption key | Required            |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses       | Required if enabled |
| `KAFKA_SECURITY_PROTOCOL` | Kafka security protocol      | `PLAINTEXT`         |
| `SPRING_REDIS_HOST`       | Redis host                   | `localhost`         |
| `SPRING_REDIS_PORT`       | Redis port                   | `6379`              |

### Build & Run

```bash
# Build the project
mvn clean package

# Run with Maven
mvn spring-boot:run
```

```bash
# Run standalone JAR
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

#### Docker

```bash
# Build image
docker build -t auth-service:latest .

# Run container
docker run -p 8081:8081 \
  -e SPRING_REDIS_HOST=redis \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  auth-service:latest
```

## 📡 API Endpoints

### Authentication

`POST /v1/auth/token`

Authenticate with API key and receive JWT tokens.

```json
Request:
{
  "apiKey": "your-api-key",
  "clientId": "client-identifier",
  "clientType": "web"
}

Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

`POST /v1/auth/refresh`

Refresh access token using refresh token.

```json
Request:
{
  "refreshToken": "eyJhbGc..."
}
```

`POST /v1/auth/revoke`

Revoke a token (stored in Redis).

```json
Request:
{
  "token": "eyJhbGc..."
}
```

`POST /v1/auth/validate`

Validate access token and retrieve claims.

```json
Headers:
Authorization: Bearer eyJhbGc...

Response:
{
  "valid": true,
  "clientId": "client-identifier",
  "clientType": "web",
  "expiresAt": 1234567890
}
```

## ⚙️ Configuration

### JWT Configuration

#### JWT Settings (from Config Server)

```yml
jwt.secret={cipher}ENCRYPTED_SECRET
jwt.expiration=3600000          # 1 hour (milliseconds)
jwt.refresh-expiration=86400000 # 24 hours
```

### API Keys Configuration

#### API Keys (from Config Server)

```yml
api-keys.web={cipher}ENCRYPTED_WEB_KEY
api-keys.mobile={cipher}ENCRYPTED_MOBILE_KEY
api-keys.platform={cipher}ENCRYPTED_PLATFORM_KEY
```

### Rate Limiting

```yml
rate-limit.capacity=100
rate-limit.refill-tokens=100
rate-limit.refill-duration-seconds=60
```

### Kafka Topics

```yml
kafka.topics.auth-login-success.name=auth.login.success
kafka.topics.auth-login-failed.name=auth.login.failed
kafka.topics.auth-tokens.name=auth.tokens
kafka.topics.auth-dlq.name=auth.dlq
```

## 📊 Monitoring

### Actuator Endpoints

- Health: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

### Custom Metrics

| Metric                              | Type    | Description                 |
| ----------------------------------- | ------- | --------------------------- |
| `auth.attempts{result=success}`     | Counter | Successful authentications  |
| `auth.attempts{result=failure}`     | Counter | Failed authentications      |
| `auth.events.published`             | Counter | Kafka events published      |
| `auth.events.failed`                | Counter | Kafka publish failures      |
| `auth.events.publish.login.latency` | Timer   | Login event publish latency |

## OpenAPI Documentation

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

## 🧪 Testing

### Run All Tests

`mvn test`

### Run with Coverage

#### Coverage reports

`target/site/jacoco/index.html`

### Test Structure

```yaml
src/test/java/
├── controller/
│   ├── AuthControllerTest.java      # WebFlux controller tests
│   └── StatusControllerTest.java
├── service/
│   ├── AuthenticationServiceTest.java
│   ├── TokenServiceTest.java        # Reactive token tests
│   ├── ApiKeyValidatorTest.java
│   ├── RateLimiterServiceTest.java
│   └── AuthEventProducerTest.java
└── utility/
    └── HttpUtilitiesTest.java
```

## 🔒 Security

### JWT Secret Management

The JWT secret is encrypted at rest using Spring Cloud Config's {cipher} encryption and stored in the Config Server. The
encryption key is provided via the ENCRYPT_KEY environment variable.

**Note:** Never commit unencrypted secrets to version control.

### Token Revocation

Revoked tokens are stored in Redis with TTL matching the token expiration. The service checks Redis on every token validation.

### Rate Limiting - Security

Rate limiting is applied per client IP address:

- Default: 100 requests per minute
- Configurable via rate-limit.\* properties

### CORS - Security

CORS is configured per environment:

- Dev: Allows `localhost:3000`, `localhost:8080`
- Prod: Strict allowlist of production domains

## 🚀 Performance

### Reactive Advantages

- **Non-blocking I/O**: All database, cache, and messaging operations are reactive
- **Thread efficiency**: Event loop model vs thread-per-request
- **Backpressure**: Built-in flow control prevents resource exhaustion
- **Scalability**: Handles high concurrency with fewer resources

### Benchmarks

| Metric               | Blocking (Pre-migration) | Reactive (Current) | Improvement   |
| -------------------- | ------------------------ | ------------------ | ------------- |
| Concurrent Users     | 500                      | 2000+              | 4x            |
| Avg Response Time    | 45ms                     | 12ms               | 73% faster    |
| Memory per Request   | 512KB                    | 64KB               | 87% reduction |
| Throughput (req/sec) | 1200                     | 5000+              | 4x            |

**Note**: Benchmarks measured with JMeter under production-like load

## 🐛 Troubleshooting

### Common Issues

#### Redis Connection Failures

##### Check Redis connectivity

`redis-cli -h $SPRING_REDIS_HOST ping`

#### JWT Secret Not Decrypted

##### Verify Config Server encryption is working

`curl http://config-server:8888/encrypt -d "test"`

#### Kafka Events Not Publishing

##### Check Kafka topic exists

`kafka-topics.sh --list --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS`

## 📝 Development

### Code Quality

#### Run SonarQube analysis

Start SonarQube with Docker

```bash
docker run -d --name sonarqube \
 -p 9000:9000 \
 sonarqube:lts-community

# Wait 2-3 minutes for it to start, then access: http://localhost:9000

# Default credentials: admin / admin (you'll be prompted to change)
```

```bash

# After running
mvn clean verify

# Scan with Maven

mvn sonar:sonar \
 -Dsonar.host.url=http://localhost:9000 \
 -Dsonar.token=TOKEN_HERE \
 -Dsonar.projectKey=auth-service
```

#### Dependency Vulnerability Checks

Snyk is used for vulnerability scanning of both dependencies and Docker images.

**If not installed** `npm install -g snyk`

**Authenticate** `snyk auth`

**Monitor continuously** `snyk monitor`

Or

```bash
# Scan Maven dependencies
snyk test --all-projects

# Scan with severity threshold
snyk test --severity-threshold=high

# Scan Docker image
docker build -t auth-service:latest .
snyk container test auth-service:latest

# Monitor for continuous vulnerability tracking
snyk monitor
```

**CI/CD Integration**: `.github/workflows/security.yml`

```yml
- name: Run Snyk to check for vulnerabilities
  uses: snyk/actions/maven@master
  env:
    SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
  with:
    args: --severity-threshold=high --all-projects
```

#### Code Coverage Requirements

- Minimum: 80% instruction coverage
- Exclusions: Config, DTOs, entities, events, exceptions, filters

## 🤝 Contributing

1. Create feature branch from master
2. Implement changes with tests (80%+ coverage)
3. Run full test suite: `mvn clean verify`
4. Submit PR with description

## 📄 License

Copyright © 2025 Currency Converter Platform

---

## 📞 Support

- **📧 Email**: [Chief Backend Engineer](mailto:eshitemi@bigboldred.agency) or [Personal Email](mailto:adiozdaniel@gmail.com)

---

**Service Version:** 0.0.1-SNAPSHOT
**Spring Boot:** 3.5.9
**Java:** 21
**Reactive:** ✅ WebFlux + Project Reactor
