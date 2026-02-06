# Backup UUID Storage Implementation

## Overview

This document describes the changes made to implement database-based storage and retrieval of base backup UUIDs for incremental backups, organized by month.

## Requirements

1. **Full Backup**: After successful full backup, extract and store the `baseBackupUUID` in the database
2. **Incremental Backup**: Fetch the `baseBackupUUID` from the database (instead of YBA API) for incremental backups
3. **Monthly Storage**: Store base UUIDs monthly in the database (format: YYYY-MM)

## Changes Made

### 1. Database Schema

**New Table**: `base_backup_uuid_tracker`

```sql
CREATE TABLE epricing.base_backup_uuid_tracker (
    id SERIAL PRIMARY KEY,
    category_code VARCHAR(255) NOT NULL,
    base_backup_uuid VARCHAR(255) NOT NULL,
    backup_month VARCHAR(7) NOT NULL,  -- Format: YYYY-MM
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(category_code, backup_month)
);
```

**Features**:
- Unique constraint on `(category_code, backup_month)` ensures one base UUID per category per month
- Index on `(category_code, backup_month)` for fast lookups
- Automatic timestamp tracking with `created_at` and `updated_at`

**File**: `src/main/resources/db/migration/V2__create_base_backup_uuid_table.sql`

### 2. Application Configuration

**New SQL Queries** added to `application.yml`:

```yaml
data:
  insert-base-backup-uuid:
    query: INSERT INTO epricing.base_backup_uuid_tracker(category_code, base_backup_uuid, backup_month, updated_at) 
           VALUES(:categoryCode, :baseBackupUuid, :backupMonth, CURRENT_TIMESTAMP) 
           ON CONFLICT (category_code, backup_month) 
           DO UPDATE SET base_backup_uuid = EXCLUDED.base_backup_uuid, updated_at = CURRENT_TIMESTAMP;
  
  get-base-backup-uuid:
    query: SELECT base_backup_uuid FROM epricing.base_backup_uuid_tracker 
           WHERE category_code = :categoryCode AND backup_month = :backupMonth 
           ORDER BY updated_at DESC LIMIT 1;
```

**Features**:
- `INSERT ... ON CONFLICT DO UPDATE` (UPSERT) ensures idempotency
- Automatically updates existing records if a new full backup is performed in the same month

### 3. BackupDaoService Updates

**New Methods**:

```java
public void storeBaseBackupUuid(String categoryCode, String baseBackupUuid, String backupMonth)
public String getBaseBackupUuidFromDb(String categoryCode, String backupMonth)
```

**Features**:
- `storeBaseBackupUuid`: Stores/updates base backup UUID for a category and month
- `getBaseBackupUuidFromDb`: Retrieves base backup UUID, returns `null` if not found
- Comprehensive error handling and logging

### 4. YbaClient Updates

**Modified Methods**:

1. **`backupInitiate()`**: Now passes `categoryCode` to backup methods
2. **`fullBackup()`**: 
   - Extracts backup UUID from YBA response
   - Stores UUID in database with current month
   - Supports multiple UUID field names: `resourceUUID`, `backupUUID`, `taskUUID`
3. **`performIncrementalBackup()`**:
   - First tries to fetch base UUID from database for current month
   - **VALIDATION**: If base UUID not found in DB, throws `IllegalStateException`
   - **REQUIREMENT**: Full backup must be performed first in current month before incremental backup
   - Uses current month (YYYY-MM format) for lookup

**New Helper Methods**:
- `extractBackupUuidFromResponse()`: Extracts UUID from YBA API response
- `getCurrentMonth()`: Returns current month in YYYY-MM format

## Workflow

### Full Backup Flow

```
1. Client → POST /backupProcess (full backup)
2. BackupService → validates and initiates
3. YbaClient.fullBackup() → calls YBA API
4. YBA API → returns response with backup UUID
5. YbaClient → extracts UUID from response
6. BackupDaoService.storeBaseBackupUuid() → stores in DB
   - category_code: e.g., "HWA_EPR_DB_BACKUP_FULL"
   - base_backup_uuid: e.g., "uuid-123-456"
   - backup_month: e.g., "2026-01"
7. Response returned to client
```

### Incremental Backup Flow

```
1. Client → POST /backupProcess (incremental backup)
2. BackupService → validates and initiates
3. YbaClient.performIncrementalBackup() → gets current month (e.g., "2026-02")
4. BackupDaoService.getBaseBackupUuidFromDb() → queries DB for current month
5. If UUID found in DB:
   - Use UUID from DB for incremental backup
   - YbaClient.incrementalBackup() → calls YBA API with base UUID
   - Response returned to client
6. If UUID NOT found in DB:
   - ❌ THROW IllegalStateException
   - Error message: "Base backup UUID not found for category 'XXX' and month 'YYYY-MM'.
                     Please perform a full backup first for the current month."
   - Backup FAILS - user must perform full backup first
```

## Benefits

1. **Performance**: Faster incremental backups (no API call to fetch last backup)
2. **Reliability**: Database is source of truth for base UUIDs
3. **Monthly Organization**: Easy to track and manage base backups by month
4. **Validation**: Ensures full backup exists before allowing incremental backup
5. **Idempotency**: Multiple full backups in same month update the same record
6. **Data Integrity**: Prevents incremental backups without proper base backup

## Validation Rules

### ✅ Full Backup
- Can be performed anytime
- Automatically stores base UUID in database with current month
- If performed multiple times in same month, updates existing record

### ⚠️ Incremental Backup
- **REQUIRES** base UUID to exist in database for current month
- **FAILS** if no base UUID found with error:
  ```
  IllegalStateException: Base backup UUID not found for category 'XXX' and month 'YYYY-MM'.
  Please perform a full backup first for the current month.
  ```
- **Solution**: Perform a full backup first to populate base UUID

### Example Scenario

**Month: February 2026**

1. ❌ **Incremental Backup** → FAILS (no base UUID for Feb 2026)
2. ✅ **Full Backup** → SUCCESS (stores base UUID for Feb 2026)
3. ✅ **Incremental Backup** → SUCCESS (uses base UUID from Feb 2026)
4. ✅ **Incremental Backup** → SUCCESS (uses same base UUID)
5. ✅ **Full Backup** → SUCCESS (updates base UUID for Feb 2026)
6. ✅ **Incremental Backup** → SUCCESS (uses updated base UUID)

**Month: March 2026**

7. ❌ **Incremental Backup** → FAILS (no base UUID for Mar 2026)
8. ✅ **Full Backup** → SUCCESS (stores base UUID for Mar 2026)
9. ✅ **Incremental Backup** → SUCCESS (uses base UUID from Mar 2026)

## Testing

### Test Full Backup

```bash
curl -X POST http://localhost:8989/backupProcess \
  -H "Content-Type: application/json" \
  -d '{
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchTransactionDate": "20260114"
  }'
```

**Verify**:
```sql
SELECT * FROM epricing.base_backup_uuid_tracker 
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL' 
AND backup_month = '2026-01';
```

### Test Incremental Backup (Without Base UUID - Should Fail)

```bash
# This should FAIL if no full backup performed in current month
curl -X POST http://localhost:8989/backupProcess \
  -H "Content-Type: application/json" \
  -d '{
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_INCRE",
    "batchTransactionDate": "20260204"
  }'
```

**Expected Result**:
- Status: 500 Internal Server Error
- Error: "Base backup UUID not found for category 'HWA_EPR_DB_BACKUP_INCRE' and month '2026-02'"

### Test Incremental Backup (With Base UUID - Should Succeed)

```bash
# First perform full backup
curl -X POST http://localhost:8989/backupProcess \
  -H "Content-Type: application/json" \
  -d '{
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchTransactionDate": "20260204"
  }'

# Then perform incremental backup - should succeed
curl -X POST http://localhost:8989/backupProcess \
  -H "Content-Type: application/json" \
  -d '{
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_INCRE",
    "batchTransactionDate": "20260204"
  }'
```

**Expected Result**:
- Status: 200 OK
- Logs: "Using base backup UUID from database: {uuid} for category: HWA_EPR_DB_BACKUP_INCRE, month: 2026-02"

## Migration Notes

1. Run database migration: `V2__create_base_backup_uuid_table.sql`
2. Perform a full backup for each category to populate initial base UUIDs
3. Subsequent incremental backups will use database-stored UUIDs

## Rollback Plan

If issues occur:
1. Revert code changes
2. System will fall back to original behavior (fetching from YBA API)
3. Table can remain in database (no harm)


