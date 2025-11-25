# Data Import Feature - Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the data import feature to production, including database migrations, environment configuration, storage setup, monitoring, and security considerations.

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Database Migration Plan](#database-migration-plan)
3. [Environment Configuration](#environment-configuration)
4. [Storage Configuration](#storage-configuration)
5. [Deployment Steps](#deployment-steps)
6. [Post-Deployment Verification](#post-deployment-verification)
7. [Monitoring and Alerts](#monitoring-and-alerts)
8. [Rollback Procedures](#rollback-procedures)
9. [Troubleshooting](#troubleshooting)

---

## Pre-Deployment Checklist

### Infrastructure Requirements

- [ ] PostgreSQL 14+ database server
- [ ] Node.js 20+ runtime
- [ ] Minimum 10GB disk space for file storage
- [ ] Reverse proxy/load balancer with HTTPS configured
- [ ] SSL/TLS certificates installed and valid

### Security Requirements

- [ ] All environment secrets generated and secured
- [ ] Database SSL/TLS enabled
- [ ] CORS configured for production domain only
- [ ] Rate limiting tested and configured
- [ ] File upload security audit completed

### Testing Requirements

- [ ] All feature tests passing on staging environment
- [ ] Database migrations tested on staging database
- [ ] Large file uploads tested (up to 100MB)
- [ ] Performance testing completed (500k row files)
- [ ] Security vulnerabilities addressed

---

## Database Migration Plan

### Migrations to Execute

The data import feature requires the following database migrations:

1. **Create datasets table** - `TIMESTAMP_create_datasets_table.ts`
2. **Create dataset_columns table** - `TIMESTAMP_create_dataset_columns_table.ts`

### Migration Execution Steps

#### On Staging Environment (Pre-Production Testing)

```bash
# 1. Backup staging database
pg_dump -h staging-db-host -U varlor_user varlor > backup_staging_$(date +%Y%m%d_%H%M%S).sql

# 2. Run migrations on staging
cd /path/to/varlor/server
NODE_ENV=staging node ace migration:run

# 3. Verify tables created
psql -h staging-db-host -U varlor_user -d varlor -c "\dt"
psql -h staging-db-host -U varlor_user -d varlor -c "\d datasets"
psql -h staging-db-host -U varlor_user -d varlor -c "\d dataset_columns"

# 4. Test the feature on staging
# Upload test files, verify data integrity

# 5. Test rollback (optional but recommended)
NODE_ENV=staging node ace migration:rollback
```

#### On Production Environment

```bash
# 1. Create backup with verification
pg_dump -h production-db-host -U varlor_user varlor > backup_production_$(date +%Y%m%d_%H%M%S).sql
gzip backup_production_*.sql
# Verify backup file is not empty
ls -lh backup_production_*.sql.gz

# 2. Put application in maintenance mode (optional)
# This prevents data corruption during migration
# Configure your reverse proxy to show maintenance page

# 3. Run migrations
cd /path/to/varlor/server
NODE_ENV=production node ace migration:run

# 4. Verify migrations succeeded
psql -h production-db-host -U varlor_user -d varlor -c "\dt"
psql -h production-db-host -U varlor_user -d varlor -c "SELECT COUNT(*) FROM datasets"
psql -h production-db-host -U varlor_user -d varlor -c "SELECT COUNT(*) FROM dataset_columns"

# 5. Check foreign key constraints
psql -h production-db-host -U varlor_user -d varlor -c "
  SELECT conname, conrelid::regclass, confrelid::regclass
  FROM pg_constraint
  WHERE conrelid IN ('datasets'::regclass, 'dataset_columns'::regclass)
  AND contype = 'f';
"

# 6. Remove maintenance mode
```

### Migration Rollback Plan

If issues are discovered after migration:

```bash
# 1. Put application in maintenance mode

# 2. Rollback migrations (only if no data has been created)
NODE_ENV=production node ace migration:rollback --batch=1

# 3. Restore database from backup (if data exists or corruption occurred)
gunzip backup_production_*.sql.gz
psql -h production-db-host -U varlor_user -d varlor < backup_production_*.sql

# 4. Verify database restored correctly
psql -h production-db-host -U varlor_user -d varlor -c "\dt"

# 5. Remove maintenance mode and investigate issues
```

### Migration Verification

After running migrations, verify:

```sql
-- Verify datasets table structure
\d datasets

-- Expected columns:
-- id, tenant_id, user_id, name, file_name, file_size, file_format,
-- storage_path, row_count, column_count, status, error_message,
-- uploaded_at, processed_at, created_at, updated_at

-- Verify dataset_columns table structure
\d dataset_columns

-- Expected columns:
-- id, dataset_id, column_name, column_index, detected_type,
-- sample_values, created_at, updated_at

-- Verify indexes created
\di datasets*
\di dataset_columns*

-- Verify foreign key constraints
SELECT
  tc.constraint_name,
  tc.table_name,
  kcu.column_name,
  ccu.table_name AS foreign_table_name,
  ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name IN ('datasets', 'dataset_columns');
```

---

## Environment Configuration

### Production Environment Variables

Create `/var/www/varlor/server/.env.production` based on the template:

```bash
# Copy template
cp /path/to/varlor/server/.env.production.template /var/www/varlor/server/.env.production

# Set file permissions (readable only by application user)
chmod 600 /var/www/varlor/server/.env.production
chown varlor-app:varlor-app /var/www/varlor/server/.env.production

# Edit and fill in all values
vim /var/www/varlor/server/.env.production
```

### Critical Configuration Values

```bash
# Application
NODE_ENV=production
PORT=3333
HOST=0.0.0.0
LOG_LEVEL=warn

# Database (use production credentials)
DB_HOST=production-db-host
DB_PORT=5432
DB_USER=varlor_prod
DB_PASSWORD=secure-password
DB_DATABASE=varlor

# CORS (match production domain exactly)
CORS_ORIGIN=https://app.varlor.com
CORS_CREDENTIALS=true

# Storage (use absolute path)
STORAGE_ROOT_PATH=/var/www/varlor/storage

# Dataset limits
MAX_FILE_SIZE=104857600  # 100MB
MAX_ROW_COUNT=500000     # 500k rows
UPLOAD_RATE_LIMIT=10     # uploads per hour per user

# Secrets (generate new keys for production)
APP_KEY=$(node ace generate:key)
ACCESS_TOKEN_SECRET=$(node ace generate:key)
```

### Environment Variable Validation

Verify all required environment variables are set:

```bash
# Validate configuration
node ace env:validate

# Test database connection
node ace db:check

# Verify storage path exists and is writable
test -d "$STORAGE_ROOT_PATH" && echo "Storage path exists" || echo "Storage path missing"
test -w "$STORAGE_ROOT_PATH" && echo "Storage path writable" || echo "Storage path not writable"
```

---

## Storage Configuration

### Create Storage Directory Structure

```bash
# Create storage root directory
sudo mkdir -p /var/www/varlor/storage/datasets

# Set ownership to application user
sudo chown -R varlor-app:varlor-app /var/www/varlor/storage

# Set permissions (owner: read/write/execute, group: read/execute, others: none)
sudo chmod -R 750 /var/www/varlor/storage

# Verify permissions
ls -la /var/www/varlor/storage
```

### Storage Permissions

- **Owner**: Application user (e.g., `varlor-app`)
- **Group**: Application group (e.g., `varlor-app`)
- **Permissions**: `750` (rwxr-x---)
  - Owner can read, write, and execute
  - Group can read and execute
  - Others have no access

### Exclude Storage from Version Control

Verify `.gitignore` includes storage directory:

```bash
# Check .gitignore
grep -q "^storage/" /path/to/varlor/server/.gitignore || echo "storage/" >> /path/to/varlor/server/.gitignore
```

### Storage Backup Strategy

Configure automated backups:

```bash
# Create backup script
cat > /usr/local/bin/varlor-storage-backup.sh <<'EOF'
#!/bin/bash
STORAGE_PATH="/var/www/varlor/storage"
BACKUP_PATH="/var/backups/varlor/storage"
DATE=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p "$BACKUP_PATH"

# Create incremental backup with rsync
rsync -av --link-dest="$BACKUP_PATH/latest" \
  "$STORAGE_PATH/" "$BACKUP_PATH/backup_$DATE/"

# Update 'latest' symlink
rm -f "$BACKUP_PATH/latest"
ln -s "$BACKUP_PATH/backup_$DATE" "$BACKUP_PATH/latest"

# Delete backups older than 30 days
find "$BACKUP_PATH" -maxdepth 1 -type d -name "backup_*" -mtime +30 -exec rm -rf {} \;

echo "Backup completed: $BACKUP_PATH/backup_$DATE"
EOF

# Make script executable
chmod +x /usr/local/bin/varlor-storage-backup.sh

# Add to crontab (daily at 2 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * /usr/local/bin/varlor-storage-backup.sh") | crontab -
```

### Disk Space Monitoring

```bash
# Add disk usage monitoring
cat > /usr/local/bin/varlor-storage-monitor.sh <<'EOF'
#!/bin/bash
STORAGE_PATH="/var/www/varlor/storage"
THRESHOLD=80  # Alert at 80% usage

USAGE=$(df -h "$STORAGE_PATH" | awk 'NR==2 {print $5}' | sed 's/%//')

if [ "$USAGE" -gt "$THRESHOLD" ]; then
  echo "ALERT: Storage usage is at ${USAGE}% (threshold: ${THRESHOLD}%)"
  # Send alert (configure your alerting system here)
  # Example: curl -X POST https://your-alert-endpoint -d "message=Storage usage high: ${USAGE}%"
fi
EOF

chmod +x /usr/local/bin/varlor-storage-monitor.sh

# Run every hour
(crontab -l 2>/dev/null; echo "0 * * * * /usr/local/bin/varlor-storage-monitor.sh") | crontab -
```

---

## Deployment Steps

### Step 1: Prepare Deployment Package

```bash
# On development/CI machine

# 1. Build backend
cd /path/to/varlor/server
npm ci --production
npm run build

# 2. Build frontend
cd /path/to/varlor/client/web
npm ci --production
npm run build

# 3. Create deployment archive
cd /path/to/varlor
tar -czf varlor-deploy-$(date +%Y%m%d_%H%M%S).tar.gz \
  --exclude=node_modules \
  --exclude=.git \
  --exclude=storage \
  --exclude=tmp \
  server/ client/

# 4. Transfer to production server
scp varlor-deploy-*.tar.gz user@production-server:/tmp/
```

### Step 2: Deploy on Production Server

```bash
# On production server

# 1. Create backup of current deployment
cd /var/www/varlor
tar -czf ../varlor-backup-$(date +%Y%m%d_%H%M%S).tar.gz .

# 2. Extract new deployment
cd /var/www/varlor
tar -xzf /tmp/varlor-deploy-*.tar.gz

# 3. Install dependencies
cd /var/www/varlor/server
npm ci --production

cd /var/www/varlor/client/web
npm ci --production

# 4. Build if not pre-built
cd /var/www/varlor/server
npm run build

# 5. Set ownership
sudo chown -R varlor-app:varlor-app /var/www/varlor

# 6. Verify environment configuration
cat /var/www/varlor/server/.env.production
node ace env:validate
```

### Step 3: Run Database Migrations

```bash
# See "Database Migration Plan" section above
cd /var/www/varlor/server
NODE_ENV=production node ace migration:run
```

### Step 4: Restart Application Services

```bash
# Using systemd (example)
sudo systemctl restart varlor-server
sudo systemctl restart varlor-client

# Or using PM2
pm2 restart varlor-server
pm2 restart varlor-client

# Verify services started successfully
sudo systemctl status varlor-server
sudo systemctl status varlor-client

# Or with PM2
pm2 status
pm2 logs varlor-server --lines 50
```

### Step 5: Verify Health Checks

```bash
# Check application is responding
curl -f https://api.varlor.com/health || echo "Backend health check failed"
curl -f https://app.varlor.com/ || echo "Frontend health check failed"

# Check database connectivity
curl -f https://api.varlor.com/health/db || echo "Database connectivity failed"

# Check storage directory
test -d /var/www/varlor/storage && echo "Storage directory exists" || echo "Storage directory missing"
```

---

## Post-Deployment Verification

### Functional Testing

```bash
# 1. Test authentication
curl -X POST https://api.varlor.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"testpassword"}'

# 2. Test file upload (with authentication token)
curl -X POST https://api.varlor.com/datasets/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test.csv"

# 3. Test dataset retrieval
curl -X GET https://api.varlor.com/datasets/1 \
  -H "Authorization: Bearer YOUR_TOKEN"

# 4. Test preview endpoint
curl -X GET https://api.varlor.com/datasets/1/preview \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Database Verification

```sql
-- Check datasets table
SELECT COUNT(*) FROM datasets;

-- Check dataset_columns table
SELECT COUNT(*) FROM dataset_columns;

-- Verify indexes
SELECT indexname, indexdef FROM pg_indexes WHERE tablename IN ('datasets', 'dataset_columns');

-- Verify foreign key constraints
SELECT conname, contype FROM pg_constraint WHERE conrelid IN ('datasets'::regclass, 'dataset_columns'::regclass);
```

### Storage Verification

```bash
# Verify storage directory permissions
ls -la /var/www/varlor/storage/datasets

# Verify application can write to storage
sudo -u varlor-app touch /var/www/varlor/storage/datasets/.test
sudo -u varlor-app rm /var/www/varlor/storage/datasets/.test

# Check disk space
df -h /var/www/varlor/storage
```

### Logging Verification

```bash
# Check application logs
tail -f /var/www/varlor/server/logs/app.log

# Check for errors
grep -i error /var/www/varlor/server/logs/app.log | tail -20

# Check dataset operation logs
grep "dataset" /var/www/varlor/server/logs/app.log | tail -20
```

---

## Monitoring and Alerts

### Disk Usage Monitoring

Monitor storage directory disk usage:

```bash
# Alert when storage exceeds 80% capacity
watch -n 300 'df -h /var/www/varlor/storage | awk "NR==2 {if (\$5+0 > 80) print \"ALERT: Storage at \" \$5}"'
```

### Response Time Monitoring

Monitor upload endpoint response times:

```bash
# Test upload endpoint performance
time curl -X POST https://api.varlor.com/datasets/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@large_test.csv"

# Alert if response time exceeds 30 seconds for 100MB file
```

### Error Rate Monitoring

Monitor dataset operation errors:

```bash
# Count errors in last hour
grep -c "ERROR.*dataset" /var/www/varlor/server/logs/app.log

# Alert if error rate exceeds 5%
ERROR_COUNT=$(grep -c "ERROR.*dataset" /var/www/varlor/server/logs/app.log)
TOTAL_COUNT=$(grep -c "dataset" /var/www/varlor/server/logs/app.log)
ERROR_RATE=$((ERROR_COUNT * 100 / TOTAL_COUNT))
if [ "$ERROR_RATE" -gt 5 ]; then
  echo "ALERT: Dataset error rate is ${ERROR_RATE}%"
fi
```

### Rate Limiting Monitoring

Monitor rate limit violations:

```bash
# Check rate limit violations
grep "Too many upload requests" /var/www/varlor/server/logs/app.log | tail -20

# Count violations per user
grep "Too many upload requests" /var/www/varlor/server/logs/app.log | \
  grep -oP 'userId=\K[0-9]+' | sort | uniq -c | sort -rn
```

### Recommended Monitoring Stack

1. **Prometheus**: Metrics collection
2. **Grafana**: Visualization and alerting
3. **Loki**: Log aggregation
4. **AlertManager**: Alert routing and management

Example Prometheus metrics to collect:

```yaml
- dataset_upload_duration_seconds
- dataset_upload_file_size_bytes
- dataset_upload_row_count
- dataset_parse_duration_seconds
- dataset_storage_disk_usage_bytes
- dataset_upload_rate_limit_violations_total
- dataset_upload_errors_total
```

---

## Rollback Procedures

### Scenario 1: Application Issues (No Data Corruption)

```bash
# 1. Stop application services
sudo systemctl stop varlor-server varlor-client

# 2. Restore previous deployment
cd /var/www/varlor
rm -rf server/ client/
tar -xzf ../varlor-backup-TIMESTAMP.tar.gz

# 3. Restart services
sudo systemctl start varlor-server varlor-client

# 4. Verify health checks
curl -f https://api.varlor.com/health
```

### Scenario 2: Database Migration Issues (Before Data Creation)

```bash
# 1. Put application in maintenance mode

# 2. Rollback migrations
cd /var/www/varlor/server
NODE_ENV=production node ace migration:rollback --batch=1

# 3. Verify rollback
psql -h production-db-host -U varlor_user -d varlor -c "\dt"

# 4. Restore previous application version (if needed)
# See Scenario 1

# 5. Remove maintenance mode
```

### Scenario 3: Database Corruption (After Data Creation)

```bash
# 1. Put application in maintenance mode immediately

# 2. Stop application services
sudo systemctl stop varlor-server varlor-client

# 3. Restore database from backup
gunzip backup_production_TIMESTAMP.sql.gz
psql -h production-db-host -U varlor_user -d varlor < backup_production_TIMESTAMP.sql

# 4. Verify database restored
psql -h production-db-host -U varlor_user -d varlor -c "SELECT COUNT(*) FROM datasets"

# 5. Restore previous application version
cd /var/www/varlor
rm -rf server/ client/
tar -xzf ../varlor-backup-TIMESTAMP.tar.gz

# 6. Restart services
sudo systemctl start varlor-server varlor-client

# 7. Remove maintenance mode

# 8. Investigate root cause before re-deploying
```

### Scenario 4: Storage Issues

```bash
# 1. Restore storage from backup
rsync -av /var/backups/varlor/storage/latest/ /var/www/varlor/storage/

# 2. Verify permissions
sudo chown -R varlor-app:varlor-app /var/www/varlor/storage
sudo chmod -R 750 /var/www/varlor/storage

# 3. Restart services
sudo systemctl restart varlor-server
```

---

## Troubleshooting

### Issue: Database Migration Fails

**Symptoms**: Migration command returns error

**Diagnosis**:
```bash
# Check database connectivity
psql -h production-db-host -U varlor_user -d varlor -c "SELECT 1"

# Check migration status
node ace migration:status

# Check database logs
sudo tail -f /var/log/postgresql/postgresql-*.log
```

**Solutions**:
1. Verify database credentials in `.env.production`
2. Ensure database user has CREATE TABLE permissions
3. Check for conflicting table names
4. Review migration files for syntax errors

### Issue: File Upload Fails with 413 Error

**Symptoms**: Upload returns "Payload Too Large"

**Diagnosis**:
```bash
# Check nginx/Apache config for client_max_body_size
grep -r "client_max_body_size" /etc/nginx/
grep -r "LimitRequestBody" /etc/apache2/
```

**Solutions**:
1. Update reverse proxy configuration:
   ```nginx
   # Nginx
   client_max_body_size 100M;
   ```
2. Verify `MAX_FILE_SIZE` in `.env.production`
3. Check bodyparser config in `config/bodyparser.ts`

### Issue: Storage Directory Permission Denied

**Symptoms**: Upload fails with "EACCES: permission denied"

**Diagnosis**:
```bash
# Check storage directory permissions
ls -la /var/www/varlor/storage

# Check application user
ps aux | grep node | grep varlor

# Test write access
sudo -u varlor-app touch /var/www/varlor/storage/test
```

**Solutions**:
```bash
# Fix ownership
sudo chown -R varlor-app:varlor-app /var/www/varlor/storage

# Fix permissions
sudo chmod -R 750 /var/www/varlor/storage
```

### Issue: Rate Limiting Too Aggressive

**Symptoms**: Legitimate users hitting rate limits

**Diagnosis**:
```bash
# Check rate limit violations
grep "Too many upload requests" /var/www/varlor/server/logs/app.log | wc -l

# Analyze violation patterns
grep "Too many upload requests" /var/www/varlor/server/logs/app.log | \
  awk '{print $1, $2}' | uniq -c
```

**Solutions**:
1. Increase `UPLOAD_RATE_LIMIT` in `.env.production`
2. Implement Redis-based distributed rate limiting
3. Consider implementing per-tenant rate limits

### Issue: Slow Upload Performance

**Symptoms**: Uploads take longer than 30 seconds for 100MB files

**Diagnosis**:
```bash
# Check disk I/O
iostat -x 1 10

# Check network throughput
iftop

# Check CPU and memory
top

# Profile application
node --prof /var/www/varlor/server/build/bin/server.js
```

**Solutions**:
1. Optimize disk I/O (use SSD, RAID configuration)
2. Increase network bandwidth
3. Implement streaming upload (already done)
4. Consider CDN for upload endpoint

### Issue: Database Connection Pool Exhausted

**Symptoms**: "Connection pool exhausted" errors in logs

**Diagnosis**:
```bash
# Check active connections
psql -h production-db-host -U varlor_user -d varlor -c "SELECT count(*) FROM pg_stat_activity"

# Check connection limits
psql -h production-db-host -U varlor_user -d varlor -c "SHOW max_connections"
```

**Solutions**:
1. Increase database connection pool size in `config/database.ts`
2. Increase database `max_connections` setting
3. Investigate connection leaks in application code

---

## Security Considerations

### File Upload Security Checklist

- [x] File type validation (MIME type + extension)
- [x] File size limits enforced on backend
- [x] Filename sanitization prevents path traversal
- [x] Tenant isolation prevents unauthorized access
- [x] Authentication required for all dataset endpoints
- [x] Rate limiting prevents abuse
- [ ] Virus scanning for uploaded files (recommended)
- [ ] Content Security Policy headers configured

### Additional Security Measures

```bash
# Enable firewall rules
sudo ufw allow 443/tcp
sudo ufw allow 80/tcp
sudo ufw deny 5432/tcp  # Block external database access

# Restrict SSH access
sudo vim /etc/ssh/sshd_config
# Set: PermitRootLogin no
# Set: PasswordAuthentication no

# Keep system updated
sudo apt update && sudo apt upgrade -y

# Enable automatic security updates
sudo apt install unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

---

## Support and Escalation

### Contact Information

- **DevOps Team**: devops@varlor.com
- **Backend Team**: backend@varlor.com
- **On-Call Engineer**: oncall@varlor.com
- **Incident Management**: incidents@varlor.com

### Escalation Procedure

1. **Level 1**: Check this documentation and troubleshooting section
2. **Level 2**: Contact DevOps team via Slack/email
3. **Level 3**: Escalate to on-call engineer for critical issues
4. **Level 4**: Page engineering leadership for service outages

### Incident Response

For production incidents:

1. Assess severity (P0 = service down, P1 = degraded, P2 = minor issue)
2. Create incident in tracking system
3. Follow appropriate escalation path
4. Document timeline and actions taken
5. Conduct post-mortem after resolution

---

## Appendix

### Useful Commands Reference

```bash
# Check application health
curl -f https://api.varlor.com/health

# Check database connection
psql -h production-db-host -U varlor_user -d varlor -c "SELECT 1"

# View application logs
tail -f /var/www/varlor/server/logs/app.log

# Restart services
sudo systemctl restart varlor-server varlor-client

# Check disk space
df -h /var/www/varlor/storage

# Check service status
sudo systemctl status varlor-server varlor-client

# Run database migrations
cd /var/www/varlor/server && NODE_ENV=production node ace migration:run

# Rollback migrations
cd /var/www/varlor/server && NODE_ENV=production node ace migration:rollback

# Check migration status
cd /var/www/varlor/server && node ace migration:status
```

### Database Backup Commands

```bash
# Manual backup
pg_dump -h production-db-host -U varlor_user varlor > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore backup
psql -h production-db-host -U varlor_user -d varlor < backup_TIMESTAMP.sql

# Backup with compression
pg_dump -h production-db-host -U varlor_user varlor | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz

# Restore compressed backup
gunzip < backup_TIMESTAMP.sql.gz | psql -h production-db-host -U varlor_user -d varlor
```

---

**Document Version**: 1.0
**Last Updated**: 2025-11-23
**Maintained By**: DevOps Team
