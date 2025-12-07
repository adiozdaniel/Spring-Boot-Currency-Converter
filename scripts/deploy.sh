#!/bin/bash

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Default values
ENVIRONMENT=${1:-development}
IMAGE_TAG=${2:-latest}
DOCKER_REGISTRY=${DOCKER_REGISTRY:-"your-registry.example.com"}

echo -e "${GREEN}====================================${NC}"
echo -e "${GREEN}Deploying Currency Converter Services${NC}"
echo -e "${GREEN}Environment: $ENVIRONMENT${NC}"
echo -e "${GREEN}Image Tag: $IMAGE_TAG${NC}"
echo -e "${GREEN}====================================${NC}"

# Validate environment
case $ENVIRONMENT in
  development|staging|production)
    ;;
  *)
    echo -e "${RED}Error: Invalid environment '$ENVIRONMENT'. Must be development, staging, or production.${NC}"
    exit 1
    ;;
esac

# Set namespace based on environment
NAMESPACE="currency-converter-$ENVIRONMENT"

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}kubectl is not installed${NC}"
    exit 1
fi

# Check if connected to cluster
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Not connected to Kubernetes cluster${NC}"
    exit 1
fi

# Create namespace if it doesn't exist
echo -e "${YELLOW}Creating namespace if not exists...${NC}"
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Apply secrets (in production, use sealed secrets or external secrets operator)
echo -e "${YELLOW}Applying secrets...${NC}"
kubectl apply -f k8s/postgres/secret.yaml -n $NAMESPACE
kubectl apply -f k8s/rate-service/secret.yaml -n $NAMESPACE

# Apply ConfigMaps
echo -e "${YELLOW}Applying ConfigMaps...${NC}"
kubectl apply -f k8s/postgres/configmap.yaml -n $NAMESPACE
kubectl apply -f k8s/rate-service/configmap.yaml -n $NAMESPACE
kubectl apply -f k8s/main-service/configmap.yaml -n $NAMESPACE

# Apply PVC
echo -e "${YELLOW}Applying Persistent Volume Claims...${NC}"
kubectl apply -f k8s/postgres/pvc.yaml -n $NAMESPACE

# Deploy PostgreSQL
echo -e "${YELLOW}Deploying PostgreSQL...${NC}"
kubectl apply -f k8s/postgres/deployment.yaml -n $NAMESPACE
kubectl apply -f k8s/postgres/service.yaml -n $NAMESPACE

# Wait for PostgreSQL to be ready
echo -e "${YELLOW}Waiting for PostgreSQL to be ready...${NC}"
kubectl wait --for=condition=available --timeout=300s deployment/postgres -n $NAMESPACE

# Deploy Rate Service
echo -e "${YELLOW}Deploying Rate Service...${NC}"
export IMAGE_TAG DOCKER_REGISTRY
envsubst < k8s/rate-service/deployment.yaml | kubectl apply -f - -n $NAMESPACE
kubectl apply -f k8s/rate-service/service.yaml -n $NAMESPACE
kubectl apply -f k8s/rate-service/hpa.yaml -n $NAMESPACE

# Wait for Rate Service to be ready
echo -e "${YELLOW}Waiting for Rate Service to be ready...${NC}"
kubectl wait --for=condition=available --timeout=300s deployment/rate-service -n $NAMESPACE

# Deploy Main Service
echo -e "${YELLOW}Deploying Main Service...${NC}"
envsubst < k8s/main-service/deployment.yaml | kubectl apply -f - -n $NAMESPACE
kubectl apply -f k8s/main-service/service.yaml -n $NAMESPACE
kubectl apply -f k8s/main-service/hpa.yaml -n $NAMESPACE

if [ "$ENVIRONMENT" = "production" ]; then
    kubectl apply -f k8s/main-service/ingress.yaml -n $NAMESPACE
fi

# Wait for Main Service to be ready
echo -e "${YELLOW}Waiting for Main Service to be ready...${NC}"
kubectl wait --for=condition=available --timeout=300s deployment/main-service -n $NAMESPACE

# Display deployment status
echo -e "${GREEN}====================================${NC}"
echo -e "${GREEN}Deployment Status${NC}"
echo -e "${GREEN}====================================${NC}"
kubectl get all -n $NAMESPACE

# Run smoke tests
echo -e "${YELLOW}Running smoke tests...${NC}"
bash scripts/smoke-test.sh $NAMESPACE

echo -e "${GREEN}====================================${NC}"
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo -e "${GREEN}====================================${NC}"
