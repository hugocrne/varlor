# Data Import - Backup and Recovery Guide

## Overview

This guide outlines the backup strategy, recovery procedures, and disaster recovery planning for the data import feature. It ensures uploaded datasets can be recovered in case of hardware failure, data corruption, or accidental deletion.

## Table of Contents

1. [Backup Strategy](#backup-strategy)
2. [Automated Backup Setup](#automated-backup-setup)
3. [Manual Backup Procedures](#manual-backup-procedures)
4. [Recovery Procedures](#recovery-procedures)
5. [Disaster Recovery](#disaster-recovery)
6. [Testing and Validation](#testing-and-validation)

---

## Backup Strategy

### What Gets Backed Up

#### 1. Database (Datasets Metadata)

**Tables:**
- `datasets` - Dataset metadata
- `dataset_columns` - Column metadata

**Backup method:**
- PostgreSQL dumps using `pg_dump`
- Frequency: Daily full backups, hourly incremental backups
- Retention: 30 days online, 90 days archived

#### 2. File Storage (Uploaded Files)

**Location:**
- `/var/www/varlor/storage/datasets/`

**Backup method:**
- Rsync incremental backups
- Frequency: Daily
- Retention: 30 days

#### 3. Application Logs

**Location:**
- `/var/log/varlor/`

**Backup method:**
- Log rotation with archival
- Retention: 90 days compressed, 365 days archived

### Backup Locations

**Primary backup location:**
- Local backup server: `/var/backups/varlor/`

**Secondary backup location (offsite):**
- Cloud storage (S3, Google Cloud Storage, or Azure Blob Storage)
- Encrypted backups for security
- Geo-redundant storage

### Retention Policy

| Data Type | Daily | Weekly | Monthly | Yearly |
|-----------|-------|--------|---------|--------|
| Database | 30 days | 12 weeks | 24 months | 5 years |
| File Storage | 30 days | 12 weeks | 6 months | 2 years |
| Application Logs | 90 days | N/A | N/A | N/A |

---

## Automated Backup Setup

### 1. Database Backup Script

```bash
#!/bin/bash
# /usr/local/bin/varlor-db-backup.sh

set -e  # Exit on error

# Configuration
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-varlor_prod}"
DB_NAME="${DB_NAME:-varlor}"
BACKUP_DIR="/var/backups/varlor/database"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Backup filename
BACKUP_FILE="$BACKUP_DIR/varlor_db_$DATE.sql"

echo "Starting database backup: $BACKUP_FILE"

# Perform backup
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  --format=custom \
  --compress=9 \
  --file="$BACKUP_FILE"

# Verify backup is not empty
if [ ! -s "$BACKUP_FILE" ]; then
  echo "ERROR: Backup file is empty!"
  exit 1
fi

echo "Database backup completed successfully"

# Compress backup (if not already compressed by pg_dump)
if [[ ! "$BACKUP_FILE" == *.gz ]]; then
  gzip "$BACKUP_FILE"
  BACKUP_FILE="${BACKUP_FILE}.gz"
fi

# Calculate checksum
sha256sum "$BACKUP_FILE" > "${BACKUP_FILE}.sha256"

# Delete old backups
find "$BACKUP_DIR" -name "varlor_db_*.sql*" -mtime +$RETENTION_DAYS -delete

echo "Backup file: $BACKUP_FILE"
echo "Checksum: $(cat ${BACKUP_FILE}.sha256)"

# Upload to offsite storage (optional)
# aws s3 cp "$BACKUP_FILE" "s3://varlor-backups/database/"
# aws s3 cp "${BACKUP_FILE}.sha256" "s3://varlor-backups/database/"

echo "Database backup process completed"
```

**Make script executable:**
```bash
sudo chmod +x /usr/local/bin/varlor-db-backup.sh
```

**Add to crontab (daily at 2 AM):**
```bash
sudo crontab -e
# Add:
0 2 * * * /usr/local/bin/varlor-db-backup.sh >> /var/log/varlor/backup.log 2>&1
```

### 2. File Storage Backup Script

```bash
#!/bin/bash
# /usr/local/bin/varlor-storage-backup.sh

set -e  # Exit on error

# Configuration
STORAGE_PATH="/var/www/varlor/storage/datasets"
BACKUP_PATH="/var/backups/varlor/storage"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Create backup directory
mkdir -p "$BACKUP_PATH"

echo "Starting storage backup: $BACKUP_PATH/backup_$DATE"

# Incremental backup using rsync with hard links
rsync -av \
  --delete \
  --link-dest="$BACKUP_PATH/latest" \
  "$STORAGE_PATH/" \
  "$BACKUP_PATH/backup_$DATE/"

# Verify backup completed
if [ $? -eq 0 ]; then
  echo "Storage backup completed successfully"

  # Update 'latest' symlink
  rm -f "$BACKUP_PATH/latest"
  ln -s "$BACKUP_PATH/backup_$DATE" "$BACKUP_PATH/latest"

  # Calculate total size
  TOTAL_SIZE=$(du -sh "$BACKUP_PATH/backup_$DATE" | cut -f1)
  echo "Backup size: $TOTAL_SIZE"
else
  echo "ERROR: Storage backup failed!"
  exit 1
fi

# Delete old backups
find "$BACKUP_PATH" -maxdepth 1 -type d -name "backup_*" -mtime +$RETENTION_DAYS -exec rm -rf {} \;

echo "Deleted backups older than $RETENTION_DAYS days"

# Upload to offsite storage (optional)
# aws s3 sync "$BACKUP_PATH/backup_$DATE/" "s3://varlor-backups/storage/backup_$DATE/" --delete

echo "Storage backup process completed"
```

**Make script executable:**
```bash
sudo chmod +x /usr/local/bin/varlor-storage-backup.sh
```

**Add to crontab (daily at 3 AM):**
```bash
sudo crontab -e
# Add:
0 3 * * * /usr/local/bin/varlor-storage-backup.sh >> /var/log/varlor/backup.log 2>&1
```

### 3. Backup Verification Script

```bash
#!/bin/bash
# /usr/local/bin/varlor-backup-verify.sh

set -e

# Configuration
DB_BACKUP_DIR="/var/backups/varlor/database"
STORAGE_BACKUP_DIR="/var/backups/varlor/storage"

echo "Verifying backups..."

# Check database backup exists and is recent
LATEST_DB_BACKUP=$(find "$DB_BACKUP_DIR" -name "varlor_db_*.sql*" -type f -mtime -1 | sort -r | head -1)
if [ -z "$LATEST_DB_BACKUP" ]; then
  echo "ERROR: No database backup found from last 24 hours!"
  exit 1
fi
echo "Latest database backup: $LATEST_DB_BACKUP"

# Verify database backup checksum
CHECKSUM_FILE="${LATEST_DB_BACKUP}.sha256"
if [ -f "$CHECKSUM_FILE" ]; then
  sha256sum -c "$CHECKSUM_FILE"
  echo "Database backup checksum verified"
else
  echo "WARNING: No checksum file found for database backup"
fi

# Check storage backup exists and is recent
if [ ! -L "$STORAGE_BACKUP_DIR/latest" ]; then
  echo "ERROR: No storage backup found!"
  exit 1
fi
LATEST_STORAGE_BACKUP=$(readlink -f "$STORAGE_BACKUP_DIR/latest")
BACKUP_AGE=$(find "$LATEST_STORAGE_BACKUP" -maxdepth 0 -mtime -1)
if [ -z "$BACKUP_AGE" ]; then
  echo "ERROR: Storage backup is older than 24 hours!"
  exit 1
fi
echo "Latest storage backup: $LATEST_STORAGE_BACKUP"

# Check backup sizes
DB_SIZE=$(stat -c%s "$LATEST_DB_BACKUP" 2>/dev/null || stat -f%z "$LATEST_DB_BACKUP")
STORAGE_SIZE=$(du -sb "$LATEST_STORAGE_BACKUP" | cut -f1)

echo "Database backup size: $(numfmt --to=iec $DB_SIZE)"
echo "Storage backup size: $(numfmt --to=iec $STORAGE_SIZE)"

# Alert if backups are suspiciously small
if [ "$DB_SIZE" -lt 1000 ]; then
  echo "WARNING: Database backup is very small ($DB_SIZE bytes)"
fi

echo "Backup verification completed successfully"
```

**Make script executable:**
```bash
sudo chmod +x /usr/local/bin/varlor-backup-verify.sh
```

**Add to crontab (daily at 5 AM):**
```bash
sudo crontab -e
# Add:
0 5 * * * /usr/local/bin/varlor-backup-verify.sh >> /var/log/varlor/backup-verify.log 2>&1
```

---

## Manual Backup Procedures

### On-Demand Database Backup

```bash
# Create manual backup
sudo /usr/local/bin/varlor-db-backup.sh

# Verify backup created
ls -lh /var/backups/varlor/database/ | tail -5
```

### On-Demand Storage Backup

```bash
# Create manual backup
sudo /usr/local/bin/varlor-storage-backup.sh

# Verify backup created
ls -lh /var/backups/varlor/storage/ | tail -5
```

### Pre-Deployment Backup

**Before any major deployment or migration:**

```bash
#!/bin/bash
# Pre-deployment backup script

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_TAG="pre_deployment_$DATE"

echo "Creating pre-deployment backup: $BACKUP_TAG"

# Database backup
pg_dump -h 127.0.0.1 -U varlor_prod -d varlor \
  --format=custom \
  --compress=9 \
  --file="/var/backups/varlor/database/${BACKUP_TAG}.sql"

# Storage backup
rsync -av /var/www/varlor/storage/datasets/ \
  "/var/backups/varlor/storage/${BACKUP_TAG}/"

echo "Pre-deployment backup completed: $BACKUP_TAG"
echo "Database: /var/backups/varlor/database/${BACKUP_TAG}.sql"
echo "Storage: /var/backups/varlor/storage/${BACKUP_TAG}/"
```

---

## Recovery Procedures

### 1. Database Recovery

#### Full Database Restore

```bash
#!/bin/bash
# Restore entire database from backup

BACKUP_FILE="/var/backups/varlor/database/varlor_db_20251123_020000.sql.gz"

# Verify backup file exists
if [ ! -f "$BACKUP_FILE" ]; then
  echo "ERROR: Backup file not found: $BACKUP_FILE"
  exit 1
fi

# Verify checksum
if [ -f "${BACKUP_FILE}.sha256" ]; then
  sha256sum -c "${BACKUP_FILE}.sha256" || exit 1
fi

# Stop application (prevent writes during restore)
sudo systemctl stop varlor-server

# Decompress if needed
if [[ "$BACKUP_FILE" == *.gz ]]; then
  gunzip -c "$BACKUP_FILE" > /tmp/restore_db.sql
  RESTORE_FILE="/tmp/restore_db.sql"
else
  RESTORE_FILE="$BACKUP_FILE"
fi

# Drop existing database (DANGER!)
echo "WARNING: This will drop the existing database!"
read -p "Are you sure? (yes/no): " confirm
if [ "$confirm" != "yes" ]; then
  echo "Restore cancelled"
  exit 0
fi

dropdb -h 127.0.0.1 -U varlor_prod varlor
createdb -h 127.0.0.1 -U varlor_prod varlor

# Restore database
pg_restore -h 127.0.0.1 -U varlor_prod -d varlor "$RESTORE_FILE"

# Verify restore
psql -h 127.0.0.1 -U varlor_prod -d varlor -c "SELECT COUNT(*) FROM datasets"

# Clean up
rm -f /tmp/restore_db.sql

# Start application
sudo systemctl start varlor-server

echo "Database restore completed successfully"
```

#### Selective Table Restore

```bash
#!/bin/bash
# Restore only datasets tables

BACKUP_FILE="/var/backups/varlor/database/varlor_db_20251123_020000.sql"

# Extract specific tables
pg_restore -h 127.0.0.1 -U varlor_prod -d varlor \
  --table=datasets \
  --table=dataset_columns \
  "$BACKUP_FILE"

echo "Datasets tables restored successfully"
```

### 2. File Storage Recovery

#### Full Storage Restore

```bash
#!/bin/bash
# Restore entire storage directory

BACKUP_DIR="/var/backups/varlor/storage/backup_20251123_030000"
STORAGE_PATH="/var/www/varlor/storage/datasets"

# Verify backup directory exists
if [ ! -d "$BACKUP_DIR" ]; then
  echo "ERROR: Backup directory not found: $BACKUP_DIR"
  exit 1
fi

# Stop application
sudo systemctl stop varlor-server

# Backup current storage (just in case)
mv "$STORAGE_PATH" "${STORAGE_PATH}_old_$(date +%Y%m%d_%H%M%S)"

# Restore from backup
rsync -av "$BACKUP_DIR/" "$STORAGE_PATH/"

# Verify permissions
sudo chown -R varlor-app:varlor-app "$STORAGE_PATH"
sudo chmod -R 750 "$STORAGE_PATH"

# Start application
sudo systemctl start varlor-server

echo "Storage restore completed successfully"
```

#### Selective File Recovery

```bash
#!/bin/bash
# Restore specific tenant's data

TENANT_ID="tenant-abc"
BACKUP_DIR="/var/backups/varlor/storage/latest"
STORAGE_PATH="/var/www/varlor/storage/datasets"

# Restore tenant directory
rsync -av \
  "$BACKUP_DIR/$TENANT_ID/" \
  "$STORAGE_PATH/$TENANT_ID/"

# Fix permissions
sudo chown -R varlor-app:varlor-app "$STORAGE_PATH/$TENANT_ID"
sudo chmod -R 750 "$STORAGE_PATH/$TENANT_ID"

echo "Tenant $TENANT_ID data restored successfully"
```

#### Single Dataset Recovery

```bash
#!/bin/bash
# Restore single dataset file

TENANT_ID="tenant-abc"
DATASET_ID="123"
BACKUP_DIR="/var/backups/varlor/storage/latest"
STORAGE_PATH="/var/www/varlor/storage/datasets"

# Restore dataset directory
rsync -av \
  "$BACKUP_DIR/$TENANT_ID/$DATASET_ID/" \
  "$STORAGE_PATH/$TENANT_ID/$DATASET_ID/"

# Fix permissions
sudo chown -R varlor-app:varlor-app "$STORAGE_PATH/$TENANT_ID/$DATASET_ID"
sudo chmod -R 750 "$STORAGE_PATH/$TENANT_ID/$DATASET_ID"

echo "Dataset $DATASET_ID restored successfully"
```

### 3. Point-in-Time Recovery

For more granular recovery, use PostgreSQL's point-in-time recovery (PITR) with Write-Ahead Log (WAL) archiving.

**Enable WAL archiving:**
```bash
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'rsync -a %p /var/backups/varlor/wal_archive/%f'
```

**Perform PITR:**
```bash
# Restore base backup
pg_restore -d varlor /var/backups/varlor/database/base_backup.sql

# Create recovery.conf
cat > /var/lib/postgresql/14/main/recovery.conf <<EOF
restore_command = 'cp /var/backups/varlor/wal_archive/%f %p'
recovery_target_time = '2025-11-23 10:30:00'
EOF

# Start PostgreSQL (will replay WAL to target time)
sudo systemctl start postgresql
```

---

## Disaster Recovery

### Disaster Scenarios

#### 1. Complete Server Failure

**Recovery steps:**

1. **Provision new server**
   - Match hardware specifications
   - Install OS and dependencies
   - Configure network and firewall

2. **Restore application**
   ```bash
   # Deploy application code
   git clone https://github.com/your-org/varlor.git /var/www/varlor
   cd /var/www/varlor/server
   npm ci --production
   npm run build
   ```

3. **Restore database**
   ```bash
   # Download backup from offsite storage
   aws s3 cp s3://varlor-backups/database/latest.sql.gz /tmp/

   # Restore database (see Database Recovery section)
   ```

4. **Restore file storage**
   ```bash
   # Download backup from offsite storage
   aws s3 sync s3://varlor-backups/storage/latest/ /var/www/varlor/storage/datasets/

   # Fix permissions
   sudo chown -R varlor-app:varlor-app /var/www/varlor/storage
   ```

5. **Configure environment**
   ```bash
   # Copy production environment file
   cp /path/to/secure/storage/.env.production /var/www/varlor/server/.env
   ```

6. **Start services**
   ```bash
   sudo systemctl start varlor-server
   sudo systemctl start varlor-client
   ```

7. **Verify recovery**
   ```bash
   curl -f https://api.varlor.com/health
   ```

**Recovery Time Objective (RTO)**: 4 hours
**Recovery Point Objective (RPO)**: 24 hours

#### 2. Database Corruption

**Recovery steps:**

1. **Identify corruption**
   ```sql
   -- Check for corruption
   SELECT * FROM pg_stat_database;
   REINDEX DATABASE varlor;
   ```

2. **Restore from backup** (see Database Recovery section)

3. **Verify data integrity**
   ```sql
   SELECT COUNT(*) FROM datasets;
   SELECT COUNT(*) FROM dataset_columns;
   ```

#### 3. Accidental Data Deletion

**Recovery steps:**

1. **Identify deletion**
   - Check application logs
   - Identify deleted datasets

2. **Restore from backup**
   - If caught quickly, restore from latest backup
   - If older, restore from appropriate backup

3. **Verify recovery**
   - Check dataset exists in database
   - Verify file exists in storage

---

## Testing and Validation

### Backup Testing Schedule

**Monthly:**
- [ ] Test database restore on staging server
- [ ] Test file storage restore on staging server
- [ ] Verify backup sizes are reasonable
- [ ] Check backup automation is working

**Quarterly:**
- [ ] Full disaster recovery drill
- [ ] Test restore from offsite backups
- [ ] Validate backup encryption
- [ ] Review and update recovery procedures

### Backup Validation Checklist

**Daily automated checks:**
- [ ] Backup files created in last 24 hours
- [ ] Backup file sizes are not zero
- [ ] Checksums match for database backups
- [ ] No errors in backup logs

**Manual validation (weekly):**
- [ ] Test restore on staging environment
- [ ] Verify data integrity after restore
- [ ] Check backup retention is working
- [ ] Confirm offsite backups are uploading

### Test Restore Procedure

```bash
#!/bin/bash
# Test restore on staging environment

STAGING_DB="varlor_staging"
PROD_BACKUP="/var/backups/varlor/database/latest.sql.gz"

echo "Starting test restore to staging..."

# Drop and recreate staging database
dropdb -h staging-db-host -U varlor_user $STAGING_DB
createdb -h staging-db-host -U varlor_user $STAGING_DB

# Restore from production backup
gunzip -c "$PROD_BACKUP" | pg_restore -h staging-db-host -U varlor_user -d $STAGING_DB

# Verify row counts
PROD_COUNT=$(psql -h production-db-host -U varlor_user -d varlor -t -c "SELECT COUNT(*) FROM datasets")
STAGING_COUNT=$(psql -h staging-db-host -U varlor_user -d $STAGING_DB -t -c "SELECT COUNT(*) FROM datasets")

if [ "$PROD_COUNT" -eq "$STAGING_COUNT" ]; then
  echo "Test restore successful: $STAGING_COUNT datasets"
else
  echo "ERROR: Row count mismatch! Prod: $PROD_COUNT, Staging: $STAGING_COUNT"
  exit 1
fi
```

---

## Backup Monitoring

### Monitor These Metrics

1. **Backup success rate**: Target 100%
2. **Backup duration**: Database < 10 min, Storage < 30 min
3. **Backup size trends**: Track growth over time
4. **Disk space**: Ensure adequate space for backups
5. **Restore test success rate**: Target 100%

### Alerts

**Critical alerts:**
- Backup failed (no backup created in 24 hours)
- Backup verification failed (checksum mismatch)
- Insufficient disk space for backups

**Warning alerts:**
- Backup duration longer than normal
- Backup size significantly different than usual
- Offsite backup upload failed

---

## Documentation Maintenance

This document should be reviewed and updated:
- After any changes to backup procedures
- After disaster recovery drills
- Quarterly as part of operations review
- When new team members join

---

**Document Version**: 1.0
**Last Updated**: 2025-11-23
**Last Tested**: Not yet tested
**Next Review**: 2026-02-23
**Maintained By**: DevOps Team
