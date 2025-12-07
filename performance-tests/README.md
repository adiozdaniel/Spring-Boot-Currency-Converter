# Performance Testing

This directory contains performance testing configurations for the Currency Converter microservices using JMeter and Gatling.

## Tools

- **JMeter**: Traditional performance testing tool with GUI
- **Gatling**: Scala-based performance testing tool with code-as-configuration

## Directory Structure

```yaml
performance-tests/
├── jmeter/
│   ├── main-service-load-test.jmx
│   ├── rate-service-load-test.jmx
│   └── run-jmeter.sh
├── gatling/
│   ├── src/test/scala/simulations/
│   │   ├── MainServiceSimulation.scala
│   │   └── RateServiceSimulation.scala
│   ├── pom.xml
│   └── run-gatling.sh
├── results/
│   ├── jmeter/
│   └── gatling/
└── README.md
```

## Prerequisites

### JMeter

```bash
# Install JMeter
wget https://downloads.apache.org/jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
export JMETER_HOME=/path/to/apache-jmeter-5.6.3
export PATH=$PATH:$JMETER_HOME/bin
```

### Gatling

```bash
# Gatling is managed via Maven, no separate installation needed
cd gatling
./mvnw clean gatling:test
```

## Running Performance Tests

### JMeter Tests

#### Main Service Load Test

```bash
cd performance-tests/jmeter
./run-jmeter.sh main-service-load-test.jmx
```

#### Rate Service Load Test

```bash
cd performance-tests/jmeter
./run-jmeter.sh rate-service-load-test.jmx
```

#### Custom Configuration

```bash
# Run with custom parameters
jmeter -n -t main-service-load-test.jmx \
  -Jusers=100 \
  -Jrampup=60 \
  -Jduration=300 \
  -Jhost=localhost \
  -Jport=8000 \
  -l results/jmeter/main-service-results.jtl \
  -e -o results/jmeter/main-service-report
```

### Gatling Tests

#### Run All Simulations

```bash
cd performance-tests/gatling
./run-gatling.sh
```

#### Run Specific Simulation

```bash
cd performance-tests/gatling
./mvnw gatling:test -Dgatling.simulationClass=simulations.MainServiceSimulation
```

## Test Scenarios

### Main Service Tests

1. **Conversion Request Load Test**
   - Endpoint: `POST /api/v1/convert`
   - Load: 100 concurrent users
   - Duration: 5 minutes
   - Ramp-up: 30 seconds

2. **Conversion History Load Test**
   - Endpoint: `GET /api/v1/conversions`
   - Load: 50 concurrent users
   - Duration: 5 minutes

### Rate Service Tests

1. **Exchange Rate Load Test**
   - Endpoint: `GET /api/v1/rates/{currency}`
   - Load: 200 concurrent users
   - Duration: 5 minutes
   - Ramp-up: 60 seconds

2. **Health Check Load Test**
   - Endpoint: `GET /actuator/health`
   - Load: 50 concurrent users
   - Duration: 2 minutes

## Performance Thresholds

### Response Time Targets

| Endpoint | 50th Percentile | 95th Percentile | 99th Percentile | Max |
|----------|----------------|-----------------|-----------------|-----|
| POST /api/v1/convert | < 200ms | < 500ms | < 1000ms | < 2000ms |
| GET /api/v1/conversions | < 100ms | < 300ms | < 500ms | < 1000ms |
| GET /api/v1/rates/* | < 100ms | < 200ms | < 400ms | < 800ms |
| GET /actuator/health | < 50ms | < 100ms | < 200ms | < 500ms |

### Throughput Targets

- Main Service: > 500 requests/second
- Rate Service: > 1000 requests/second

### Error Rate Targets

- Error rate: < 0.1%
- Timeout rate: < 0.01%

## CI/CD Integration

### GitHub Actions

Add this job to your workflow:

```yaml
performance-test:
  runs-on: ubuntu-latest
  needs: [build, test]
  if: github.event_name == 'push' && github.ref == 'refs/heads/develop'

  steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Start services
      run: docker-compose up -d

    - name: Wait for services
      run: |
        timeout 60 bash -c 'until curl -f http://localhost:8000/actuator/health; do sleep 2; done'
        timeout 60 bash -c 'until curl -f http://localhost:8080/actuator/health; do sleep 2; done'

    - name: Run JMeter tests
      run: |
        cd performance-tests/jmeter
        ./run-jmeter.sh main-service-load-test.jmx
        ./run-jmeter.sh rate-service-load-test.jmx

    - name: Upload JMeter results
      uses: actions/upload-artifact@v4
      with:
        name: jmeter-results
        path: performance-tests/results/jmeter/

    - name: Run Gatling tests
      run: |
        cd performance-tests/gatling
        ./run-gatling.sh

    - name: Upload Gatling results
      uses: actions/upload-artifact@v4
      with:
        name: gatling-results
        path: performance-tests/gatling/target/gatling/

    - name: Stop services
      if: always()
      run: docker-compose down
```

### Jenkins

Add this stage to your Jenkinsfile:

```groovy
stage('Performance Tests') {
    when {
        branch 'develop'
    }
    steps {
        script {
            sh 'docker-compose up -d'

            sh '''
                timeout 60 bash -c 'until curl -f http://localhost:8000/actuator/health; do sleep 2; done'
                timeout 60 bash -c 'until curl -f http://localhost:8080/actuator/health; do sleep 2; done'
            '''

            dir('performance-tests/jmeter') {
                sh './run-jmeter.sh main-service-load-test.jmx'
                sh './run-jmeter.sh rate-service-load-test.jmx'
            }

            dir('performance-tests/gatling') {
                sh './run-gatling.sh'
            }

            perfReport sourceDataFiles: 'performance-tests/results/jmeter/*.jtl',
                       compareBuildPrevious: true,
                       errorFailedThreshold: 5,
                       errorUnstableThreshold: 2
        }
    }
    post {
        always {
            sh 'docker-compose down'
            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'performance-tests/results/jmeter',
                reportFiles: 'index.html',
                reportName: 'JMeter Report'
            ])
            gatlingArchive()
        }
    }
}
```

## Analyzing Results

### JMeter Reports

JMeter generates HTML reports in `performance-tests/results/jmeter/`:

- Open `index.html` in a browser
- Review key metrics: throughput, response times, error rate
- Check graphs: Response Times Over Time, Transactions per Second

### Gatling Reports

Gatling generates HTML reports in `performance-tests/gatling/target/gatling/`:

- Open the latest simulation report
- Review Global Information, Request Details, and Percentiles
- Analyze charts for response time distribution

## Best Practices

1. **Baseline Testing**: Always establish a performance baseline before making changes
2. **Consistent Environment**: Run tests in a consistent environment (same hardware, network)
3. **Warm-up Period**: Allow services to warm up before starting load
4. **Realistic Scenarios**: Use realistic user scenarios and data
5. **Monitor Resources**: Monitor CPU, memory, and network during tests
6. **Trend Analysis**: Compare results over time to detect regressions
7. **Load Patterns**: Test with various load patterns (steady, spike, stress)

## Troubleshooting

### Services Not Responding

```bash
# Check service logs
docker-compose logs main-service
docker-compose logs rate-service

# Check service health
curl http://localhost:8000/actuator/health
curl http://localhost:8080/actuator/health
```

### High Error Rates

- Check service logs for errors
- Verify database connectivity
- Check resource limits (CPU, memory)
- Review application configuration

### Performance Degradation

- Profile the application using JProfiler or YourKit
- Check database query performance
- Review caching strategy
- Analyze GC logs
