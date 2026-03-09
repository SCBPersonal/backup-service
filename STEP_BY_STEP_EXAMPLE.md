# 📋 Step-by-Step Example: Full Backup Execution

This document provides a complete step-by-step walkthrough of how a full backup request flows through the backup-orchestrator-service.

---

## 🎯 Scenario: Monthly Full Backup

**Goal:** Execute a full backup on the 1st of every month at 2:00 AM

**Cron Expression:** `0 0 2 1 * *`

---

## 📝 Step 1: User Sends Backup Request

**Endpoint:** `POST http://localhost:10022/backupProcess`

**Request Body:**
```json
{
  "batchId": "BATCH_20260309_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260309",
  "payload": {
    "cronExpression": "0 0 2 1 * *",
    "subCategoryCode": "YUGABYTE_FULL"
  }
}
```

**What happens:**
- Request received by `BackupController.backupProcess()`
- StopWatch starts to measure execution time
- Request forwarded to `BackupService.execute()`

---

## 📝 Step 2: Batch Framework Processing

**Class:** `BackupService.execute()`

**Actions:**
1. **Validate batch parameters:**
   - ✅ `batchId` is not null/empty
   - ✅ `batchCategoryCode` is not null/empty
   - ✅ `batchTransactionDate` is valid (YYYYMMDD format)
   - ✅ `cronExpression` is valid

2. **Extract cron expression from payload:**
   ```java
   String cronExpression = JPathUtils.get(payload, "cronExpression");
   // Result: "0 0 2 1 * *"
   ```

3. **Add to batch parameters:**
   ```java
   batchParams.put("cronExpression", "0 0 2 1 * *");
   ```

4. **Check if batch exists:**
   - Query: `SELECT * FROM batch_execution WHERE batch_id = 'BATCH_20260309_001'`
   - If not exists → Create new batch record with status `IN_PROGRESS`

5. **Call process() method:**
   - Delegates to `YbaClient.backupInitiate()`

---

## 📝 Step 3: Cron Expression Parsing

**Class:** `CronExpressionParser`

**Input:** `"0 0 2 1 * *"`

**Processing:**

1. **Normalize cron expression:**
   ```java
   normalizeCronExpression("0 0 2 1 * *")
   // Input:  "0 0 2 1 * *"
   // Output: "0 0 2 1 * ?" (day-of-week changed to ?)
   ```

2. **Validate cron expression:**
   ```java
   CronExpression.parse("0 0 2 1 * ?")
   // ✅ Valid
   ```

3. **Calculate next execution time:**
   ```java
   getNextExecutionTime("0 0 2 1 * *")
   // Result: 2026-04-01T02:00:00
   ```

4. **Calculate backup period:**
   ```java
   calculateBackupPeriod("0 0 2 1 * *")
   // Format: "yyyy-MM-dd-HHmm"
   // Result: "2026-04-01-0200"
   ```

5. **Calculate backup interval:**
   ```java
   calculateBackupInterval("0 0 2 1 * *")
   // Result: "2026-04-01 02:00:00 (Cron: 0 0 2 1 * *)"
   ```

6. **Determine frequency type:**
   ```java
   determineFrequencyType("0 0 2 1 * *")
   // Result: "MONTHLY"
   ```

---

## 📝 Step 4: YBA Configuration Resolution

**Class:** `YbaConfigService.resolve()`

**Input:** `categoryCode = "HWA_EPR_DB_BACKUP_FULL"`

**Database Query:**
```sql
SELECT * FROM ydb_config_details 
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
```

**Result:**
```java
YbaDynamicConfig {
  backupCategoryType: "FULL",
  fullBackupUrl: "http://yba-server:9000/api/v1/customers/uuid/universes/uuid/backups",
  apiToken: "***",
  universeUuid: "universe-123",
  storageConfigUuid: "storage-456",
  backupType: "PGSQL_TABLE_TYPE",
  expiryMs: 2592000000,  // 30 days
  dbName: "yugabyte"
}
```

---

## 📝 Step 5: Insert Full Backup Record

**Class:** `BackupDaoService.insertFullBackupRecord()`

**Database Insert:**
```sql
INSERT INTO full_backup_tracker (
  batch_id,
  category_code,
  business_date,
  backup_period,
  backup_interval,
  backup_status,
  task_uuid,
  full_backup_response,
  database_name,
  created_date
) VALUES (
  'BATCH_20260309_001',
  'HWA_EPR_DB_BACKUP_FULL',
  '20260309',
  '2026-04-01-0200',
  '2026-04-01 02:00:00 (Cron: 0 0 2 1 * *)',
  'IN_PROGRESS',
  NULL,  -- Will be updated after YBA API call
  NULL,  -- Will be updated after YBA API call
  'yugabyte',
  CURRENT_TIMESTAMP
);
```

**Result:**
- ✅ Record inserted with status `IN_PROGRESS`
- ⏳ Waiting for YBA API response

---

## 📝 Step 6: Call YBA API for Full Backup

**Class:** `YbaClient.fullBackup()`

**HTTP Request:**
```http
POST http://yba-server:9000/api/v1/customers/uuid/universes/uuid/backups
Headers:
  X-AUTH-YW-API-TOKEN: ***
  Content-Type: application/json

Body:
{
  "universeUUID": "universe-123",
  "storageConfigUUID": "storage-456",
  "backupType": "PGSQL_TABLE_TYPE",
  "timeBeforeDelete": 2592000000
}
```

**YBA Response:**
```json
{
  "taskUUID": "task-abc-123",
  "resourceUUID": "backup-xyz-789",
  "status": "Running"
}
```

---

## 📝 Step 7: Update Full Backup Record with Task UUID

**Database Update:**
```sql
UPDATE full_backup_tracker
SET task_uuid = 'task-abc-123',
    full_backup_response = '{"taskUUID":"task-abc-123","resourceUUID":"backup-xyz-789","status":"Running"}'
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
  AND backup_period = '2026-04-01-0200'
  AND batch_id = 'BATCH_20260309_001';
```

**Result:**
- ✅ Task UUID stored
- ✅ Full response stored for audit
- ⏳ Backup job running in YBA

---

## 📝 Step 8: Start Async Polling (Background Thread)

**Class:** `BackupPollerService.pollFullBackupCompletion()`

**Configuration:**
```yaml
backup.poller:
  enabled: true
  initial-delay-ms: 5000      # Wait 5 seconds before first poll
  interval-ms: 30000          # Poll every 30 seconds
  max-attempts: 120           # Max 120 attempts (1 hour)
```

**Polling Loop:**

### Poll #1 (After 5 seconds):
```http
GET http://yba-server:9000/api/v1/customers/uuid/tasks/task-abc-123
```
**Response:**
```json
{
  "status": "Running",
  "percent": 25
}
```
**Action:** Continue polling...

### Poll #2 (After 35 seconds):
```http
GET http://yba-server:9000/api/v1/customers/uuid/tasks/task-abc-123
```
**Response:**
```json
{
  "status": "Running",
  "percent": 50
}
```
**Action:** Continue polling...

### Poll #3 (After 65 seconds):
```http
GET http://yba-server:9000/api/v1/customers/uuid/tasks/task-abc-123
```
**Response:**
```json
{
  "status": "Success",
  "percent": 100
}
```
**Action:** ✅ Backup completed! Proceed to fetch base UUID...

---

## 📝 Step 9: Fetch Base Backup UUID

**Class:** `BackupPollerService.fetchLastBackupDetailsReactive()`

**HTTP Request:**
```http
POST http://yba-server:9000/api/v1/customers/uuid/universes/uuid/backups/page
Headers:
  X-AUTH-YW-API-TOKEN: ***
  Content-Type: application/json

Body:
{
  "storageConfigUUID": "storage-456",
  "backupType": "PGSQL_TABLE_TYPE",
  "direction": "DESC",
  "sortBy": "createTime",
  "limit": 1,
  "filter": {
    "universeUUIDList": ["universe-123"]
  }
}
```

**YBA Response:**
```json
{
  "entities": [
    {
      "commonBackupInfo": {
        "baseBackupUUID": "base-backup-uuid-456",
        "taskUUID": "task-abc-123",
        "createTime": "2026-03-09T10:30:00Z"
      }
    }
  ]
}
```

**Extracted:**
- `baseBackupUUID`: `"base-backup-uuid-456"`
- `taskUUID`: `"task-abc-123"`

**Validation:**
- ✅ Task UUID matches expected: `task-abc-123`
- ✅ Base UUID found

---

## 📝 Step 10: Update Full Backup with Base UUID

**Database Update:**
```sql
UPDATE full_backup_tracker
SET base_backup_uuid = 'base-backup-uuid-456',
    backup_status = 'SUCCESS',
    updated_date = CURRENT_TIMESTAMP
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
  AND backup_period = '2026-04-01-0200'
  AND batch_id = 'BATCH_20260309_001';
```

**Final Record:**
```
batch_id: BATCH_20260309_001
category_code: HWA_EPR_DB_BACKUP_FULL
business_date: 20260309
backup_period: 2026-04-01-0200
backup_interval: 2026-04-01 02:00:00 (Cron: 0 0 2 1 * *)
backup_status: SUCCESS ✅
task_uuid: task-abc-123
base_backup_uuid: base-backup-uuid-456
full_backup_response: {"taskUUID":"task-abc-123",...}
database_name: yugabyte
created_date: 2026-03-09 10:25:00
updated_date: 2026-03-09 10:31:00
```

---

## 📝 Step 11: Update Batch Execution Status

**Class:** `BackupService.handleBackupSuccess()`

**Database Update:**
```sql
UPDATE batch_execution
SET execution_status = 'SUCCESS',
    end_time = CURRENT_TIMESTAMP
WHERE batch_id = 'BATCH_20260309_001'
  AND batch_category_code = 'HWA_EPR_DB_BACKUP_FULL';
```

---

## 📝 Step 12: Return Response to User

**Response:**
```json
{
  "batchId": "BATCH_20260309_001",
  "executionStatus": "SUCCESS",
  "extensionFields": {
    "taskUUID": "task-abc-123",
    "baseBackupUUID": "base-backup-uuid-456"
  }
}
```

**Logs:**
```
[INFO] backup request received for db backup for job Type-{...}
[INFO] Calculated backup period: 2026-04-01-0200 for category: HWA_EPR_DB_BACKUP_FULL with cron: 0 0 2 1 * *
[INFO] Inserting full backup record with response for dbName: yugabyte, month: 2026-04-01-0200
[INFO] Starting reactive backup poller for FULL backup - category: HWA_EPR_DB_BACKUP_FULL, task: task-abc-123
[INFO] Polling YBA task status: task-abc-123 (attempt 1/120)
[INFO] Polling YBA task status: task-abc-123 (attempt 2/120)
[INFO] Polling YBA task status: task-abc-123 (attempt 3/120)
[INFO] Full backup job completed successfully for task: task-abc-123
[INFO] Task UUID validation successful. Expected: task-abc-123, Fetched: task-abc-123
[INFO] Successfully updated full backup with base UUID: base-backup-uuid-456 for category: HWA_EPR_DB_BACKUP_FULL
[INFO] Completion time - 6.5 sec
```

---

## ✅ Summary

| Step | Component | Duration | Status |
|------|-----------|----------|--------|
| 1 | Request received | 0s | ✅ |
| 2 | Batch validation | 0.1s | ✅ |
| 3 | Cron parsing | 0.05s | ✅ |
| 4 | Config resolution | 0.2s | ✅ |
| 5 | Insert tracker record | 0.1s | ✅ |
| 6 | YBA API call | 0.5s | ✅ |
| 7 | Update task UUID | 0.1s | ✅ |
| 8-9 | Async polling (3 polls) | 65s | ✅ |
| 10 | Update base UUID | 0.1s | ✅ |
| 11 | Update batch status | 0.1s | ✅ |
| 12 | Return response | 0.05s | ✅ |
| **TOTAL** | | **~66s** | **✅ SUCCESS** |

---

## 🔍 Database State After Completion

### batch_execution table:
```
batch_id: BATCH_20260309_001
batch_category_code: HWA_EPR_DB_BACKUP_FULL
execution_status: SUCCESS
start_time: 2026-03-09 10:25:00
end_time: 2026-03-09 10:26:06
```

### full_backup_tracker table:
```
batch_id: BATCH_20260309_001
category_code: HWA_EPR_DB_BACKUP_FULL
backup_period: 2026-04-01-0200
backup_status: SUCCESS
base_backup_uuid: base-backup-uuid-456 ← Used for incremental backups
task_uuid: task-abc-123
```

---

## 🎯 Next Steps

This base backup UUID (`base-backup-uuid-456`) will be used for:
- **Incremental backups** in the same backup period (`2026-04-01-0200`)
- **Querying** from `full_backup_tracker` when incremental backup is requested
- **Validation** to ensure incremental backups have a valid base

---

---

# 📋 Example 2: Custom Date Backup Execution

This example shows how to execute a backup on a **specific custom date** (e.g., June 15th at 3:00 AM).

---

## 🎯 Scenario: Custom Date Backup

**Goal:** Execute a backup on June 15, 2026 at 3:00 AM (one-time or yearly)

**Cron Expression:** `0 0 3 15 6 *`

**Breakdown:**
- `0` - Second: 0
- `0` - Minute: 0
- `3` - Hour: 3 AM
- `15` - Day of month: 15th
- `6` - Month: June
- `*` - Day of week: Any

---

## 📝 Step 1: User Sends Custom Date Backup Request

**Endpoint:** `POST http://localhost:10022/backupProcess`

**Request Body:**
```json
{
  "batchId": "BATCH_20260309_002",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260309",
  "payload": {
    "cronExpression": "0 0 3 15 6 *",
    "subCategoryCode": "YUGABYTE_FULL"
  }
}
```

---

## 📝 Step 2: Cron Expression Parsing (Custom Date)

**Class:** `CronExpressionParser`

**Input:** `"0 0 3 15 6 *"`

**Processing:**

1. **Normalize cron expression:**
   ```java
   normalizeCronExpression("0 0 3 15 6 *")
   // Input:  "0 0 3 15 6 *"
   // Output: "0 0 3 15 6 ?" (day-of-week changed to ? because day-of-month is specified)
   ```

2. **Validate cron expression:**
   ```java
   CronExpression.parse("0 0 3 15 6 ?")
   // ✅ Valid
   ```

3. **Calculate next execution time:**
   ```java
   getNextExecutionTime("0 0 3 15 6 *")
   // Current date: 2026-03-09
   // Result: 2026-06-15T03:00:00
   ```

4. **Calculate backup period:**
   ```java
   calculateBackupPeriod("0 0 3 15 6 *")
   // Format: "yyyy-MM-dd-HHmm"
   // Result: "2026-06-15-0300"
   ```

5. **Calculate backup interval:**
   ```java
   calculateBackupInterval("0 0 3 15 6 *")
   // Result: "2026-06-15 03:00:00 (Cron: 0 0 3 15 6 *)"
   ```

6. **Determine frequency type:**
   ```java
   determineFrequencyType("0 0 3 15 6 *")
   // Day of month: 15 (specific)
   // Month: 6 (specific)
   // Result: "CUSTOM"
   ```

---

## 📝 Step 3: Insert Full Backup Record (Custom Date)

**Database Insert:**
```sql
INSERT INTO full_backup_tracker (
  batch_id,
  category_code,
  business_date,
  backup_period,
  backup_interval,
  backup_status,
  task_uuid,
  full_backup_response,
  database_name,
  created_date
) VALUES (
  'BATCH_20260309_002',
  'HWA_EPR_DB_BACKUP_FULL',
  '20260309',
  '2026-06-15-0300',                              -- Custom date period
  '2026-06-15 03:00:00 (Cron: 0 0 3 15 6 *)',    -- Custom date interval
  'IN_PROGRESS',
  NULL,
  NULL,
  'yugabyte',
  CURRENT_TIMESTAMP
);
```

**Key Differences from Monthly:**
- `backup_period`: `"2026-06-15-0300"` (includes specific date)
- `backup_interval`: Shows the exact custom date and time
- Frequency type: `CUSTOM` instead of `MONTHLY`

---

## 📝 Step 4: YBA API Call (Same as Monthly)

**HTTP Request:**
```http
POST http://yba-server:9000/api/v1/customers/uuid/universes/uuid/backups
Headers:
  X-AUTH-YW-API-TOKEN: ***
  Content-Type: application/json

Body:
{
  "universeUUID": "universe-123",
  "storageConfigUUID": "storage-456",
  "backupType": "PGSQL_TABLE_TYPE",
  "timeBeforeDelete": 2592000000
}
```

**YBA Response:**
```json
{
  "taskUUID": "task-custom-456",
  "resourceUUID": "backup-custom-789",
  "status": "Running"
}
```

---

## 📝 Step 5: Update with Task UUID

**Database Update:**
```sql
UPDATE full_backup_tracker
SET task_uuid = 'task-custom-456',
    full_backup_response = '{"taskUUID":"task-custom-456","resourceUUID":"backup-custom-789","status":"Running"}'
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
  AND backup_period = '2026-06-15-0300'
  AND batch_id = 'BATCH_20260309_002';
```

---

## 📝 Step 6: Async Polling & Completion

**Polling completes after ~60 seconds**

**Final Database Update:**
```sql
UPDATE full_backup_tracker
SET base_backup_uuid = 'base-custom-uuid-789',
    backup_status = 'SUCCESS',
    updated_date = CURRENT_TIMESTAMP
WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
  AND backup_period = '2026-06-15-0300'
  AND batch_id = 'BATCH_20260309_002';
```

---

## 📝 Final Record (Custom Date Backup)

```
batch_id: BATCH_20260309_002
category_code: HWA_EPR_DB_BACKUP_FULL
business_date: 20260309
backup_period: 2026-06-15-0300                              ← Custom date
backup_interval: 2026-06-15 03:00:00 (Cron: 0 0 3 15 6 *)  ← Custom date
backup_status: SUCCESS ✅
task_uuid: task-custom-456
base_backup_uuid: base-custom-uuid-789
full_backup_response: {"taskUUID":"task-custom-456",...}
database_name: yugabyte
created_date: 2026-03-09 10:35:00
updated_date: 2026-03-09 10:36:05
```

---

## 🔍 Comparison: Monthly vs Custom Date

| Aspect | Monthly Backup | Custom Date Backup |
|--------|----------------|-------------------|
| **Cron Expression** | `0 0 2 1 * *` | `0 0 3 15 6 *` |
| **Frequency Type** | `MONTHLY` | `CUSTOM` |
| **Backup Period** | `2026-04-01-0200` | `2026-06-15-0300` |
| **Next Execution** | 1st of every month | June 15th (yearly) |
| **Use Case** | Regular monthly backups | Specific date backups (e.g., end of fiscal year) |

---

## 📊 More Custom Date Examples

### Example 1: Year-End Backup (December 31st at 11:59 PM)
```json
{
  "cronExpression": "0 59 23 31 12 *"
}
```
**Result:**
- Backup Period: `2026-12-31-2359`
- Frequency Type: `CUSTOM`
- Next Execution: `2026-12-31T23:59:00`

### Example 2: Quarter-End Backup (March 31st at 6:00 AM)
```json
{
  "cronExpression": "0 0 6 31 3 *"
}
```
**Result:**
- Backup Period: `2026-03-31-0600`
- Frequency Type: `CUSTOM`
- Next Execution: `2026-03-31T06:00:00`

### Example 3: Mid-Year Backup (July 1st at 12:00 AM)
```json
{
  "cronExpression": "0 0 0 1 7 *"
}
```
**Result:**
- Backup Period: `2026-07-01-0000`
- Frequency Type: `CUSTOM`
- Next Execution: `2026-07-01T00:00:00`

---

## 🎯 When to Use Custom Date Backups

✅ **Good Use Cases:**
- **Fiscal year-end backups** (e.g., March 31st, December 31st)
- **Compliance deadlines** (e.g., quarterly reporting dates)
- **Project milestones** (e.g., product launch date)
- **Audit requirements** (e.g., specific regulatory dates)
- **One-time backups** (e.g., before major system upgrade)

❌ **Not Recommended For:**
- Regular recurring backups → Use `MONTHLY` or `WEEKLY` instead
- Daily backups → Use daily cron expression
- Ad-hoc backups → Trigger manually without cron

---

## 🔄 Incremental Backup with Custom Date

**Scenario:** Run incremental backup on June 20th (5 days after full backup)

**Request:**
```json
{
  "batchId": "BATCH_20260620_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_INCREMENTAL",
  "batchTransactionDate": "20260620",
  "payload": {
    "cronExpression": "0 0 3 15 6 *"
  }
}
```

**Processing:**
1. Calculate backup period: `2026-06-15-0300` (same as full backup)
2. Query `full_backup_tracker` for base UUID:
   ```sql
   SELECT base_backup_uuid
   FROM full_backup_tracker
   WHERE category_code = 'HWA_EPR_DB_BACKUP_FULL'
     AND backup_period = '2026-06-15-0300'
     AND backup_status = 'SUCCESS';
   ```
3. Result: `base-custom-uuid-789`
4. Use this base UUID for incremental backup

**Key Point:** All incremental backups in the same period (`2026-06-15-0300`) will use the same base backup UUID.

---

**End of Examples** ✅

