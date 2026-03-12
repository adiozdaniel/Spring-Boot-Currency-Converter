# **Spring-Boot-Currency-Converter**

**A Spring Boot microservice demo for real-time currency conversion**
This project demonstrates a distributed system with two Spring Boot services:

1. **`auth-service`**: Reactive JWT authentication & gRPC server for token validation (Port: 8081 / gRPC: 9091)
2. **`rate-service`**: Fetches real-time exchange rates via gRPC server (Port: 8082 / gRPC: 9092)
3. **`main-service`**: Edge API Gateway / Transcoder. Translates REST to gRPC (Port: 8080)
4. **`common-config`**: Centralized gRPC contracts (.proto) and shared library
5. **`config-server`**: Centralized configuration management with encryption (Port: 8888)

Key features include:  
✔ **Reactive authentication** (WebFlux + JWT)
✔ **Inter-service communication** (High-performance gRPC)
✔ **Centralized Contracts** (Versioned .proto files in `common-config`)
✔ **Zero-Trust Security** (Asynchronous gRPC Auth Interceptors)
✔ **Event-driven architecture** (Kafka for auth events)
✔ **Third-party API integration** (ExchangeRate-API)
✔ **Resilience Patterns** (Circuit breakers, Retries via Resilience4j)
✔ **Database persistence** (PostgreSQL for transactions, Redis for tokens)
✔ **Centralized configuration** (Spring Cloud Config)
✔ **Observability** (Prometheus metrics, Distributed Tracing)
✔ **Global exception handling** with standardized gRPC error mapping
✔ **Containerized deployment** (Docker & Kubernetes support)

---

## 🐳 Docker Deployment

```bash
# Build all services

docker-compose build

# Start all services

docker-compose up -d

# View logs

docker-compose logs -f

# Stop all services

docker-compose down
```

---

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
