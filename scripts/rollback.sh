#!/bin/bash

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ENVIRONMENT=${1:-development}
SERVICE=${2:-all}

echo -e "${YELLOW}Rolling back deployment in $ENVIRONMENT${NC}"

NAMESPACE="currency-converter-$ENVIRONMENT"

case $SERVICE in
  main-service)
    echo -e "${YELLOW}Rolling back Main Service...${NC}"
    kubectl rollout undo deployment/main-service -n $NAMESPACE
    kubectl rollout status deployment/main-service -n $NAMESPACE
    ;;
  rate-service)
    echo -e "${YELLOW}Rolling back Rate Service...${NC}"
    kubectl rollout undo deployment/rate-service -n $NAMESPACE
    kubectl rollout status deployment/rate-service -n $NAMESPACE
    ;;
  all)
    echo -e "${YELLOW}Rolling back all services...${NC}"
    kubectl rollout undo deployment/main-service -n $NAMESPACE
    kubectl rollout undo deployment/rate-service -n $NAMESPACE
    kubectl rollout status deployment/main-service -n $NAMESPACE
    kubectl rollout status deployment/rate-service -n $NAMESPACE
    ;;
  *)
    echo -e "${RED}Invalid service name. Use: main-service, rate-service, or all${NC}"
    exit 1
    ;;
esac

echo -e "${GREEN}Rollback completed successfully${NC}"
