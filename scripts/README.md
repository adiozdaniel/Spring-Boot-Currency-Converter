# Deployment Scripts

This directory contains deployment and operational scripts for the Currency Converter microservices.

> **Note:** All shell scripts (`.sh` files) in this directory have executable permissions set. If you encounter permission errors, run: `chmod +x scripts/*.sh`

## Available Scripts

### deploy.sh

Deploys the Currency Converter services to Kubernetes.

**Usage:**

```bash
./scripts/deploy.sh <environment>
```

**Parameters:**

- `environment`: Target environment (dev, staging, production)

**Example:**

```bash
# Deploy to development
./scripts/deploy.sh dev

# Deploy to staging
./scripts/deploy.sh staging

# Deploy to production
./scripts/deploy.sh production
```

**What it does:**

1. Validates the environment parameter
2. Sets up Kubernetes namespace
3. Applies PostgreSQL deployment and secrets
4. Applies ConfigMaps for both services
5. Deploys rate-service and main-service
6. Applies services and ingress configurations
7. Waits for deployments to be ready
8. Runs smoke tests

### smoke-test.sh

Runs basic health checks on deployed services.

**Usage:**

```bash
./scripts/smoke-test.sh <environment>
```

**Parameters:**

- `environment`: Target environment (dev, staging, production)

**Example:**

```bash
./scripts/smoke-test.sh dev
```

**What it does:**

1. Checks if services are accessible
2. Verifies health endpoints return 200 OK
3. Tests basic API functionality
4. Reports test results

### rollback.sh

Rolls back deployments to the previous version.

**Usage:**

```bash
./scripts/rollback.sh <environment> <service>
```

**Parameters:**

- `environment`: Target environment (dev, staging, production)
- `service`: Service name (main-service, rate-service, or all)

**Example:**

```bash
# Rollback all services in production
./scripts/rollback.sh production all

# Rollback only main-service in staging
./scripts/rollback.sh staging main-service
```

**What it does:**

1. Identifies the previous deployment revision
2. Rolls back to that revision
3. Verifies rollback success
4. Runs smoke tests

## Prerequisites

Before running these scripts, ensure you have:

- **kubectl** installed and configured
- **Access to Kubernetes cluster** with proper credentials
- **Namespace created** for the target environment
- **Secrets configured** (see `k8s/**/secret.yaml.example`)
- **Docker images built** and pushed to registry

## Environment Variables

The scripts use these environment variables (optional):

- `KUBERNETES_NAMESPACE`: Override the default namespace
- `DOCKER_REGISTRY`: Docker registry URL (default: ghcr.io)
- `IMAGE_TAG`: Docker image tag (default: latest)

**Example:**

```bash
export KUBERNETES_NAMESPACE=my-custom-namespace
export IMAGE_TAG=v1.2.3
./scripts/deploy.sh dev
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Deploy to Kubernetes
  run: ./scripts/deploy.sh ${{ env.ENVIRONMENT }}
  env:
    KUBERNETES_NAMESPACE: currency-converter-${{ env.ENVIRONMENT }}
    IMAGE_TAG: ${{ github.sha }}
```

### Jenkins

```groovy
stage('Deploy') {
    steps {
        sh """
            export IMAGE_TAG=${env.BUILD_NUMBER}
            ./scripts/deploy.sh ${env.ENVIRONMENT}
        """
    }
}
```

## Troubleshooting

### Permission Denied

If you see "Permission denied" errors:

```bash
chmod +x scripts/*.sh
```

### kubectl not found

Install kubectl:

```bash
# macOS
brew install kubectl

# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

### Namespace not found

Create the namespace:

```bash
kubectl create namespace currency-converter-dev
```

### Image pull errors

Ensure you're authenticated to the container registry:

```bash
# GitHub Container Registry
echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USERNAME --password-stdin
```

## Best Practices

1. **Always test in dev first** before deploying to staging or production
2. **Review changes** before running deployment scripts
3. **Monitor deployments** using `kubectl get pods -w`
4. **Keep rollback ready** - know how to quickly rollback if needed
5. **Run smoke tests** after each deployment
6. **Check logs** if deployments fail: `kubectl logs <pod-name>`

## See Also

- [Docker Compose](../docker-compose.yml)
- [CI/CD Workflows](../.github/workflows/)
