# **Spring-Boot-Currency-Converter**  
**A Spring Boot microservice demo for real-time currency conversion**  

This project demonstrates a distributed system with two Spring Boot services:  
1. **`rate-service`**: Fetches real-time exchange rates from a third-party API.  
2. **`main-service`**: Handles currency conversion logic, stores transactions in PostgreSQL, and communicates with `rate-service`.  

Key features include:  
✔ **Service-to-service communication** (HTTP/REST)  
✔ **Third-party API integration** (e.g., ExchangeRate-API)  
✔ **Database persistence** (PostgreSQL for audit logging)  
✔ **Global exception handling** with standardized error responses  
✔ **Containerized deployment** (Docker support)  

---

### 🩺 Health Check

**Run main service:**

```bash
cd main-service
./mvnw spring-boot:run
```

**Run rate service:**

```bash
cd rate-service
./mvnw spring-boot:run
```

**Test service status:**

```bash
# Rate Service (port 8080)
curl http://localhost:8080/status

# Main Service (port 8000)
curl http://localhost:8000/status
```

Expected:

```json
{ "status": "UP" }
```
---
