# Secrets Management Guide

This project uses example/template files for secrets. You need to copy and customize them with your actual values.

## Quick Setup

### 1. Docker Compose (Local Development)

```bash
# Copy the example file
cp .env.example .env

# Edit .env with your actual values
nano .env  # or use your preferred editor
```

### 2. Kubernetes (Production/Staging)

```bash
# PostgreSQL secrets
cp k8s/postgres/secret.yaml.example k8s/postgres/secret.yaml
# Edit and add your actual base64-encoded credentials
nano k8s/postgres/secret.yaml

# Rate service secrets
cp k8s/rate-service/secret.yaml.example k8s/rate-service/secret.yaml
# Edit and add your actual base64-encoded API key
nano k8s/rate-service/secret.yaml
```

**To encode values to base64:**
```bash
echo -n "your-actual-password" | base64
```

### 3. Monitoring (AlertManager)

```bash
# Copy the example
cp monitoring/alertmanager/alertmanager.yml.example monitoring/alertmanager/alertmanager.yml

# Edit and replace placeholders
nano monitoring/alertmanager/alertmanager.yml

# Replace these placeholders:
# - <smtp-password>
# - <slack-webhook-url>
# - <teams-webhook-url>
# - <pagerduty-service-key>
```

## Important Notes

- **NEVER commit** the actual secret files (`.env`, `secret.yaml`, `alertmanager.yml`)
- These files are already in `.gitignore`
- Only commit the `.example` files
- For production, consider using:
  - Kubernetes Sealed Secrets
  - External Secrets Operator
  - HashiCorp Vault
  - Cloud provider secret managers (AWS Secrets Manager, Azure Key Vault, etc.)

## Files to Create

| Template File | Create As | Used For |
|--------------|-----------|----------|
| `.env.example` | `.env` | Docker Compose |
| `k8s/postgres/secret.yaml.example` | `k8s/postgres/secret.yaml` | Kubernetes PostgreSQL credentials |
| `k8s/rate-service/secret.yaml.example` | `k8s/rate-service/secret.yaml` | Kubernetes Rate Service API key |
| `monitoring/alertmanager/alertmanager.yml.example` | `monitoring/alertmanager/alertmanager.yml` | AlertManager notifications |
