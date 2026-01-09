# **Spring-Boot-Currency-Converter**

**A Spring Boot microservice demo for real-time currency conversion**
This project demonstrates a distributed system with two Spring Boot services:

1. **`auth-service`**: Reactive JWT authentication service with Spring WebFlux (NEW ✨)
2. **`rate-service`**: Fetches real-time exchange rates from third-party APIs
3. **`main-service`**: Handles currency conversion logic and transaction persistence
4. **`config-server`**: Centralized configuration management with encryption

Key features include:  
✔ **Reactive authentication** (WebFlux + JWT)
✔ **Service-to-service communication** (HTTP/REST)
✔ **Event-driven architecture** (Kafka for auth events)
✔ **Third-party API integration** (ExchangeRate-API)
✔ **Database persistence** (PostgreSQL for transactions, Redis for tokens)
✔ **Centralized configuration** (Spring Cloud Config)
✔ **Observability** (Prometheus metrics, distributed tracing)
✔ **Global exception handling** with standardized error responses
✔ **Containerized deployment** (Docker support)

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
