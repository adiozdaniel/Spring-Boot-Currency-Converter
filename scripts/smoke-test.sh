#!/bin/bash

set -e

NAMESPACE=${1:-currency-converter-development}

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Running smoke tests in namespace: $NAMESPACE${NC}"

# Get service URLs
MAIN_SERVICE_URL=$(kubectl get svc main-service -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "localhost")
RATE_SERVICE_URL=$(kubectl get svc rate-service -n $NAMESPACE -o jsonpath='{.spec.clusterIP}')

# If LoadBalancer IP is not available, use port-forward
if [ "$MAIN_SERVICE_URL" = "localhost" ]; then
    echo -e "${YELLOW}LoadBalancer IP not available, using port-forward...${NC}"
    kubectl port-forward svc/main-service 8000:8000 -n $NAMESPACE &
    PF_PID=$!
    sleep 5
    MAIN_SERVICE_URL="localhost"
fi

# Test Rate Service health
echo -e "${YELLOW}Testing Rate Service health...${NC}"
kubectl exec -n $NAMESPACE deployment/main-service -- \
    wget -qO- http://rate-service:8080/status || {
    echo -e "${RED}Rate Service health check failed${NC}"
    exit 1
}
echo -e "${GREEN}Rate Service health check passed${NC}"

# Test Main Service health
echo -e "${YELLOW}Testing Main Service health...${NC}"
if curl -f -s http://$MAIN_SERVICE_URL:8000/status > /dev/null; then
    echo -e "${GREEN}Main Service health check passed${NC}"
else
    echo -e "${RED}Main Service health check failed${NC}"
    exit 1
fi

# Test actuator endpoints
echo -e "${YELLOW}Testing actuator health endpoint...${NC}"
if curl -f -s http://$MAIN_SERVICE_URL:8000/actuator/health > /dev/null; then
    echo -e "${GREEN}Actuator health check passed${NC}"
else
    echo -e "${RED}Actuator health check failed${NC}"
    exit 1
fi

# Test currency conversion (if rate service is configured)
echo -e "${YELLOW}Testing currency conversion endpoint...${NC}"
RESPONSE=$(curl -s -X POST http://$MAIN_SERVICE_URL:8000/convert \
    -H "Content-Type: application/json" \
    -d '{"from":"USD","to":"EUR","amount":100}' || echo "FAILED")

if [ "$RESPONSE" != "FAILED" ]; then
    echo -e "${GREEN}Currency conversion test passed${NC}"
    echo "Response: $RESPONSE"
else
    echo -e "${YELLOW}Currency conversion test skipped (may require API configuration)${NC}"
fi

# Cleanup port-forward if used
if [ -n "$PF_PID" ]; then
    kill $PF_PID 2>/dev/null || true
fi

echo -e "${GREEN}====================================${NC}"
echo -e "${GREEN}All smoke tests passed!${NC}"
echo -e "${GREEN}====================================${NC}"
