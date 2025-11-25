# Data Import - Monitoring and Logging Guide

## Overview

This guide provides comprehensive instructions for setting up monitoring, logging, and alerting for the data import feature. Proper monitoring ensures early detection of issues, optimal performance, and quick incident response.

## Table of Contents

1. [Logging Configuration](#logging-configuration)
2. [Monitoring Metrics](#monitoring-metrics)
3. [Alert Configuration](#alert-configuration)
4. [Log Analysis](#log-analysis)
5. [Performance Monitoring](#performance-monitoring)
6. [Troubleshooting with Logs](#troubleshooting-with-logs)

---

## Logging Configuration

### Application Logging

The data import feature uses AdonisJS's built-in logger (Pino) for structured logging.

#### Log Levels

```bash
# Development
LOG_LEVEL=debug

# Staging
LOG_LEVEL=info

# Production
LOG_LEVEL=warn
```

**Log level hierarchy:**
- `error`: Critical errors requiring immediate attention
- `warn`: Warning conditions that should be investigated
- `info`: Informational messages for normal operations
- `debug`: Detailed information for debugging
- `trace`: Very detailed information (not used in production)

#### Logging Pattern

**Standard log entry structure:**
```typescript
logger.info('Operation name', {
  userId: number,
  tenantId: string,
  datasetId: number,
  fileName: string,
  fileSize: number,
  duration: number,
  ipAddress: string,
  userAgent: string,
  timestamp: string
})
```

### Dataset Operation Logging

#### File Upload Logging

**Success:**
```typescript
logger.info('Dataset upload started', {
  userId: user.id,
  tenantId: user.tenantId,
  fileName: file.clientName,
  fileSize: file.size,
  fileFormat: file.extname,
  ipAddress: ctx.request.ip(),
  userAgent: ctx.request.header('user-agent')
})

logger.info('Dataset upload completed', {
  userId: user.id,
  tenantId: user.tenantId,
  datasetId: dataset.id,
  fileName: dataset.fileName,
  fileSize: dataset.fileSize,
  rowCount: dataset.rowCount,
  columnCount: dataset.columnCount,
  duration: Date.now() - startTime,
  status: 'READY'
})
```

**Failure:**
```typescript
logger.error('Dataset upload failed', {
  userId: user.id,
  tenantId: user.tenantId,
  fileName: file.clientName,
  fileSize: file.size,
  error: error.message,
  errorStack: error.stack,
  duration: Date.now() - startTime
})
```

#### Parsing Logging

```typescript
logger.info('File parsing started', {
  datasetId: dataset.id,
  fileName: dataset.fileName,
  fileFormat: dataset.fileFormat
})

logger.info('File parsing completed', {
  datasetId: dataset.id,
  rowCount: result.rowCount,
  columnCount: result.columnCount,
  detectedTypes: result.columnMetadata.map(c => c.detectedType),
  parseDuration: result.duration
})
```

#### Rate Limiting Logging

```typescript
logger.warn('Rate limit exceeded', {
  userId: user.id,
  tenantId: user.tenantId,
  endpoint: '/datasets/upload',
  attemptCount: rateLimitData.count,
  limit: this.maxUploadsPerHour,
  resetAt: rateLimitData.resetAt,
  ipAddress: ctx.request.ip()
})
```

#### Security Event Logging

```typescript
// Authentication failures
logger.warn('Unauthorized dataset access attempt', {
  userId: user.id,
  tenantId: user.tenantId,
  datasetId: requestedDatasetId,
  ipAddress: ctx.request.ip(),
  userAgent: ctx.request.header('user-agent')
})

// File type validation failures
logger.warn('Invalid file type rejected', {
  userId: user.id,
  tenantId: user.tenantId,
  fileName: file.clientName,
  mimeType: file.type,
  detectedMimeType: detectedType?.mime,
  ipAddress: ctx.request.ip()
})

// Tenant isolation violations
logger.error('Tenant isolation violation detected', {
  userId: user.id,
  userTenantId: user.tenantId,
  requestedDatasetId: datasetId,
  datasetTenantId: dataset.tenantId,
  ipAddress: ctx.request.ip()
})
```

### Log Storage and Rotation

#### Production Log Configuration

**Location:**
```bash
/var/log/varlor/app.log
```

**Rotation configuration:**
```bash
# Install logrotate
sudo apt install logrotate

# Create logrotate config
sudo cat > /etc/logrotate.d/varlor <<EOF
/var/log/varlor/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 varlor-app varlor-app
    sharedscripts
    postrotate
        systemctl reload varlor-server > /dev/null 2>&1 || true
    endscript
}
EOF
```

**Log retention:**
- Development: 7 days
- Staging: 30 days
- Production: 90 days (compressed), 365 days (archived)

---

## Monitoring Metrics

### Key Metrics to Monitor

#### 1. Upload Metrics

**Metric**: `dataset_upload_total`
- Type: Counter
- Labels: status (success, failed), tenant_id, file_format
- Description: Total number of dataset uploads

**Metric**: `dataset_upload_duration_seconds`
- Type: Histogram
- Labels: status, file_format
- Description: Upload duration in seconds
- Buckets: [1, 5, 10, 30, 60, 120]

**Metric**: `dataset_upload_file_size_bytes`
- Type: Histogram
- Labels: file_format
- Description: Uploaded file size in bytes
- Buckets: [1MB, 10MB, 50MB, 100MB]

**Metric**: `dataset_upload_row_count`
- Type: Histogram
- Labels: file_format
- Description: Number of rows in uploaded file
- Buckets: [100, 1000, 10000, 100000, 500000]

#### 2. Parsing Metrics

**Metric**: `dataset_parse_duration_seconds`
- Type: Histogram
- Labels: file_format
- Description: Time to parse file
- Buckets: [0.1, 0.5, 1, 5, 10, 30]

**Metric**: `dataset_parse_errors_total`
- Type: Counter
- Labels: error_type (corrupted, unsupported_format, unrecognized_encoding)
- Description: Number of parsing errors

#### 3. Storage Metrics

**Metric**: `dataset_storage_disk_usage_bytes`
- Type: Gauge
- Labels: tenant_id
- Description: Disk space used by uploaded datasets

**Metric**: `dataset_storage_disk_available_bytes`
- Type: Gauge
- Description: Available disk space for dataset storage

#### 4. Rate Limiting Metrics

**Metric**: `dataset_upload_rate_limit_violations_total`
- Type: Counter
- Labels: user_id, tenant_id
- Description: Number of rate limit violations

#### 5. Error Metrics

**Metric**: `dataset_errors_total`
- Type: Counter
- Labels: error_type, endpoint
- Description: Total errors in dataset operations

### Prometheus Integration Example

```typescript
// server/app/services/metrics_service.ts
import { Counter, Histogram, Gauge, register } from 'prom-client'

export class MetricsService {
  private static uploadTotal = new Counter({
    name: 'dataset_upload_total',
    help: 'Total number of dataset uploads',
    labelNames: ['status', 'tenant_id', 'file_format']
  })

  private static uploadDuration = new Histogram({
    name: 'dataset_upload_duration_seconds',
    help: 'Upload duration in seconds',
    labelNames: ['status', 'file_format'],
    buckets: [1, 5, 10, 30, 60, 120]
  })

  private static uploadFileSize = new Histogram({
    name: 'dataset_upload_file_size_bytes',
    help: 'Uploaded file size in bytes',
    labelNames: ['file_format'],
    buckets: [1048576, 10485760, 52428800, 104857600] // 1MB, 10MB, 50MB, 100MB
  })

  private static storageDiskUsage = new Gauge({
    name: 'dataset_storage_disk_usage_bytes',
    help: 'Disk space used by uploaded datasets',
    labelNames: ['tenant_id']
  })

  public static recordUpload(
    status: 'success' | 'failed',
    tenantId: string,
    fileFormat: string,
    fileSize: number,
    duration: number
  ) {
    this.uploadTotal.inc({ status, tenant_id: tenantId, file_format: fileFormat })
    this.uploadDuration.observe({ status, file_format: fileFormat }, duration)
    this.uploadFileSize.observe({ file_format: fileFormat }, fileSize)
  }

  public static async updateStorageMetrics(tenantId: string, bytesUsed: number) {
    this.storageDiskUsage.set({ tenant_id: tenantId }, bytesUsed)
  }

  public static getMetrics() {
    return register.metrics()
  }
}
```

**Metrics endpoint:**
```typescript
// server/start/routes.ts
router.get('/metrics', async ({ response }) => {
  const metrics = await MetricsService.getMetrics()
  response.header('Content-Type', register.contentType)
  return metrics
})
```

### Grafana Dashboard Template

```json
{
  "dashboard": {
    "title": "Varlor Data Import Monitoring",
    "panels": [
      {
        "title": "Upload Success Rate",
        "targets": [
          {
            "expr": "rate(dataset_upload_total{status=\"success\"}[5m]) / rate(dataset_upload_total[5m]) * 100"
          }
        ]
      },
      {
        "title": "Upload Duration (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(dataset_upload_duration_seconds_bucket[5m]))"
          }
        ]
      },
      {
        "title": "Storage Disk Usage",
        "targets": [
          {
            "expr": "dataset_storage_disk_usage_bytes"
          }
        ]
      },
      {
        "title": "Rate Limit Violations",
        "targets": [
          {
            "expr": "rate(dataset_upload_rate_limit_violations_total[5m])"
          }
        ]
      }
    ]
  }
}
```

---

## Alert Configuration

### Critical Alerts

#### 1. Storage Disk Usage High

**Alert rule:**
```yaml
- alert: DatasetStorageDiskUsageHigh
  expr: (dataset_storage_disk_usage_bytes / dataset_storage_disk_available_bytes) > 0.80
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Dataset storage disk usage exceeds 80%"
    description: "Storage disk usage is at {{ $value | humanizePercentage }}. Consider expanding storage or cleaning up old datasets."
```

**Action:**
- Investigate disk usage
- Clean up old or unused datasets
- Expand storage capacity
- Notify DevOps team

#### 2. Upload Failure Rate High

**Alert rule:**
```yaml
- alert: DatasetUploadFailureRateHigh
  expr: (rate(dataset_upload_total{status="failed"}[5m]) / rate(dataset_upload_total[5m])) > 0.05
  for: 10m
  labels:
    severity: critical
  annotations:
    summary: "Dataset upload failure rate exceeds 5%"
    description: "{{ $value | humanizePercentage }} of uploads are failing. Check application logs for errors."
```

**Action:**
- Check application logs for errors
- Verify storage directory permissions
- Check database connectivity
- Investigate parsing service issues

#### 3. Upload Duration Slow

**Alert rule:**
```yaml
- alert: DatasetUploadDurationSlow
  expr: histogram_quantile(0.95, rate(dataset_upload_duration_seconds_bucket[5m])) > 30
  for: 15m
  labels:
    severity: warning
  annotations:
    summary: "Dataset upload duration (p95) exceeds 30 seconds"
    description: "95th percentile upload duration is {{ $value }}s. Performance degradation detected."
```

**Action:**
- Check disk I/O performance
- Check network throughput
- Review application resource usage (CPU, memory)
- Consider scaling infrastructure

#### 4. Rate Limit Violations High

**Alert rule:**
```yaml
- alert: DatasetRateLimitViolationsHigh
  expr: rate(dataset_upload_rate_limit_violations_total[1h]) > 100
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High number of rate limit violations"
    description: "{{ $value }} rate limit violations per hour. Potential abuse or legitimate high usage."
```

**Action:**
- Review rate limit logs
- Identify users/IPs with high violations
- Investigate for abuse or bot activity
- Consider adjusting rate limits if legitimate usage

### Alert Channels

**Configuration:**
```yaml
# AlertManager config
receivers:
  - name: 'devops-team'
    email_configs:
      - to: 'devops@varlor.com'
    slack_configs:
      - channel: '#alerts-production'
        api_url: 'https://hooks.slack.com/services/YOUR_WEBHOOK'

  - name: 'oncall'
    pagerduty_configs:
      - service_key: 'YOUR_PAGERDUTY_KEY'

route:
  receiver: 'devops-team'
  routes:
    - match:
        severity: critical
      receiver: 'oncall'
      continue: true
```

---

## Log Analysis

### Useful Log Queries

#### Find Failed Uploads

```bash
# Last 100 failed uploads
grep "Dataset upload failed" /var/log/varlor/app.log | tail -100

# Failed uploads by error type
grep "Dataset upload failed" /var/log/varlor/app.log | \
  jq -r '.error' | sort | uniq -c | sort -rn
```

#### Analyze Upload Performance

```bash
# Average upload duration
grep "Dataset upload completed" /var/log/varlor/app.log | \
  jq -r '.duration' | awk '{sum+=$1; count++} END {print sum/count}'

# Uploads by file format
grep "Dataset upload completed" /var/log/varlor/app.log | \
  jq -r '.fileFormat' | sort | uniq -c | sort -rn
```

#### Detect Security Issues

```bash
# Rate limit violations
grep "Rate limit exceeded" /var/log/varlor/app.log | \
  jq -r '.userId' | sort | uniq -c | sort -rn

# Unauthorized access attempts
grep "Unauthorized dataset access attempt" /var/log/varlor/app.log | \
  jq -r '.ipAddress' | sort | uniq -c | sort -rn

# Invalid file type rejections
grep "Invalid file type rejected" /var/log/varlor/app.log | \
  jq -r '.detectedMimeType' | sort | uniq -c | sort -rn
```

#### Track User Activity

```bash
# Uploads by user
grep "Dataset upload completed" /var/log/varlor/app.log | \
  jq -r '.userId' | sort | uniq -c | sort -rn

# Uploads by tenant
grep "Dataset upload completed" /var/log/varlor/app.log | \
  jq -r '.tenantId' | sort | uniq -c | sort -rn
```

### ELK Stack Integration

**Logstash pipeline configuration:**
```conf
input {
  file {
    path => "/var/log/varlor/app.log"
    start_position => "beginning"
    codec => json
  }
}

filter {
  if [msg] =~ "Dataset" {
    mutate {
      add_tag => ["dataset_operation"]
    }
  }

  if [level] == "error" {
    mutate {
      add_tag => ["error"]
    }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "varlor-logs-%{+YYYY.MM.dd}"
  }
}
```

**Elasticsearch query examples:**
```json
// Failed uploads in last 24 hours
GET varlor-logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "msg": "Dataset upload failed" } },
        { "range": { "@timestamp": { "gte": "now-24h" } } }
      ]
    }
  }
}

// Top error types
GET varlor-logs-*/_search
{
  "size": 0,
  "query": {
    "match": { "level": "error" }
  },
  "aggs": {
    "error_types": {
      "terms": {
        "field": "error.keyword",
        "size": 10
      }
    }
  }
}
```

---

## Performance Monitoring

### Key Performance Indicators (KPIs)

1. **Upload Success Rate**: Target > 95%
2. **Upload Duration (p95)**: Target < 30 seconds
3. **Parse Duration (p95)**: Target < 5 seconds
4. **Error Rate**: Target < 5%
5. **Rate Limit Hit Rate**: Target < 1% of requests

### Performance Baselines

**File size benchmarks:**
- 1 MB file: Upload < 2s, Parse < 0.5s
- 10 MB file: Upload < 5s, Parse < 2s
- 50 MB file: Upload < 15s, Parse < 5s
- 100 MB file: Upload < 30s, Parse < 10s

**Row count benchmarks:**
- 1,000 rows: Parse < 0.5s
- 10,000 rows: Parse < 1s
- 100,000 rows: Parse < 3s
- 500,000 rows: Parse < 10s

### Performance Degradation Detection

**Monitor these trends:**
```bash
# Upload duration over time
SELECT
  DATE_TRUNC('hour', timestamp) as hour,
  AVG(duration) as avg_duration,
  PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY duration) as p95_duration
FROM dataset_upload_logs
WHERE timestamp > NOW() - INTERVAL '7 days'
GROUP BY hour
ORDER BY hour;

# Parse duration by file format
SELECT
  file_format,
  AVG(parse_duration) as avg_parse,
  MAX(parse_duration) as max_parse
FROM dataset_upload_logs
WHERE timestamp > NOW() - INTERVAL '1 day'
GROUP BY file_format;
```

---

## Troubleshooting with Logs

### Common Issues and Log Patterns

#### Issue: High Upload Failures

**Log pattern:**
```json
{
  "level": "error",
  "msg": "Dataset upload failed",
  "error": "ENOSPC: no space left on device"
}
```

**Resolution:**
- Check disk space: `df -h /var/www/varlor/storage`
- Clean up old datasets or expand storage

#### Issue: Slow Parsing

**Log pattern:**
```json
{
  "level": "info",
  "msg": "File parsing completed",
  "parseDuration": 45000  // 45 seconds
}
```

**Resolution:**
- Check CPU usage: `top`
- Check disk I/O: `iostat -x 1 10`
- Consider optimizing parser or scaling infrastructure

#### Issue: Rate Limiting Too Aggressive

**Log pattern:**
```json
{
  "level": "warn",
  "msg": "Rate limit exceeded",
  "userId": 123,
  "attemptCount": 11
}
```

**Resolution:**
- Review user's upload pattern
- Increase `UPLOAD_RATE_LIMIT` if legitimate usage
- Contact user if potential abuse

---

## Maintenance Tasks

### Daily

- [ ] Check error rate in last 24 hours
- [ ] Review rate limit violations
- [ ] Check disk usage alerts

### Weekly

- [ ] Review performance trends
- [ ] Analyze top error types
- [ ] Review slow uploads (> 30s)
- [ ] Check log rotation working correctly

### Monthly

- [ ] Review and update alert thresholds
- [ ] Analyze storage growth trends
- [ ] Review security event logs
- [ ] Update monitoring dashboards

---

**Document Version**: 1.0
**Last Updated**: 2025-11-23
**Maintained By**: DevOps Team
