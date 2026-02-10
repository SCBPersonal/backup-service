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
   - Poll YBA for job completion
   - Fetch base_backup_uuid
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
```properties
# Polling Configuration
backup.poller.interval=30000  # 30 seconds
backup.poller.max-attempts=120  # 1 hour max

# YBA API Configuration
yba.base-url=https://yba-api.example.com
yba.api-token=<your-api-token>
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


