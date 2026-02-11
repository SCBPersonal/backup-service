# Backup Orchestrator Service - Example Use Cases

## Table of Contents
1. [Full Backup Use Case](#full-backup-use-case)
2. [Incremental Backup Use Case](#incremental-backup-use-case)
3. [Error Scenarios](#error-scenarios)
4. [Database State Examples](#database-state-examples)

---

## Full Backup Use Case

### Scenario
Perform a full backup of the EPricing database on February 6, 2026.

### Step-by-Step Flow

#### Step 1: Trigger Full Backup
**Request**:
```http
POST http://localhost:8989/backupProcess
Content-Type: application/json

{
  "batchId": "BATCH_20260206_001",
  "businessDate": "2026-02-06",
  "categoryCode": "HWA_EPR_DB_BACKUP_FULL"
}
```

#### Step 2: Service Processing

**2.1 BackupService.process()**
- Validates batch parameters
- Extracts business date from batch ID
- Calls YbaClient.backupInitiate()

**2.2 YbaClient.fullBackup()**
- Calls YBA API: `POST /api/v1/customers/{cUUID}/universes/{uUUID}/backups`
- Request Body:
```json
{
  "storageConfigUUID": "storage-uuid-123",
  "sse": false,
  "backupType": "PGSQL_TABLE_TYPE",
  "backupCategory": "YB_CONTROLLER",
  "universeUUID": "universe-uuid-456",
  "timeBeforeDelete": 86400000,
  "expiryTimeUnit": "MILLISECONDS",
  "keyspaceTableList": [
    {"keyspace": "epricing"}
  ]
}
```

**2.3 YBA Response**:
```json
{
  "taskUUID": "task-abc-123",
  "resourceUUID": "backup-xyz-789",
  "status": "Running"
}
```

**2.4 Database Updates**:

**Insert into full_backup_tracker**:
```sql
INSERT INTO epricing.full_backup_tracker(
  batch_id, category_code, business_date, backup_month, 
  backup_status, task_uuid, start_time
) VALUES(
  'BATCH_20260206_001', 
  'HWA_EPR_DB_BACKUP_FULL', 
  '2026-02-06', 
  '2026-02',
  'IN_PROGRESS', 
  'task-abc-123', 
  CURRENT_TIMESTAMP
);
```

**Update with full backup response**:
```sql
UPDATE epricing.full_backup_tracker 
SET full_backup_response = '{
  "taskUUID": "task-abc-123",
  "resourceUUID": "backup-xyz-789",
  "status": "Running"
}',
updated_at = CURRENT_TIMESTAMP
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL' 
AND backup_month = '2026-02';
```

#### Step 3: Async Polling (BackupPollerService)

**3.1 Poller Monitors Job**:
- Polls YBA API: `GET /api/v1/customers/{cUUID}/tasks/{taskUUID}`
- Checks job status every 30 seconds (configurable)

**3.2 Job Completion Response**:
```json
{
  "taskUUID": "task-abc-123",
  "status": "Success",
  "completionTime": "2026-02-06T10:45:00Z"
}
```

**3.3 Fetch Base UUID**:
- Calls YBA API: `POST /api/v1/customers/{cUUID}/universes/{uUUID}/backups/page`
- Request body:
```json
{
  "direction": "DESC",
  "limit": 1,
  "sortBy": "createTime",
  "filter": {
    "universeUUIDList": ["universe-uuid-456"]
  }
}
```
- Extracts `baseBackupUUID` from paginated response: `entities[0].backupUUID`

**3.4 Update with Base UUID**:
```sql
UPDATE epricing.full_backup_tracker 
SET base_backup_uuid = 'base-uuid-full-2026-02',
    backup_status = 'SUCCESS',
    end_time = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL' 
AND backup_month = '2026-02';
```

### Final State in full_backup_tracker

| batch_id | category_code | business_date | backup_month | backup_status | base_backup_uuid | task_uuid | start_time | end_time |
|----------|---------------|---------------|--------------|---------------|------------------|-----------|------------|----------|
| BATCH_20260206_001 | HWA_EPR_DB_BACKUP_FULL | 2026-02-06 | 2026-02 | SUCCESS | base-uuid-full-2026-02 | task-abc-123 | 2026-02-06 10:30:00 | 2026-02-06 10:45:00 |

---

## Incremental Backup Use Case

### Scenario
Perform an incremental backup of the EPricing database on February 7, 2026 (after full backup on Feb 6).

### Step-by-Step Flow

#### Step 1: Trigger Incremental Backup
**Request**:
```http
POST http://localhost:8989/backupProcess
Content-Type: application/json

{
  "batchId": "BATCH_20260207_001",
  "businessDate": "2026-02-07",
  "categoryCode": "HWA_EPR_DB_BACKUP_INCRE"
}
```

#### Step 2: Service Processing

**2.1 YbaClient.performIncrementalBackup()**
- Fetches base UUID from full_backup_tracker:
```sql
SELECT base_backup_uuid 
FROM epricing.full_backup_tracker 
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL' 
AND backup_month = '2026-02' 
AND backup_status = 'SUCCESS'
ORDER BY updated_at DESC LIMIT 1;
```
- Result: `base-uuid-full-2026-02`

**2.2 Call YBA API**:
```json
{
  "storageConfigUUID": "storage-uuid-123",
  "sse": false,
  "backupType": "PGSQL_TABLE_TYPE",
  "backupCategory": "YB_CONTROLLER",
  "universeUUID": "universe-uuid-456",
  "baseBackupUUID": "base-uuid-full-2026-02",
  "keyspaceTableList": [
    {"keyspace": "epricing"}
  ]
}
```

**2.3 YBA Response**:
```json
{
  "taskUUID": "task-incr-456",
  "resourceUUID": "backup-incr-999",
  "status": "Running"
}
```

**2.4 Database Updates**:

**Insert into incremental_backup_tracker**:
```sql
INSERT INTO epricing.incremental_backup_tracker(
  batch_id, category_code, business_date, backup_month,
  base_backup_uuid, backup_status, task_uuid, start_time
) VALUES(
  'BATCH_20260207_001',
  'HWA_EPR_DB_BACKUP_INCRE',
  '2026-02-07',
  '2026-02',
  'base-uuid-full-2026-02',
  'IN_PROGRESS',
  'task-incr-456',
  CURRENT_TIMESTAMP
);
```

