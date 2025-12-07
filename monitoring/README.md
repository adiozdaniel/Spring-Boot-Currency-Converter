# Monitoring Configuration

This directory contains monitoring configuration for the Currency Converter microservices using Prometheus, Grafana, and Alertmanager.

## Components

- **Prometheus**: Metrics collection and storage
- **Grafana**: Metrics visualization and dashboards
- **Alertmanager**: Alert routing and notification
- **Micrometer**: Application metrics instrumentation (already included in Spring Boot Actuator)

## Directory Structure

```yaml
monitoring/
├── prometheus/
│   ├── prometheus.yml           # Prometheus configuration
│   ├── alerts.yml              # Alert rules
│   └── scrape-configs/         # Service discovery configs
├── grafana/
│   ├── dashboards/
│   │   ├── main-service-dashboard.json
│   │   ├── rate-service-dashboard.json
│   │   └── overview-dashboard.json
│   └── provisioning/
│       ├── datasources.yml
│       └── dashboards.yml
├── alertmanager/
│   └── alertmanager.yml        # Alertmanager configuration
├── docker-compose.monitoring.yml
└── README.md
```

## Quick Start

### Using Docker Compose

```bash
# Start monitoring stack
docker-compose -f docker-compose.monitoring.yml up -d

# Access dashboards
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
# Alertmanager: http://localhost:9093
```

### Using Kubernetes

```bash
# Create monitoring namespace
kubectl create namespace monitoring

# Deploy Prometheus
kubectl apply -f k8s/monitoring/prometheus/

# Deploy Grafana
kubectl apply -f k8s/monitoring/grafana/

# Deploy Alertmanager
kubectl apply -f k8s/monitoring/alertmanager/
```

## Application Configuration

Both services already include Spring Boot Actuator with Micrometer. Ensure these properties are set:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${SPRING_PROFILES_ACTIVE:development}
```

## Prometheus Configuration

### Scrape Configuration

Prometheus is configured to scrape metrics from:

- Main Service: `http://main-service:8000/actuator/prometheus`
- Rate Service: `http://rate-service:8080/actuator/prometheus`
- Kubernetes pods with annotation `prometheus.io/scrape: "true"`

### Retention

- Default retention: 15 days
- Storage: Local disk (configurable to use persistent volumes)

## Grafana Dashboards

### 1. Overview Dashboard

Displays:

- Overall system health
- Request rates across all services
- Error rates
- Response time percentiles
- JVM metrics overview

### 2. Main Service Dashboard

Displays:

- Conversion request metrics
- Database connection pool status
- JVM memory usage
- Garbage collection metrics
- Thread pool metrics
- HTTP request/response metrics

### 3. Rate Service Dashboard

Displays:

- Exchange rate request metrics
- Cache hit/miss rates
- External API call metrics
- JVM metrics
- Response time distribution

## Metrics Available

### Spring Boot Actuator Metrics

#### JVM Metrics

- `jvm.memory.used` - Memory usage by pool
- `jvm.memory.max` - Maximum memory
- `jvm.gc.pause` - GC pause times
- `jvm.threads.live` - Thread count

#### HTTP Metrics

- `http.server.requests` - HTTP request metrics
- Labels: uri, method, status, exception

#### Application Metrics

- `process.cpu.usage` - CPU usage
- `system.cpu.usage` - System CPU
- `process.uptime` - Application uptime

### Custom Application Metrics

#### Main Service

- `currency.conversion.requests.total` - Total conversion requests
- `currency.conversion.duration` - Conversion processing time
- `currency.conversion.errors.total` - Conversion errors

#### Rate Service

- `exchange.rate.requests.total` - Total rate requests
- `exchange.rate.cache.hits` - Cache hit count
- `exchange.rate.cache.misses` - Cache miss count
- `exchange.rate.api.calls` - External API calls

## Alert Rules

### Critical Alerts

#### High Error Rate

```yaml
alert: HighErrorRate
expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.05
severity: critical
description: Error rate is above 5%
```

#### Service Down

```yaml
alert: ServiceDown
expr: up{job=~"main-service|rate-service"} == 0
severity: critical
description: Service is down
```

#### High Response Time

```yaml
alert: HighResponseTime
expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1
severity: warning
description: 95th percentile response time is above 1 second
```

### Warning Alerts

#### High Memory Usage

```yaml
alert: HighMemoryUsage
expr: (jvm_memory_used_bytes / jvm_memory_max_bytes) > 0.9
severity: warning
description: JVM memory usage is above 90%
```

#### High CPU Usage

```yaml
alert: HighCPUUsage
expr: system_cpu_usage > 0.8
severity: warning
description: CPU usage is above 80%
```

## Alertmanager Configuration

### Notification Channels

#### Slack

```yaml
receivers:
  - name: slack-notifications
    slack_configs:
      - api_url: '<slack-webhook-url>'
        channel: '#alerts'
        title: 'Currency Converter Alert'
```

#### Email

```yaml
receivers:
  - name: email-notifications
    email_configs:
      - to: 'team@example.com'
        from: 'alerts@example.com'
        smarthost: 'smtp.gmail.com:587'
```

#### PagerDuty

```yaml
receivers:
  - name: pagerduty-critical
    pagerduty_configs:
      - service_key: '<pagerduty-integration-key>'
```

### Alert Routing

```yaml
route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 5m
  repeat_interval: 3h
  receiver: 'slack-notifications'

  routes:
    - match:
        severity: critical
      receiver: 'pagerduty-critical'
    - match:
        severity: warning
      receiver: 'slack-notifications'
```

## Dashboard Screenshots

### Accessing Dashboards

1. Open Grafana: `http://localhost:3000` or `https://grafana.your-domain.com`
2. Login with credentials (default: admin/admin)
3. Navigate to Dashboards
4. Select:
   - "Currency Converter - Overview"
   - "Currency Converter - Main Service"
   - "Currency Converter - Rate Service"

### Dashboard Features

- **Time Range Selector**: Adjust time window for metrics
- **Auto Refresh**: Configure auto-refresh interval
- **Variables**: Filter by service, environment, instance
- **Annotations**: Mark deployments and incidents
- **Export/Import**: Share dashboard configurations

## Custom Metrics Integration

### Adding Custom Metrics to Your Code

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

@Service
public class ConversionService {
    private final Counter conversionCounter;
    private final Timer conversionTimer;

    public ConversionService(MeterRegistry registry) {
        this.conversionCounter = Counter.builder("currency.conversion.requests")
            .tag("type", "conversion")
            .description("Total currency conversion requests")
            .register(registry);

        this.conversionTimer = Timer.builder("currency.conversion.duration")
            .tag("type", "conversion")
            .description("Currency conversion processing time")
            .register(registry);
    }

    public ConversionResult convert(ConversionRequest request) {
        conversionCounter.increment();
        return conversionTimer.record(() -> {
            // Conversion logic here
            return performConversion(request);
        });
    }
}
```

## Query Examples

### Prometheus Queries

#### Request Rate

```promql
rate(http_server_requests_total[5m])
```

#### Error Rate

```promql
rate(http_server_requests_total{status=~"5.."}[5m])
```

#### 95th Percentile Response Time

```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

#### Memory Usage Percentage

```promql
100 * (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"})
```

#### Conversion Rate

```promql
rate(currency_conversion_requests_total[5m])
```

## Troubleshooting

### Prometheus Not Scraping Targets

```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Verify service endpoints
curl http://main-service:8000/actuator/prometheus
curl http://rate-service:8080/actuator/prometheus
```

### Grafana Dashboard Not Showing Data

1. Verify Prometheus datasource configuration
2. Check Prometheus is collecting metrics
3. Verify time range selection
4. Check dashboard variables

### High Memory Usage in Prometheus

```bash
# Reduce retention period
--storage.tsdb.retention.time=7d

# Limit sample retention
--storage.tsdb.retention.size=50GB
```

## Best Practices

1. **Label Cardinality**: Avoid high-cardinality labels (e.g., user IDs, request IDs)
2. **Metric Naming**: Follow naming conventions (e.g., `service_operation_unit_total`)
3. **Dashboard Organization**: Group related metrics logically
4. **Alert Fatigue**: Set appropriate thresholds to avoid too many alerts
5. **Documentation**: Document custom metrics and their purpose
6. **Regular Review**: Periodically review and update dashboards and alerts
7. **Backup**: Export and version control dashboard configurations

## Security Considerations

1. **Authentication**: Enable authentication for Grafana and Prometheus
2. **TLS**: Use HTTPS for Grafana access
3. **Network Policies**: Restrict access to monitoring stack
4. **Secrets Management**: Use Kubernetes secrets for sensitive configs
5. **RBAC**: Implement role-based access control in Grafana

## Performance Tuning

### Prometheus

```yaml
# prometheus.yml
global:
  scrape_interval: 15s      # Default scrape interval
  evaluation_interval: 15s  # Alert evaluation interval

scrape_configs:
  - job_name: 'main-service'
    scrape_interval: 10s    # More frequent for critical services
    scrape_timeout: 5s
```

### Grafana

```ini
# grafana.ini
[dashboards]
min_refresh_interval = 5s

[users]
viewers_can_edit = false

[auth.anonymous]
enabled = false
```

## Additional Resources

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Micrometer Documentation](https://micrometer.io/docs/)
- [Spring Boot Actuator Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
