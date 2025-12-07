#!/bin/bash

# Gatling Performance Test Runner Script
# Usage: ./run-gatling.sh [simulation-class]

set -e

echo "=========================================="
echo "Gatling Performance Test Runner"
echo "=========================================="

# Default parameters
USERS=${USERS:-50}
RAMP_DURATION=${RAMP_DURATION:-30}
TEST_DURATION=${TEST_DURATION:-300}
MAIN_SERVICE_URL=${MAIN_SERVICE_URL:-http://localhost:8000}
RATE_SERVICE_URL=${RATE_SERVICE_URL:-http://localhost:8080}

echo "Configuration:"
echo "  Users: $USERS"
echo "  Ramp-up Duration: $RAMP_DURATION seconds"
echo "  Test Duration: $TEST_DURATION seconds"
echo "  Main Service URL: $MAIN_SERVICE_URL"
echo "  Rate Service URL: $RATE_SERVICE_URL"
echo "=========================================="

# Check if Maven wrapper exists
if [ ! -f "./mvnw" ]; then
    echo "Error: Maven wrapper not found"
    echo "Please ensure you're in the gatling directory"
    exit 1
fi

# Check if simulation class is provided
if [ -n "$1" ]; then
    SIMULATION_CLASS=$1
    echo "Running simulation: $SIMULATION_CLASS"
    echo "=========================================="

    ./mvnw clean gatling:test \
        -Dgatling.simulationClass="$SIMULATION_CLASS" \
        -Dusers="$USERS" \
        -DrampDuration="$RAMP_DURATION" \
        -DtestDuration="$TEST_DURATION" \
        -DbaseUrl="$MAIN_SERVICE_URL"
else
    echo "Running all simulations..."
    echo "=========================================="

    # Run Main Service Simulation
    echo ""
    echo "Running Main Service Simulation..."
    ./mvnw clean gatling:test \
        -Dgatling.simulationClass=simulations.MainServiceSimulation \
        -Dusers="$USERS" \
        -DrampDuration="$RAMP_DURATION" \
        -DtestDuration="$TEST_DURATION" \
        -DbaseUrl="$MAIN_SERVICE_URL"

    # Run Rate Service Simulation
    echo ""
    echo "Running Rate Service Simulation..."
    ./mvnw gatling:test \
        -Dgatling.simulationClass=simulations.RateServiceSimulation \
        -Dusers="$((USERS * 2))" \
        -DrampDuration="$((RAMP_DURATION * 2))" \
        -DtestDuration="$TEST_DURATION" \
        -DbaseUrl="$RATE_SERVICE_URL"
fi

echo ""
echo "=========================================="
echo "Tests completed successfully!"
echo "Reports saved to: target/gatling/"
echo "=========================================="

# Find and display latest report
LATEST_REPORT=$(find target/gatling -name "index.html" -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)
if [ -n "$LATEST_REPORT" ]; then
    echo "Latest report: $LATEST_REPORT"
fi

exit 0
