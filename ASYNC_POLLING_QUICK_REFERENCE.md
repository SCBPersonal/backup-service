# Async Polling Quick Reference Guide

## Overview
This guide provides quick reference for developers working with the async backup polling feature.

## How It Works

### Full Backup Flow
```
1. Client calls POST /backup with categoryCode
2. YbaClient triggers YBA full backup API
3. YbaClient extracts taskUUID from response
4. YbaClient stores record in full_backup_tracker table
5. YbaClient starts BackupPollerService (async, non-blocking)
6. API returns immediately to client
7. Poller monitors task status every 30 seconds
8. When complete, poller fetches base UUID and updates DB
```

### Incremental Backup Flow
```
1. Client calls POST /backup with categoryCode
2. YbaClient retrieves base UUID from full_backup_tracker
3. YbaClient triggers YBA incremental backup API
4. YbaClient stores record in incremental_backup_tracker
5. API returns immediately to client
```

## Key Classes

### BackupPollerService
- **Location**: `src/main/java/com/scb/backup/service/BackupPollerService.java`
- **Purpose**: Background polling of YBA task status
- **Key Method**: `startPolling(config, categoryCode, month, taskUuid, customerUuid)`
- **Annotation**: `@Async` for non-blocking execution

### BackupPollerProperties
- **Location**: `src/main/java/com/scb/backup/config/BackupPollerProperties.java`
- **Purpose**: Configuration properties for poller
- **Prefix**: `backup.poller`

### YbaClient
- **Location**: `src/main/java/com/scb/backup/client/YbaClient.java`
- **Key Changes**:
  - `fullBackup()` now starts async poller
  - `incrementalBackup()` stores record in DB
  - `extractTaskUuid()` extracts task UUID from response

## Database Tables

### full_backup_tracker
Stores full backup details and base UUID after completion.

**Key Columns**:
- `category_code`: Backup category (e.g., "HWA_EPR_DB_BACKUP_FULL")
- `backup_month`: Month in YYYY-MM format
- `task_uuid`: YBA task UUID for polling
- `base_backup_uuid`: Populated after job completes
- `backup_status`: IN_PROGRESS, SUCCESS, FAILED
- `full_backup_response`: YBA API response JSON

### incremental_backup_tracker
Stores incremental backup details.

**Key Columns**:
- `category_code`: Backup category (e.g., "HWA_EPR_DB_BACKUP_INCRE")
- `backup_month`: Month in YYYY-MM format
- `base_backup_uuid`: Reference to full backup UUID
- `task_uuid`: YBA task UUID
- `backup_status`: IN_PROGRESS, SUCCESS, FAILED
- `incremental_backup_response`: YBA API response JSON

## Configuration

### Default Values
```yaml
backup:
  poller:
    enabled: true
    initial-delay-ms: 5000        # 5 seconds
    polling-interval-ms: 30000    # 30 seconds
    max-poll-attempts: 120        # 1 hour total
    thread-pool-size: 5
```

### Environment Variables
- `BACKUP_POLLER_ENABLED`: Enable/disable (default: true)
- `BACKUP_POLLER_INTERVAL`: Polling interval in ms (default: 30000)
- `BACKUP_POLLER_MAX_ATTEMPTS`: Max attempts (default: 120)

## Monitoring

### Log Messages to Watch
```
✅ "Starting background polling for full backup task..."
✅ "Polling task status for task UUID: {}"
✅ "Task completed successfully"
✅ "Base backup UUID stored successfully: {}"
⚠️  "Polling timed out after {} attempts"
❌ "Error polling task status: {}"
```

### Database Queries

**Check active pollers**:
```sql
SELECT * FROM epricing.full_backup_tracker 
WHERE backup_status = 'IN_PROGRESS' 
ORDER BY start_time DESC;
```

**Check completed backups**:
```sql
SELECT category_code, backup_month, base_backup_uuid, backup_status, end_time
FROM epricing.full_backup_tracker 
WHERE backup_status = 'SUCCESS' 
ORDER BY end_time DESC 
LIMIT 10;
```

**Check failed backups**:
```sql
SELECT category_code, backup_month, backup_status, error_message, end_time
FROM epricing.full_backup_tracker 
WHERE backup_status = 'FAILED' 
ORDER BY end_time DESC 
LIMIT 10;
```

## Troubleshooting

### Issue: Poller not starting
**Check**:
1. `@EnableAsync` annotation present in `BackupOrchestratorApplication`
2. `backup.poller.enabled=true` in configuration
3. Check logs for "Starting background polling" message

### Issue: Base UUID not populated
**Check**:
1. Task status in YBA (may still be running)
2. Poller logs for errors
3. Database record in `full_backup_tracker` table
4. YBA "last backup" API response

### Issue: Polling timeout
**Check**:
1. Increase `max-poll-attempts` in configuration
2. Check YBA for job status
3. Verify network connectivity to YBA

## Testing

### Manual Test - Full Backup
```bash
curl -X POST http://localhost:8989/backup \
  -H "Content-Type: application/json" \
  -d '{
    "categoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchId": "TEST_BATCH_001",
    "businessDate": "2026-02-06"
  }'
```

**Expected**:
1. API returns immediately with 200 OK
2. Check logs for "Starting background polling"
3. Query database for IN_PROGRESS status
4. Wait for job completion (check logs)
5. Verify base_backup_uuid is populated

### Manual Test - Incremental Backup
```bash
curl -X POST http://localhost:8989/backup \
  -H "Content-Type: application/json" \
  -d '{
    "categoryCode": "HWA_EPR_DB_BACKUP_INCRE",
    "batchId": "TEST_BATCH_002",
    "businessDate": "2026-02-06"
  }'
```

**Expected**:
1. API returns immediately with 200 OK
2. Check logs for base UUID retrieval
3. Verify incremental backup record in database

