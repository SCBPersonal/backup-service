# Testing Guide: Configurable Backup Frequency

## 📋 Prerequisites

1. **Configure frequency in application.yml**
2. **Start the application**
3. **Have API endpoint ready** (e.g., `http://localhost:8080/api/backup`)

---

## 🧪 Test Scenario 1: MONTHLY Frequency

### Step 1: Configure in application.yml

```yaml
backup:
  frequency:
    databases:
      HWA_EPR_DB_BACKUP_FULL:
        type: MONTHLY
```

### Step 2: Trigger Full Backup (March 1, 2026)

**Request:**
```json
{
  "batchId": "BATCH_20260301_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260301",
  "payload": {
    "backupType": "FULL"
  }
}
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_20260301_001",
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchTransactionDate": "20260301",
    "payload": {
      "backupType": "FULL"
    }
  }'
```

**Expected Log Output:**
```
INFO: Calculated backup period: 2026-03 for category: HWA_EPR_DB_BACKUP_FULL with frequency type: MONTHLY
INFO: Inserting full backup record with response for dbName: HWA_EPR_DB, month: 2026-03
INFO: Full backup record with response inserted successfully for category: HWA_EPR_DB_BACKUP_FULL
```

**Expected Database Record (full_backup_tracker):**
```sql
SELECT * FROM epricing.full_backup_tracker 
WHERE batch_id = 'BATCH_20260301_001';

-- Result:
-- batch_id: BATCH_20260301_001
-- category_code: HWA_EPR_DB_BACKUP_FULL
-- backup_month: 2026-03  ← MONTHLY format
-- backup_status: SUCCESS
-- base_backup_uuid: <generated-uuid>
```

---

### Step 3: Trigger Incremental Backup (March 15, 2026)

**Request:**
```json
{
  "batchId": "BATCH_20260315_002",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL"
  }
}
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_20260315_002",
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchTransactionDate": "20260315",
    "payload": {
      "backupType": "INCREMENTAL"
    }
  }'
```

**Expected Log Output:**
```
INFO: Calculated backup period: 2026-03 for category: HWA_EPR_DB_BACKUP_FULL with frequency type: MONTHLY
INFO: Retrieving base backup UUID for month: 2026-03
INFO: Base backup UUID retrieved: <uuid-from-march-1>
INFO: Using base backup UUID from full_backup_tracker: <uuid-from-march-1> for category: HWA_EPR_DB_BACKUP_FULL, period: 2026-03
```

**Expected Database Record (incremental_backup_tracker):**
```sql
SELECT * FROM epricing.incremental_backup_tracker 
WHERE batch_id = 'BATCH_20260315_002';

-- Result:
-- batch_id: BATCH_20260315_002
-- category_code: HWA_EPR_DB_BACKUP_FULL
-- backup_month: 2026-03  ← Same as full backup
-- base_backup_uuid: <uuid-from-march-1>
-- backup_status: SUCCESS
```

---

## 🧪 Test Scenario 2: WEEKLY Frequency

### Step 1: Configure in application.yml

```yaml
backup:
  frequency:
    databases:
      UAM_DB_BACKUP_FULL:
        type: WEEKLY
```

### Step 2: Trigger Full Backup (Monday, March 9, 2026 - Week 11)

**Request:**
```json
{
  "batchId": "BATCH_20260309_W11",
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260309",
  "payload": {
    "backupType": "FULL"
  }
}
```

**cURL Command:**
```bash
curl -X POST http://localhost:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_20260309_W11",
    "batchCategoryCode": "UAM_DB_BACKUP_FULL",
    "batchTransactionDate": "20260309",
    "payload": {
      "backupType": "FULL"
    }
  }'
```

**Expected Log Output:**
```
INFO: Calculated backup period: 2026-W11 for category: UAM_DB_BACKUP_FULL with frequency type: WEEKLY
INFO: Inserting full backup record with response for dbName: UAM_DB, month: 2026-W11
INFO: Full backup record with response inserted successfully for category: UAM_DB_BACKUP_FULL
```

**Expected Database Record:**
```sql
SELECT * FROM epricing.full_backup_tracker 
WHERE batch_id = 'BATCH_20260309_W11';

-- Result:
-- batch_id: BATCH_20260309_W11
-- category_code: UAM_DB_BACKUP_FULL
-- backup_month: 2026-W11  ← WEEKLY format
-- backup_status: SUCCESS
```

---

### Step 3: Trigger Incremental Backup (Thursday, March 12, 2026 - Week 11)

**Request:**
```json
{
  "batchId": "BATCH_20260312_W11_INC",
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260312",
  "payload": {
    "backupType": "INCREMENTAL"
  }
}
```

**Expected Log Output:**
```
INFO: Calculated backup period: 2026-W11 for category: UAM_DB_BACKUP_FULL with frequency type: WEEKLY
INFO: Retrieving base backup UUID for month: 2026-W11
INFO: Base backup UUID retrieved: <uuid-from-week11>
INFO: Using base backup UUID from full_backup_tracker: <uuid-from-week11> for category: UAM_DB_BACKUP_FULL, period: 2026-W11
```

---

### Step 4: Trigger Full Backup (Monday, March 16, 2026 - Week 12 - NEW WEEK!)

**Request:**
```json
{
  "batchId": "BATCH_20260316_W12",
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260316",
  "payload": {
    "backupType": "FULL"
  }
}
```

**Expected Log Output:**
```
INFO: Calculated backup period: 2026-W12 for category: UAM_DB_BACKUP_FULL with frequency type: WEEKLY
INFO: Inserting full backup record with response for dbName: UAM_DB, month: 2026-W12
```

**Expected Database Record:**
```sql
SELECT * FROM epricing.full_backup_tracker 
WHERE category_code = 'UAM_DB_BACKUP_FULL'
ORDER BY backup_month;

-- Results:
-- backup_month: 2026-W11  ← Week 11 full backup
-- backup_month: 2026-W12  ← Week 12 full backup (NEW!)
```

---

## 🧪 Test Scenario 3: CUSTOM Frequency (10 Days)

### Step 1: Configure in application.yml

```yaml
backup:
  frequency:
    databases:
      COMPLIANCE_DB_BACKUP_FULL:
        type: CUSTOM
        interval-days: 10
```

### Step 2: Trigger Full Backup (March 11, 2026)

**Request:**
```json
{
  "batchId": "BATCH_20260311_D11",
  "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
  "batchTransactionDate": "20260311",
  "payload": {
    "backupType": "FULL"
  }
}
```

**Expected Log Output:**
```
INFO: Calculated backup period: 2026-03-11 for category: COMPLIANCE_DB_BACKUP_FULL with frequency type: CUSTOM
INFO: Inserting full backup record with response for dbName: COMPLIANCE_DB, month: 2026-03-11
```

**Expected Database Record:**
```sql
-- backup_month: 2026-03-11  ← CUSTOM format (interval start date)
```

---

### Step 3: Trigger Incremental Backup (March 15, 2026 - Same Interval)

**Request:**
```json
{
  "batchId": "BATCH_20260315_D11_INC",
  "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL"
  }
}
```

**Expected Log Output:**
```
INFO: Calculated backup period: 2026-03-11 for category: COMPLIANCE_DB_BACKUP_FULL with frequency type: CUSTOM
INFO: Retrieving base backup UUID for month: 2026-03-11
INFO: Using base backup UUID from full_backup_tracker: <uuid> for category: COMPLIANCE_DB_BACKUP_FULL, period: 2026-03-11
```

**Note:** March 15 falls in the same 10-day interval (days 71-80), so it uses period `2026-03-11`

---

### Step 4: Trigger Full Backup (March 21, 2026 - NEW Interval!)

**Request:**
```json
{
  "batchId": "BATCH_20260321_D21",
  "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
  "batchTransactionDate": "20260321",
  "payload": {
    "backupType": "FULL"
  }
}
```

**Expected Log Output:**
```
INFO: Calculated backup period: 2026-03-21 for category: COMPLIANCE_DB_BACKUP_FULL with frequency type: CUSTOM
INFO: Inserting full backup record with response for dbName: COMPLIANCE_DB, month: 2026-03-21
```

**Expected Database Records:**
```sql
SELECT backup_month FROM epricing.full_backup_tracker 
WHERE category_code = 'COMPLIANCE_DB_BACKUP_FULL'
ORDER BY backup_month;

-- Results:
-- 2026-03-11  ← First 10-day interval
-- 2026-03-21  ← Second 10-day interval (NEW!)
```

---

## 🔍 Verification Queries

### Check All Backup Periods
```sql
SELECT 
  category_code,
  backup_month,
  backup_status,
  COUNT(*) as backup_count
FROM epricing.full_backup_tracker
GROUP BY category_code, backup_month, backup_status
ORDER BY category_code, backup_month;
```

### Expected Results:
```
category_code              | backup_month | backup_status | backup_count
---------------------------|--------------|---------------|-------------
HWA_EPR_DB_BACKUP_FULL    | 2026-03      | SUCCESS       | 1
UAM_DB_BACKUP_FULL        | 2026-W11     | SUCCESS       | 1
UAM_DB_BACKUP_FULL        | 2026-W12     | SUCCESS       | 1
COMPLIANCE_DB_BACKUP_FULL | 2026-03-11   | SUCCESS       | 1
COMPLIANCE_DB_BACKUP_FULL | 2026-03-21   | SUCCESS       | 1
```

---

## ✅ Success Criteria

For each test scenario, verify:

1. ✅ **Logs show correct period format**
   - MONTHLY: `2026-03`
   - WEEKLY: `2026-W11`
   - CUSTOM: `2026-03-11`

2. ✅ **Database records use correct backup_month**
   - Check `full_backup_tracker` table
   - Check `incremental_backup_tracker` table

3. ✅ **Incremental backups find correct base UUID**
   - Query uses the calculated period
   - Returns UUID from the correct full backup

4. ✅ **Period transitions work correctly**
   - New month → New full backup (MONTHLY)
   - New week → New full backup (WEEKLY)
   - New interval → New full backup (CUSTOM)

---

## 🚨 Error Scenarios to Test

### Test 1: Incremental Without Full Backup

**Request:**
```json
{
  "batchId": "BATCH_ERROR_001",
  "batchCategoryCode": "NEW_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL"
  }
}
```

**Expected Error:**
```
WARN: No base backup UUID found for category: 2026-03, month: 2026-03
ERROR: Base backup UUID not found for category 'NEW_DB_BACKUP_FULL' and period '2026-03'
```

**Solution:** Trigger full backup first

---

## 📊 Summary Table

| Frequency | Period Format | Example | Full Backup Trigger |
|-----------|--------------|---------|-------------------|
| MONTHLY | `YYYY-MM` | `2026-03` | 1st of each month |
| WEEKLY | `YYYY-Www` | `2026-W11` | Every Monday |
| CUSTOM (10 days) | `YYYY-MM-DD` | `2026-03-11` | Every 10 days from epoch |

---

**For complete JSON samples, see:** `SAMPLE_BACKUP_REQUESTS.json`

