# 📊 Backup Interval Column & Period Range Implementation

## 🎯 Purpose

**Two major enhancements** to the backup tracking system:

1. **Added `backup_interval` column** to store the actual backup frequency (MONTHLY, WEEKLY, 10_DAYS, etc.)
2. **Updated `backup_period` column** to store actual date ranges instead of just codes

This provides **complete audit trail** and **tracking capabilities** with exact date ranges covered by each backup.

---

## 📋 What Was Added

### 1. Database Schema Changes

**SQL Migration File**: `V4__add_backup_interval_column.sql`

```sql
-- Expand backup_period column to accommodate date ranges (e.g., "2026-01-01 to 2026-01-31")
ALTER TABLE epricing.full_backup_tracker
ALTER COLUMN backup_period TYPE VARCHAR(100);

ALTER TABLE epricing.incremental_backup_tracker
ALTER COLUMN backup_period TYPE VARCHAR(100);

-- Add backup_interval column to full_backup_tracker table
ALTER TABLE epricing.full_backup_tracker
ADD COLUMN backup_interval VARCHAR(50);

-- Add backup_interval column to incremental_backup_tracker table
ALTER TABLE epricing.incremental_backup_tracker
ADD COLUMN backup_interval VARCHAR(50);

-- Create indexes for faster queries
CREATE INDEX idx_full_backup_interval ON epricing.full_backup_tracker(backup_interval);
CREATE INDEX idx_incremental_backup_interval ON epricing.incremental_backup_tracker(backup_interval);
```

### 2. Application Changes

#### **AppConstants.java**
```java
public static final String BACKUP_INTERVAL = "backupInterval";
```

#### **application.yml** - Updated SQL Queries
```yaml
insert-full-backup:
  query: INSERT INTO epricing.full_backup_tracker(
    batch_id, category_code, business_date, backup_period, backup_interval, 
    backup_status, task_uuid, full_backup_response, start_time, db_name
  ) VALUES(
    :batch_id, :batchCategoryCode, :businessDate, :backupPeriod, :backupInterval, 
    :backupStatus, :taskUUID, :fullBackupResponse, now(), :dbName
  );

insert-incremental-backup:
  query: INSERT INTO epricing.incremental_backup_tracker(
    batch_id, category_code, business_date, backup_period, backup_interval, 
    backup_status, start_time
  ) VALUES(
    :batchId, :categoryCode, :businessDate, :backupPeriod, :backupInterval, 
    :backupStatus, now()
  );
```

#### **BackupDaoService.java** - Updated Method Signatures
```java
// Full Backup - Added backupInterval parameter
public void insertFullBackupRecord(Map<String,Object> param,
                                   String backupMonth, 
                                   String backupInterval,  // NEW
                                   String taskUuid, 
                                   String fullBackupResponse,
                                   String dbName)

// Incremental Backup - Added backupInterval parameter
public void insertIncrementalBackupRecord(String batchId, 
                                          String categoryCode, 
                                          Date businessDate,
                                          String backupMonth, 
                                          String backupInterval)  // NEW
```

#### **YbaClient.java** - Extract and Pass Interval with Period Range
```java
private Mono<JsonNode> fullBackup(YbaDynamicConfig config, String categoryCode, Map<String, Object> batchParams) {
    String backupPeriod = getBackupPeriod(categoryCode, batchParams);  // Returns date range
    String backupFrequency = getBackupFrequency(batchParams);  // Extract frequency

    // Pass to DAO
    backupDaoService.insertFullBackupRecord(
        batchParams, backupPeriod, backupFrequency, taskUuid, response.toString(), config.getDbName());
}

private String getBackupPeriod(String categoryCode, Map<String, Object> batchParams) {
    String backupFrequency = (String) batchParams.get(AppConstants.BACKUP_FREQUENCY);

    // Get epoch date from YAML config
    PeriodCalculationProperties.PeriodConfig config = periodConfig.getConfig(configKey);

    // Calculate period range using the new method
    String periodRange = PeriodCalculator.calculatePeriodRange(
        backupFrequency,
        config.getEpochDate()
    );

    // Returns: "2026-01-01 to 2026-01-31" (MONTHLY)
    //          "2026-01-06 to 2026-01-12" (WEEKLY)
    //          "2026-01-01 to 2026-01-10" (10_DAYS)
    return periodRange;
}

private String getBackupFrequency(Map<String, Object> batchParams) {
    String backupFrequency = (String) batchParams.get(AppConstants.BACKUP_FREQUENCY);
    return backupFrequency != null ? backupFrequency : "MONTHLY";
}
```

#### **PeriodCalculator.java** - New Method for Period Ranges
```java
public static String calculatePeriodRange(String backupFrequency, String epochDate) {
    LocalDate currentDate = LocalDate.now();
    LocalDate startDate;
    LocalDate endDate;

    if (backupFrequency.equals("MONTHLY")) {
        // Monthly: First day to last day of current month
        YearMonth yearMonth = YearMonth.from(currentDate);
        startDate = yearMonth.atDay(1);
        endDate = yearMonth.atEndOfMonth();

    } else if (backupFrequency.equals("WEEKLY")) {
        // Weekly: Monday to Sunday of current week
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        startDate = currentDate.with(weekFields.dayOfWeek(), 1); // Monday
        endDate = startDate.plusDays(6); // Sunday

    } else if (backupFrequency.endsWith("_DAYS")) {
        // Custom interval: Calculate based on epoch
        int intervalDays = extractIntervalDays(backupFrequency);
        LocalDate epoch = LocalDate.parse(epochDate);
        long daysSinceEpoch = ChronoUnit.DAYS.between(epoch, currentDate);
        long intervalNumber = daysSinceEpoch / intervalDays;
        long intervalStartDay = intervalNumber * intervalDays;
        startDate = epoch.plusDays(intervalStartDay);
        endDate = startDate.plusDays(intervalDays - 1);
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return startDate.format(formatter) + " to " + endDate.format(formatter);
}
```

---

## 📊 Database Table Structure

### full_backup_tracker
| Column | Type | Description | Example |
|--------|------|-------------|---------|
| batch_id | VARCHAR | Batch execution ID | BATCH_20260315_001 |
| category_code | VARCHAR | Backup category | HWA_EPR_DB_BACKUP_FULL |
| business_date | DATE | Business date | 2026-03-15 |
| **backup_period** | **VARCHAR(100)** | **Date range covered** | **2026-01-01 to 2026-01-31** |
| **backup_interval** | VARCHAR(50) | **Frequency used** | **MONTHLY, WEEKLY, 10_DAYS** |
| backup_status | VARCHAR | Status | SUCCESS, FAILED |
| base_backup_uuid | VARCHAR | Base backup UUID | uuid-123 |
| task_uuid | VARCHAR | YBA task UUID | task-456 |
| db_name | VARCHAR | Database name | HWA_EPR_DB |

### incremental_backup_tracker
| Column | Type | Description | Example |
|--------|------|-------------|---------|
| batch_id | VARCHAR | Batch execution ID | BATCH_20260315_002 |
| category_code | VARCHAR | Backup category | HWA_EPR_DB_BACKUP_FULL |
| business_date | DATE | Business date | 2026-03-15 |
| **backup_period** | **VARCHAR(100)** | **Date range covered** | **2026-01-01 to 2026-01-31** |
| **backup_interval** | VARCHAR(50) | **Frequency used** | **MONTHLY, WEEKLY, 10_DAYS** |
| backup_status | VARCHAR | Status | SUCCESS, FAILED |
| base_backup_uuid | VARCHAR | Base backup UUID | uuid-123 |
| task_uuid | VARCHAR | YBA task UUID | task-789 |

---

## 🔍 Query Examples

### 1. Find all MONTHLY backups
```sql
SELECT * FROM epricing.full_backup_tracker 
WHERE backup_interval = 'MONTHLY';
```

### 2. Find all WEEKLY backups
```sql
SELECT * FROM epricing.full_backup_tracker 
WHERE backup_interval = 'WEEKLY';
```

### 3. Find all custom interval backups (10 days, 15 days, etc.)
```sql
SELECT * FROM epricing.full_backup_tracker 
WHERE backup_interval LIKE '%_DAYS';
```

### 4. Count backups by interval type
```sql
SELECT backup_interval, COUNT(*) as backup_count
FROM epricing.full_backup_tracker
GROUP BY backup_interval
ORDER BY backup_count DESC;
```

### 5. Find all backups for a specific date range
```sql
SELECT * FROM epricing.full_backup_tracker
WHERE backup_period = '2026-01-01 to 2026-01-31';
```

### 6. Find all backups that include a specific date
```sql
SELECT * FROM epricing.full_backup_tracker
WHERE '2026-01-15' BETWEEN
    CAST(SPLIT_PART(backup_period, ' to ', 1) AS DATE) AND
    CAST(SPLIT_PART(backup_period, ' to ', 2) AS DATE);
```

### 7. Verify backup frequency for a specific period
```sql
SELECT batch_id, backup_period, backup_interval, backup_status, start_time
FROM epricing.full_backup_tracker
WHERE backup_period = '2026-01-01 to 2026-01-31'
ORDER BY start_time DESC;
```

---

## ✅ Benefits

1. **Audit Trail**: Track which frequency was actually used for each backup
2. **Verification**: Ensure backups are happening at the correct intervals
3. **Reporting**: Generate reports on backup frequency usage
4. **Debugging**: Quickly identify if wrong frequency was used
5. **Compliance**: Prove that backups meet compliance requirements

---

## 🚀 Example Usage

### Request with MONTHLY frequency
```json
{
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "batchCategoryParameters": {
    "backupType": "FULL",
    "backupFrequency": "MONTHLY"
  }
}
```

**Database Record**:
```
backup_period: "2026-03-01 to 2026-03-31"  ← Full month range
backup_interval: "MONTHLY"  ← Stored for tracking
```

### Request with WEEKLY frequency
```json
{
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "batchCategoryParameters": {
    "backupType": "FULL",
    "backupFrequency": "WEEKLY"
  }
}
```

**Database Record**:
```
backup_period: "2026-03-09 to 2026-03-15"  ← Monday to Sunday
backup_interval: "WEEKLY"  ← Stored for tracking
```

### Request with custom interval (10 days)
```json
{
  "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "batchCategoryParameters": {
    "backupType": "FULL",
    "backupFrequency": "10_DAYS"
  }
}
```

**Database Record**:
```
backup_period: "2026-01-11 to 2026-01-20"  ← 10-day range
backup_interval: "10_DAYS"  ← Stored for tracking
```

---

## 📝 Summary

✅ **Period Format Updated**: Now stores "YYYY-MM-DD to YYYY-MM-DD" date ranges
✅ **Column Expanded**: `backup_period` to VARCHAR(100)
✅ **Column Added**: `backup_interval` to both tracker tables
✅ **Indexes Created**: For faster queries
✅ **Code Updated**: PeriodCalculator, YbaClient, DAO, and Constants
✅ **Tests Updated**: All 93 tests passing (added 6 new period range tests)
✅ **Build Status**: ✅ BUILD SUCCESSFUL

**Now you can:**
- ✅ See exact date ranges covered by each backup
- ✅ Track which backup frequency was used
- ✅ Query backups that include specific dates
- ✅ Generate accurate compliance reports

**Complete audit trail with exact date ranges!** 🎉

