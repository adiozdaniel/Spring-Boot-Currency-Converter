#!/bin/bash

# JMeter Performance Test Runner Script
# Usage: ./run-jmeter.sh <test-plan.jmx>

set -e

# Check if JMeter is installed
if ! command -v jmeter &> /dev/null; then
    echo "Error: JMeter is not installed or not in PATH"
    echo "Please install JMeter and add it to your PATH"
    exit 1
fi

# Check if test plan is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <test-plan.jmx>"
    echo "Example: $0 main-service-load-test.jmx"
    exit 1
fi

TEST_PLAN=$1
TEST_NAME=$(basename "$TEST_PLAN" .jmx)
RESULTS_DIR="../results/jmeter/${TEST_NAME}-$(date +%Y%m%d-%H%M%S)"

# Create results directory
mkdir -p "$RESULTS_DIR"

echo "=========================================="
echo "JMeter Performance Test Runner"
echo "=========================================="
echo "Test Plan: $TEST_PLAN"
echo "Results Directory: $RESULTS_DIR"
echo "=========================================="

# Default parameters
USERS=${USERS:-50}
RAMPUP=${RAMPUP:-30}
DURATION=${DURATION:-300}
HOST=${HOST:-localhost}
MAIN_PORT=${MAIN_PORT:-8000}
RATE_PORT=${RATE_PORT:-8080}

echo "Configuration:"
echo "  Users: $USERS"
echo "  Ramp-up: $RAMPUP seconds"
echo "  Duration: $DURATION seconds"
echo "  Host: $HOST"
echo "  Main Service Port: $MAIN_PORT"
echo "  Rate Service Port: $RATE_PORT"
echo "=========================================="

# Run JMeter in non-GUI mode
jmeter -n \
    -t "$TEST_PLAN" \
    -Jusers="$USERS" \
    -Jrampup="$RAMPUP" \
    -Jduration="$DURATION" \
    -Jhost="$HOST" \
    -Jmain_port="$MAIN_PORT" \
    -Jrate_port="$RATE_PORT" \
    -l "$RESULTS_DIR/results.jtl" \
    -j "$RESULTS_DIR/jmeter.log" \
    -e -o "$RESULTS_DIR/html-report"

echo "=========================================="
echo "Test completed successfully!"
echo "Results saved to: $RESULTS_DIR"
echo "HTML Report: $RESULTS_DIR/html-report/index.html"
echo "=========================================="

# Display summary
if [ -f "$RESULTS_DIR/results.jtl" ]; then
    echo ""
    echo "Quick Summary:"
    echo "Total Samples: $(grep -c '^[0-9]' "$RESULTS_DIR/results.jtl" || echo 0)"
    echo ""
fi

exit 0
