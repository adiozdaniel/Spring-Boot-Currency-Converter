# Configuration Server

Spring Cloud Config Server for centralized configuration management of Currency Converter microservices.

## Overview

The Config Server provides:
- Centralized configuration management
- Environment-specific configurations (dev, staging, prod)
- Encrypted secrets storage
- Configuration versioning via Git
- Runtime configuration refresh
- Multi-service support

## Quick Start

### Local Development

```bash
# Build the config server
cd config-server
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Or run with Docker
docker build -t config-server .
docker run -p 8888:8888 \
  -e ENCRYPT_KEY=your-encryption-key \
  -e CONFIG_REPO_URI=file:///config-repo \
  config-server
```

The server will be available at: http://localhost:8888

### Testing Configuration

```bash
# Test main-service dev configuration
curl http://config-user:changeme@localhost:8888/main-service/dev

# Test rate-service prod configuration
curl http://config-user:changeme@localhost:8888/rate-service/prod
```

## Configuration Repository

The config server reads configuration from a Git repository located at `config-repo/`.

### Repository Structure

```
config-repo/
├── main-service/
│   ├── application.yml           # Common settings
│   ├── application-dev.yml       # Development
│   ├── application-staging.yml   # Staging
│   └── application-prod.yml      # Production
├── rate-service/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-staging.yml
│   └── application-prod.yml
└── auth-service/
    └── (future service configs)
```

### Initialize Config Repository

```bash
cd config-repo
git init
git add .
git commit -m "Initial configuration"

# For production, push to remote repository
git remote add origin https://github.com/your-org/config-repo.git
git push -u origin main
```

## Encryption

### Generate Encryption Key

```bash
# Generate a strong encryption key
openssl rand -base64 32
```

### Configure Encryption

Set the encryption key as an environment variable:

```bash
export ENCRYPT_KEY="your-generated-encryption-key"
```

Or configure in `bootstrap.yml`:

```yaml
encrypt:
  key: ${ENCRYPT_KEY}
```

### Encrypt a Value

```bash
# Start config server, then encrypt
curl -X POST http://config-user:changeme@localhost:8888/encrypt \
  -d "my-secret-password"

# Output: AQBEncryptedValueHere...
```

### Use Encrypted Values

Add to configuration files with `{cipher}` prefix:

```yaml
database:
  password: '{cipher}AQBEncryptedValueHere...'
```

### Decrypt for Testing

```bash
curl -X POST http://config-user:changeme@localhost:8888/decrypt \
  -d "AQBEncryptedValueHere..."
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `CONFIG_SERVER_PORT` | Server port | 8888 |
| `CONFIG_SERVER_USERNAME` | Basic auth username | config-user |
| `CONFIG_SERVER_PASSWORD` | Basic auth password | changeme |
| `ENCRYPT_KEY` | Symmetric encryption key | changeme-default-encryption-key |
| `CONFIG_REPO_URI` | Git repository URI | file://${user.home}/config-repo |
| `CONFIG_REPO_BRANCH` | Git branch | main |
| `CONFIG_REPO_USERNAME` | Git username (if private) | - |
| `CONFIG_REPO_PASSWORD` | Git password/token (if private) | - |
| `SPRING_PROFILES_ACTIVE` | Active profile | development |

## Client Configuration

### Add Dependencies

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

### Configure Client

Create `bootstrap.yml` in client service:

```yaml
spring:
  application:
    name: main-service
  cloud:
    config:
      uri: http://config-server:8888
      username: config-user
      password: changeme
      fail-fast: false
```

### Refresh Configuration

Use `@RefreshScope` on beans that need config refresh:

```java
@RefreshScope
@Configuration
public class ApiConfig {
    @Value("${api.keys.platform}")
    private String platformApiKey;
}
```

Trigger refresh:

```bash
curl -X POST http://localhost:8000/actuator/refresh
```

## Kubernetes Deployment

### Prerequisites

1. Create config repository secret:

```bash
# Generate encryption key
ENCRYPT_KEY=$(openssl rand -base64 32)

# Create secret
kubectl create secret generic config-server-secret \
  --from-literal=username=config-user \
  --from-literal=password=$(openssl rand -base64 32) \
  --from-literal=encrypt-key=$ENCRYPT_KEY \
  --from-literal=repo-username=your-git-username \
  --from-literal=repo-password=your-git-token \
  -n currency-converter
```

2. Update ConfigMap with your Git repository:

```yaml
# k8s/config-server/base/configmap.yaml
data:
  repo-uri: "https://github.com/your-org/config-repo.git"
  repo-branch: "main"
```

### Deploy

```bash
# Development
kubectl apply -k k8s/config-server/overlays/dev

# Staging
kubectl apply -k k8s/config-server/overlays/staging

# Production
kubectl apply -k k8s/config-server/overlays/prod
```

### Verify Deployment

```bash
# Check pods
kubectl get pods -l app=config-server

# Check logs
kubectl logs -f deployment/config-server

# Port-forward to test
kubectl port-forward svc/config-server 8888:8888

# Test endpoint
curl http://config-user:password@localhost:8888/main-service/dev
```

## Monitoring

### Health Endpoints

```bash
# Liveness probe
curl http://localhost:8888/actuator/health/liveness

# Readiness probe
curl http://localhost:8888/actuator/health/readiness

# Full health
curl http://config-user:changeme@localhost:8888/actuator/health
```

### Metrics

Prometheus metrics available at:

```bash
curl http://config-user:changeme@localhost:8888/actuator/prometheus
```

Add to Prometheus scrape config:

```yaml
scrape_configs:
  - job_name: 'config-server'
    static_configs:
      - targets: ['config-server:8888']
    metrics_path: '/actuator/prometheus'
    basic_auth:
      username: config-user
      password: changeme
```

## Security Best Practices

1. **Change Default Credentials**
   ```bash
   export CONFIG_SERVER_USERNAME=your-username
   export CONFIG_SERVER_PASSWORD=$(openssl rand -base64 32)
   ```

2. **Use Strong Encryption Key**
   ```bash
   export ENCRYPT_KEY=$(openssl rand -base64 32)
   ```

3. **Secure Git Repository**
   - Use private repository
   - Use deploy keys or tokens (not passwords)
   - Enable audit logging

4. **Network Security**
   - Use HTTPS in production
   - Restrict access via network policies
   - Enable authentication

5. **Backup Encryption Keys**
   - Store securely (e.g., HashiCorp Vault, AWS Secrets Manager)
   - Without the key, encrypted values cannot be decrypted

6. **Rotate Secrets Regularly**
   - Update encryption keys
   - Re-encrypt configuration values
   - Update client configurations

## Troubleshooting

### Config Server Won't Start

```bash
# Check logs
./mvnw spring-boot:run

# Common issues:
# - Missing ENCRYPT_KEY
# - Invalid Git repository URI
# - Git credentials incorrect
```

### Client Can't Connect

```bash
# Verify config server is running
curl http://config-user:changeme@localhost:8888/actuator/health

# Check client bootstrap.yml
# Ensure spring.cloud.config.uri is correct

# Check authentication
# Username/password must match config server
```

### Encryption Fails

```bash
# Ensure ENCRYPT_KEY is set
echo $ENCRYPT_KEY

# Test encryption endpoint
curl -X POST http://config-user:changeme@localhost:8888/encrypt \
  -d "test-value"

# If fails, check config server logs
```

### Configuration Not Refreshing

```bash
# Ensure @RefreshScope is on beans
@RefreshScope
@Component
public class MyConfig { }

# Trigger refresh
curl -X POST http://localhost:8000/actuator/refresh

# Check actuator is enabled
management.endpoints.web.exposure.include=refresh
```

## API Reference

### Get Configuration

```bash
# Pattern: /{application}/{profile}/{label}
GET http://config-user:changeme@localhost:8888/main-service/dev/main

# Pattern: /{application}-{profile}.{format}
GET http://config-user:changeme@localhost:8888/main-service-dev.yml
GET http://config-user:changeme@localhost:8888/main-service-dev.json
```

### Encrypt

```bash
POST http://config-user:changeme@localhost:8888/encrypt
Content-Type: text/plain

my-secret-value
```

### Decrypt

```bash
POST http://config-user:changeme@localhost:8888/decrypt
Content-Type: text/plain

AQBEncryptedValueHere...
```

## Development Workflow

1. **Add new configuration**
   ```bash
   cd config-repo/main-service
   vim application-dev.yml  # Add new property
   ```

2. **Encrypt secrets**
   ```bash
   curl -X POST http://config-user:changeme@localhost:8888/encrypt \
     -d "secret-value"
   # Copy encrypted value to config file
   ```

3. **Commit and push**
   ```bash
   cd config-repo
   git add .
   git commit -m "Add new configuration"
   git push
   ```

4. **Refresh clients**
   ```bash
   curl -X POST http://localhost:8000/actuator/refresh
   curl -X POST http://localhost:8080/actuator/refresh
   ```

## References

- [Spring Cloud Config Documentation](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Encryption and Decryption](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_encryption_and_decryption)
- [Config Repository](../config-repo/README.md)
