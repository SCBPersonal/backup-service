# Backup Orchestrator Service - Quick Reference Guide

## 📋 Table of Contents
1. [API Endpoints](#api-endpoints)
2. [Database Tables](#database-tables)
3. [Key Workflows](#key-workflows)
4. [Configuration](#configuration)
5. [Monitoring Queries](#monitoring-queries)
6. [Troubleshooting](#troubleshooting)

---

## API Endpoints

### Trigger Backup
```http
POST http://localhost:8989/backupProcess
Content-Type: application/json

{
  "batchId": "BATCH_20260206_001",
  "businessDate": "2026-02-06",
  "categoryCode": "HWA_EPR_DB_BACKUP_FULL"  // or "HWA_EPR_DB_BACKUP_INCRE"
}
```

**Response:**
```json
{
  "batchId": "BATCH_20260206_001",
  "status": "INITIATED",
  "taskUUID": "task-abc-123",
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

## Database Tables

### 1. BATCH_EXECUTION_TABLE (Managed by Batch Framework)
```sql
-- DO NOT directly insert/delete
-- Only UPDATE status field via BackupService
```

### 2. FULL_BACKUP_TRACKER
```sql
-- Stores full backup state and base UUID
-- UNIQUE constraint: (category_code, backup_month)
-- One full backup per category per month

Key Fields:
- base_backup_uuid: Critical for incremental backups
- backup_status: IN_PROGRESS | SUCCESS | FAILED
- task_uuid: For YBA job tracking
- full_backup_response: YBA API response JSON
```

### 3. INCREMENTAL_BACKUP_TRACKER
```sql
-- Stores incremental backup state
-- NO UNIQUE constraint: Multiple incrementals per month allowed
-- References base_backup_uuid from full_backup_tracker

Key Fields:
- base_backup_uuid: FK to full_backup_tracker
- backup_status: IN_PROGRESS | SUCCESS | FAILED
- task_uuid: For YBA job tracking
- incremental_backup_response: YBA API response JSON
```

---

## Key Workflows

### Full Backup Workflow
```
1. POST /backupProcess (categoryCode contains "FULL")
2. YbaClient.fullBackup()
   - INSERT into full_backup_tracker (status=IN_PROGRESS)
   - Call YBA API
   - UPDATE full_backup_response
3. BackupPollerService (async)
   - Poll YBA for job completion (GET /tasks/{taskUUID})
   - Fetch base_backup_uuid (POST /backups/page with pagination)
   - UPDATE full_backup_tracker (base_backup_uuid, status=SUCCESS)
```

### Incremental Backup Workflow
```
1. POST /backupProcess (categoryCode contains "INCRE")
2. YbaClient.performIncrementalBackup()
   - SELECT base_backup_uuid from full_backup_tracker
   - If NOT found → throw IllegalStateException
   - INSERT into incremental_backup_tracker (with base_backup_uuid)
   - Call YBA API with baseBackupUUID
   - UPDATE incremental_backup_response
```

---

## Configuration

### YBA Configuration (yba_config_table)
```sql
SELECT * FROM epricing.yba_config_table 
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL';
```

**Key Fields:**
- `customer_uuid`: YBA customer identifier
- `universe_uuid`: YBA universe identifier
- `storage_config_uuid`: Storage configuration
- `keyspace_name`: Database/keyspace name
- `backup_type`: PGSQL_TABLE_TYPE | YCQL_TABLE_TYPE

### Application Properties
```yaml
# Backup Poller Configuration
backup:
  poller:
    enabled: true
    initial-delay-ms: 5000      # 5 seconds
    polling-interval-ms: 30000  # 30 seconds
    thread-pool-size: 5
    # Note: Infinite retry - no max-attempts limit

# YBA API Configuration
yba:
  base-url: https://yba-api.example.com/api/v1
  customer-id: cust123
  databases:
    uam-db:
      job-completion-check-url: ${yba.base-url}/customers/${yba.customer-id}/tasks/{taskUuid}
      full-backup-url: ${yba.base-url}/customers/${yba.customer-id}/backups
      api-token: <your-api-token>
      # ... other config
```

---

## Monitoring Queries

### Check Full Backup Status
```sql
SELECT 
  batch_id,
  category_code,
  backup_month,
  backup_status,
  base_backup_uuid,
  start_time,
  end_time,
  EXTRACT(EPOCH FROM (end_time - start_time))/60 AS duration_minutes
FROM epricing.full_backup_tracker
WHERE backup_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
ORDER BY start_time DESC;
```

### Check Incremental Backups
```sql
SELECT 
  batch_id,
  category_code,
  business_date,
  base_backup_uuid,
  backup_status,
  start_time
FROM epricing.incremental_backup_tracker
WHERE backup_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
ORDER BY business_date DESC;
```

### Find Failed Backups
```sql
SELECT 
  'FULL' AS backup_type,
  batch_id,
  category_code,
  backup_status,
  error_message,
  start_time
FROM epricing.full_backup_tracker
WHERE backup_status = 'FAILED'
  AND start_time >= CURRENT_DATE - INTERVAL '7 days'

UNION ALL

SELECT 
  'INCREMENTAL' AS backup_type,
  batch_id,
  category_code,
  backup_status,
  error_message,
  start_time
FROM epricing.incremental_backup_tracker
WHERE backup_status = 'FAILED'
  AND start_time >= CURRENT_DATE - INTERVAL '7 days'

ORDER BY start_time DESC;
```

### Get Base UUID for Current Month
```sql
SELECT 
  category_code,
  base_backup_uuid,
  backup_status,
  updated_at
FROM epricing.full_backup_tracker
WHERE backup_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
  AND backup_status = 'SUCCESS'
ORDER BY category_code;
```

### Check In-Progress Backups
```sql
SELECT 
  'FULL' AS type,
  batch_id,
  category_code,
  task_uuid,
  start_time,
  EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - start_time))/60 AS running_minutes
FROM epricing.full_backup_tracker
WHERE backup_status = 'IN_PROGRESS'

UNION ALL

SELECT 
  'INCREMENTAL' AS type,
  batch_id,
  category_code,
  task_uuid,
  start_time,
  EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - start_time))/60 AS running_minutes
FROM epricing.incremental_backup_tracker
WHERE backup_status = 'IN_PROGRESS';
```

---

## Troubleshooting

### Issue 1: Incremental Backup Fails - "Base backup UUID not found"

**Symptom:**
```
IllegalStateException: Base backup UUID not found for category 'HWA_EPR_DB_BACKUP_INCRE'
and month '2026-02'. Please perform a full backup first for the current month.
```

**Root Cause:**
- No successful full backup exists for the current month
- Full backup failed or still in progress

**Solution:**
```sql
-- 1. Check if full backup exists for current month
SELECT * FROM epricing.full_backup_tracker
WHERE backup_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM')
  AND category_code LIKE '%FULL%';

-- 2. If no record or status != SUCCESS, trigger full backup first
-- POST /backupProcess with categoryCode containing "FULL"

-- 3. Wait for BackupPollerService to update base_backup_uuid
-- Then retry incremental backup
```

---

### Issue 2: Full Backup Stuck in IN_PROGRESS

**Symptom:**
- Full backup status remains IN_PROGRESS for extended period
- base_backup_uuid is NULL

**Root Cause:**
- BackupPollerService not running
- YBA job failed but poller didn't update status
- Network issues between service and YBA

**Solution:**
```sql
-- 1. Check how long backup has been running
SELECT
  batch_id,
  task_uuid,
  start_time,
  EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - start_time))/60 AS running_minutes
FROM epricing.full_backup_tracker
WHERE backup_status = 'IN_PROGRESS';

-- 2. Manually check YBA job status
-- GET https://yba-api/api/v1/customers/{cUUID}/tasks/{taskUUID}

-- 3. If YBA shows success, manually update:
UPDATE epricing.full_backup_tracker
SET backup_status = 'SUCCESS',
    base_backup_uuid = '<uuid-from-yba>',
    end_time = CURRENT_TIMESTAMP
WHERE task_uuid = '<task-uuid>';

-- 4. If YBA shows failure, mark as failed:
UPDATE epricing.full_backup_tracker
SET backup_status = 'FAILED',
    error_message = '<error-from-yba>',
    end_time = CURRENT_TIMESTAMP
WHERE task_uuid = '<task-uuid>';
```

---

### Issue 3: Duplicate Full Backup Attempt

**Symptom:**
```
ERROR: duplicate key value violates unique constraint "unique_category_month"
```

**Root Cause:**
- Attempting second full backup in same month for same category
- UNIQUE constraint on (category_code, backup_month)

**Solution:**
```sql
-- 1. Check existing full backup for the month
SELECT * FROM epricing.full_backup_tracker
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
  AND backup_month = '2026-02';

-- 2. Options:
-- a) If previous backup failed, delete and retry:
DELETE FROM epricing.full_backup_tracker
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
  AND backup_month = '2026-02'
  AND backup_status = 'FAILED';

-- b) If previous backup succeeded, use it for incrementals
-- c) If you need to re-run, delete existing record (use with caution):
DELETE FROM epricing.full_backup_tracker
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
  AND backup_month = '2026-02';
```

---

### Issue 4: YBA API Connection Timeout

**Symptom:**
```
WebClientRequestException: Connection timeout
```

**Root Cause:**
- YBA API unreachable
- Network connectivity issues
- Incorrect YBA configuration

**Solution:**
```bash
# 1. Check YBA API connectivity
curl -X GET https://yba-api/api/v1/customers \
  -H "X-AUTH-YW-API-TOKEN: <token>"

# 2. Verify configuration
SELECT * FROM epricing.yba_config_table
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL';

# 3. Check application logs
tail -f logs/backup-orchestrator-service.log | grep YbaClient

# 4. Verify network connectivity
ping yba-api.example.com
telnet yba-api.example.com 443
```

---

### Issue 5: Batch Execution Table Not Updated

**Symptom:**
- Backup completes but batch_execution_table shows IN_PROGRESS

**Root Cause:**
- BackupService.handleBackupSuccess() not called
- Exception during batch status update

**Solution:**
```sql
-- 1. Check batch execution status
SELECT * FROM batch_execution_table
WHERE batch_id = 'BATCH_20260206_001';

-- 2. Manually update if needed
UPDATE batch_execution_table
SET status = 'COMPLETED',
    updated_at = CURRENT_TIMESTAMP
WHERE batch_id = 'BATCH_20260206_001';

-- 3. Check application logs for exceptions
grep "BATCH_20260206_001" logs/backup-orchestrator-service.log
```

---

## Common Commands

### Restart Backup Poller Service
```bash
# If using Spring Boot Actuator
curl -X POST http://localhost:8989/actuator/restart

# Or restart entire service
systemctl restart backup-orchestrator-service
```

### View Application Logs
```bash
# Real-time logs
tail -f logs/backup-orchestrator-service.log

# Filter by batch ID
grep "BATCH_20260206_001" logs/backup-orchestrator-service.log

# Filter by error
grep "ERROR" logs/backup-orchestrator-service.log | tail -50
```

### Manual Cleanup (Use with Caution)
```sql
-- Delete failed backups older than 30 days
DELETE FROM epricing.full_backup_tracker
WHERE backup_status = 'FAILED'
  AND start_time < CURRENT_DATE - INTERVAL '30 days';

DELETE FROM epricing.incremental_backup_tracker
WHERE backup_status = 'FAILED'
  AND start_time < CURRENT_DATE - INTERVAL '30 days';

-- Archive old successful backups (optional)
-- Create archive table first, then move old records
```

---

## Performance Tips

### Index Optimization
```sql
-- Ensure indexes exist for common queries
CREATE INDEX IF NOT EXISTS idx_full_category_month
ON epricing.full_backup_tracker(category_code, backup_month);

CREATE INDEX IF NOT EXISTS idx_incr_base_uuid
ON epricing.incremental_backup_tracker(base_backup_uuid);

CREATE INDEX IF NOT EXISTS idx_full_task_uuid
ON epricing.full_backup_tracker(task_uuid);

CREATE INDEX IF NOT EXISTS idx_incr_task_uuid
ON epricing.incremental_backup_tracker(task_uuid);
```

### Query Performance
```sql
-- Use EXPLAIN ANALYZE to check query performance
EXPLAIN ANALYZE
SELECT base_backup_uuid
FROM epricing.full_backup_tracker
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
  AND backup_month = '2026-02'
  AND backup_status = 'SUCCESS';
```

---

## Best Practices

1. ✅ **Always run full backup first** at the beginning of each month
2. ✅ **Monitor BackupPollerService** to ensure it's updating base UUIDs
3. ✅ **Check backup status** before triggering incremental backups
4. ✅ **Archive old backup records** to maintain database performance
5. ✅ **Set up alerts** for failed backups
6. ✅ **Test restore procedures** regularly
7. ✅ **Document YBA configuration** for each category
8. ✅ **Monitor disk space** on YBA storage

---

## Quick Reference - Category Codes

| Category Code | Type | Description |
|---------------|------|-------------|
| HWA_EPR_DB_BACKUP_FULL | Full | EPricing full backup |
| HWA_EPR_DB_BACKUP_INCRE | Incremental | EPricing incremental backup |
| HWA_CRM_DB_BACKUP_FULL | Full | CRM full backup |
| HWA_CRM_DB_BACKUP_INCRE | Incremental | CRM incremental backup |

---

## Contact & Support

- **Service Owner**: Backup Orchestrator Team
- **Documentation**: See BACKUP_ORCHESTRATOR_DIAGRAMS.md
- **Example Use Cases**: See EXAMPLE_USE_CASES.md
- **Master Prompt**: See document/BACKUP_ORCHESTRATOR_SERVICE_MASTER_PROMPT.md

---

**Document Version:** 1.0
**Last Updated:** 2026-02-06

