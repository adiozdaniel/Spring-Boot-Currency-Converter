# Configuration Repository

This repository contains externalized configuration for the Currency Converter microservices.

## Structure

```
config-repo/
├── main-service/
│   ├── application.yml           # Common configuration
│   ├── application-dev.yml       # Development environment
│   ├── application-staging.yml   # Staging environment
│   └── application-prod.yml      # Production environment
├── rate-service/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-staging.yml
│   └── application-prod.yml
└── auth-service/
    └── (future authentication service configs)
```

## Encryption

### Encrypting Values

1. Start the config server
2. Use the `/encrypt` endpoint:

```bash
curl -X POST http://config-user:changeme@localhost:8888/encrypt \
  -d "my-secret-value"
```

3. Copy the encrypted value
4. Add to configuration with `{cipher}` prefix:

```yaml
api:
  key: '{cipher}AQBEncryptedValueHere...'
```

### Decrypting Values (for verification)

```bash
curl -X POST http://config-user:changeme@localhost:8888/decrypt \
  -d "AQBEncryptedValueHere..."
```

## Environment Variables

The config server supports these environment variables:

- `ENCRYPT_KEY`: Symmetric encryption key (required in production)
- `CONFIG_REPO_URI`: Git repository URI
- `CONFIG_REPO_BRANCH`: Branch to use (default: main)
- `CONFIG_REPO_USERNAME`: Git username (if private repo)
- `CONFIG_REPO_PASSWORD`: Git password or token (if private repo)

## Usage by Services

Services load configuration at startup:

```yaml
# bootstrap.yml in client service
spring:
  cloud:
    config:
      uri: http://config-server:8888
      username: config-user
      password: changeme
      fail-fast: true
```

## Security Best Practices

1. **Never commit plaintext secrets** - Always encrypt sensitive values
2. **Rotate encryption keys** regularly in production
3. **Use environment-specific keys** - Different keys for dev/staging/prod
4. **Restrict config server access** - Use strong authentication
5. **Use HTTPS** in production for config server endpoints
6. **Audit configuration changes** - Track who changed what and when
7. **Backup encryption keys** securely (without them, encrypted values are useless)

## Git Setup

This directory should be initialized as a separate Git repository:

```bash
cd config-repo
git init
git add .
git commit -m "Initial configuration"
```

For production, use a private remote repository:

```bash
git remote add origin https://github.com/your-org/config-repo.git
git push -u origin main
```

## Refreshing Configuration

Services can refresh configuration without restart:

```bash
curl -X POST http://localhost:8000/actuator/refresh
```

For classes using `@RefreshScope` annotation, configuration will be reloaded.

## Testing Configuration

Test configuration loading:

```bash
# Get main-service dev configuration
curl http://config-user:changeme@localhost:8888/main-service/dev

# Get rate-service prod configuration
curl http://config-user:changeme@localhost:8888/rate-service/prod
```
