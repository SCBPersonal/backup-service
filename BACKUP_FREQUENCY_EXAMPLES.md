# Backup Frequency - Real-World Examples

## 📚 Understanding the YAML-Driven Logic

### Old Logic (Before)
- **Fixed**: Full backup always happened once per month
- **Period**: Always stored as `YYYY-MM` (e.g., "2026-03")
- **Problem**: No flexibility - couldn't do weekly or custom intervals

### New Logic (After - YAML-Driven with Single Parameter)
- **Request-Based**: Single `backupFrequency` parameter in every backup request
- **YAML Configuration**: Only 3 configs needed - dynamic interval support!
- **Period Formats**:
  - MONTHLY: `YYYY-MM` (e.g., "2026-03")
  - WEEKLY: `YYYY-Www` (e.g., "2026-W11")
  - N_DAYS: `YYYY-MM-DD` (e.g., "10_DAYS" → "2026-03-11")
- **Benefit**: Single parameter, fully dynamic, no code changes needed!

---

## 🎯 Scenario 1: Monthly Backups (Default - Same as Before)

### YAML Configuration (application.yml)
```yaml
backup:
  period-calculation:
    configs:
      MONTHLY:
        format: "yyyy-MM"
        description: "Monthly backup period - format: YYYY-MM"
```

### Request Format
```json
{
  "batchId": "BATCH_20260315_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "backupFrequency": "MONTHLY"
  }
}
```

### How It Works

**Timeline: March 2026**

| Date | Request Type | What Happens | backup_period Value | base_backup_uuid |
|------|-------------|--------------|---------------------|------------------|
| Mar 1 | Full Backup | Creates new full backup | `2026-03` | `uuid-123` |
| Mar 5 | Incremental | Uses base from `2026-03` | `2026-03` | `uuid-123` |
| Mar 10 | Incremental | Uses base from `2026-03` | `2026-03` | `uuid-123` |
| Mar 20 | Incremental | Uses base from `2026-03` | `2026-03` | `uuid-123` |
| Mar 31 | Incremental | Uses base from `2026-03` | `2026-03` | `uuid-123` |

**Timeline: April 2026**

| Date | Request Type | What Happens | backup_period Value | base_backup_uuid |
|------|-------------|--------------|---------------------|------------------|
| Apr 1 | Full Backup | Creates NEW full backup | `2026-04` | `uuid-456` |
| Apr 5 | Incremental | Uses base from `2026-04` | `2026-04` | `uuid-456` |
| Apr 15 | Incremental | Uses base from `2026-04` | `2026-04` | `uuid-456` |

**Key Points:**
- ✅ All backups in March use period `2026-03`
- ✅ All backups in April use period `2026-04`
- ✅ Each month has ONE full backup
- ✅ Incremental backups reference the full backup from the same month

---

## 🎯 Scenario 2: Weekly Backups (New Feature)

### YAML Configuration (application.yml)
```yaml
backup:
  period-calculation:
    configs:
      WEEKLY:
        format: "yyyy-'W'ww"
        description: "Weekly backup period - format: YYYY-Www (ISO week)"
```

### Request Format
```json
{
  "batchId": "BATCH_20260309_001",
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260309",
  "payload": {
    "backupType": "FULL",
    "backupFrequency": "WEEKLY"
  }
}
```

### How It Works

**Timeline: March 2026**

| Date | Week | Request Type | What Happens | backup_period Value | base_backup_uuid |
|------|------|-------------|--------------|---------------------|------------------|
| Mar 2 (Mon) | W10 | Full Backup | Creates new full backup | `2026-W10` | `uuid-w10` |
| Mar 3 (Tue) | W10 | Incremental | Uses base from `2026-W10` | `2026-W10` | `uuid-w10` |
| Mar 5 (Thu) | W10 | Incremental | Uses base from `2026-W10` | `2026-W10` | `uuid-w10` |
| Mar 7 (Sat) | W10 | Incremental | Uses base from `2026-W10` | `2026-W10` | `uuid-w10` |
| | | | | | |
| Mar 9 (Mon) | W11 | Full Backup | Creates NEW full backup | `2026-W11` | `uuid-w11` |
| Mar 10 (Tue) | W11 | Incremental | Uses base from `2026-W11` | `2026-W11` | `uuid-w11` |
| Mar 12 (Thu) | W11 | Incremental | Uses base from `2026-W11` | `2026-W11` | `uuid-w11` |
| Mar 14 (Sat) | W11 | Incremental | Uses base from `2026-W11` | `2026-W11` | `uuid-w11` |
| | | | | | |
| Mar 16 (Mon) | W12 | Full Backup | Creates NEW full backup | `2026-W12` | `uuid-w12` |
| Mar 18 (Wed) | W12 | Incremental | Uses base from `2026-W12` | `2026-W12` | `uuid-w12` |

**Key Points:**
- ✅ Each week has ONE full backup
- ✅ Week changes every Monday (ISO week standard)
- ✅ Incremental backups reference the full backup from the same week
- ✅ More frequent full backups = better recovery points

---

## 🎯 Scenario 3: Custom 10-Day Intervals (New Feature - Dynamic!)

### YAML Configuration (application.yml)
```yaml
backup:
  period-calculation:
    configs:
      # Single CUSTOM config handles ALL intervals (10, 15, 20 days, etc.)
      CUSTOM:
        format: "yyyy-MM-dd"
        description: "Custom interval backup period - format: YYYY-MM-DD (dynamic)"
        epoch-date: "2026-01-01"  # Reference date for interval calculation
```

### Request Format (10-Day Interval)
```json
{
  "batchId": "BATCH_20260111_001",
  "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
  "batchTransactionDate": "20260111",
  "payload": {
    "backupType": "FULL",
    "backupFrequency": "10_DAYS"
  }
}
```

### Request Format (15-Day Interval)
```json
{
  "batchId": "BATCH_20260111_002",
  "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
  "batchTransactionDate": "20260111",
  "payload": {
    "backupType": "FULL",
    "backupFrequency": "15_DAYS"
  }
}
```

**Note**: The interval (10, 15, 20, etc.) is extracted from the `backupFrequency` value. No need for separate `intervalDays` parameter!

### How It Works

**Timeline: January 2026 (Starting from epoch 2026-01-01)**

| Date | Interval | Request Type | What Happens | backup_period Value | base_backup_uuid |
|------|----------|-------------|--------------|---------------------|------------------|
| Jan 1 | Days 1-10 | Full Backup | Creates new full backup | `2026-01-01` | `uuid-d1` |
| Jan 3 | Days 1-10 | Incremental | Uses base from `2026-01-01` | `2026-01-01` | `uuid-d1` |
| Jan 7 | Days 1-10 | Incremental | Uses base from `2026-01-01` | `2026-01-01` | `uuid-d1` |
| Jan 10 | Days 1-10 | Incremental | Uses base from `2026-01-01` | `2026-01-01` | `uuid-d1` |
| | | | | | |
| Jan 11 | Days 11-20 | Full Backup | Creates NEW full backup | `2026-01-11` | `uuid-d11` |
| Jan 13 | Days 11-20 | Incremental | Uses base from `2026-01-11` | `2026-01-11` | `uuid-d11` |
| Jan 17 | Days 11-20 | Incremental | Uses base from `2026-01-11` | `2026-01-11` | `uuid-d11` |
| Jan 20 | Days 11-20 | Incremental | Uses base from `2026-01-11` | `2026-01-11` | `uuid-d11` |
| | | | | | |
| Jan 21 | Days 21-30 | Full Backup | Creates NEW full backup | `2026-01-21` | `uuid-d21` |
| Jan 25 | Days 21-30 | Incremental | Uses base from `2026-01-21` | `2026-01-21` | `uuid-d21` |
| Jan 28 | Days 21-30 | Incremental | Uses base from `2026-01-21` | `2026-01-21` | `uuid-d21` |
| | | | | | |
| Jan 31 | Days 31-40 | Full Backup | Creates NEW full backup | `2026-01-31` | `uuid-d31` |
| Feb 3 | Days 31-40 | Incremental | Uses base from `2026-01-31` | `2026-01-31` | `uuid-d31` |

**Key Points:**
- ✅ Every 10 days, a new full backup is created
- ✅ Period represents the START date of the 10-day interval
- ✅ Incremental backups reference the full backup from the same interval
- ✅ Useful for compliance requirements (e.g., "backup every 10 days")

---

## 🔄 Complete Flow Example

### Example: Processing a Backup Request

**Input Request:**
```json
{
  "batchId": "BATCH_20260315_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "backupFrequency": "MONTHLY"
  }
}
```

**YAML Configuration:**
```yaml
backup:
  period-calculation:
    configs:
      MONTHLY:
        format: "yyyy-MM"
```

**Step-by-Step Flow:**

```
1. Request arrives: INCREMENTAL backup for HWA_EPR_DB_BACKUP_FULL
   Date: March 15, 2026
   Frequency Type: MONTHLY (from request payload)

2. System reads YAML configuration:
   ✓ Found: MONTHLY config with format "yyyy-MM"

3. System calculates backup period:
   ✓ PeriodCalculator.calculatePeriod()
   ✓ Frequency type: MONTHLY (from request)
   ✓ Format pattern: "yyyy-MM" (from YAML)
   ✓ Current date: 2026-03-15
   ✓ Calculated period: "2026-03"

4. System queries database for base backup UUID:
   ✓ Query: SELECT base_backup_uuid FROM full_backup_tracker
            WHERE backup_period = '2026-03' AND backup_status = 'SUCCESS'
   ✓ Result: "base-uuid-march-2026"

5. System performs incremental backup:
   ✓ Calls YBA API with base UUID: "base-uuid-march-2026"
   ✓ Inserts record into incremental_backup_tracker:
      - batch_id: "BATCH_20260315_001"
      - category_code: "HWA_EPR_DB_BACKUP_FULL"
      - backup_period: "2026-03"
      - base_backup_uuid: "base-uuid-march-2026"
      - backup_status: "IN_PROGRESS"

6. Backup completes successfully:
   ✓ Updates incremental_backup_tracker:
      - backup_status: "SUCCESS"
```

---

## 📊 Database Table Examples

### full_backup_tracker Table

**Monthly Frequency:**
| batch_id | category_code | backup_period | base_backup_uuid | backup_status |
|----------|--------------|---------------|------------------|---------------|
| BATCH_001 | HWA_EPR_DB_BACKUP_FULL | `2026-03` | uuid-march | SUCCESS |
| BATCH_050 | HWA_EPR_DB_BACKUP_FULL | `2026-04` | uuid-april | SUCCESS |

**Weekly Frequency:**
| batch_id | category_code | backup_period | base_backup_uuid | backup_status |
|----------|--------------|---------------|------------------|---------------|
| BATCH_010 | UAM_DB_BACKUP_FULL | `2026-W10` | uuid-week10 | SUCCESS |
| BATCH_020 | UAM_DB_BACKUP_FULL | `2026-W11` | uuid-week11 | SUCCESS |
| BATCH_030 | UAM_DB_BACKUP_FULL | `2026-W12` | uuid-week12 | SUCCESS |

**Custom 10-Day Frequency:**
| batch_id | category_code | backup_period | base_backup_uuid | backup_status |
|----------|--------------|---------------|------------------|---------------|
| BATCH_100 | COMPLIANCE_DB_BACKUP_FULL | `2026-01-01` | uuid-day1 | SUCCESS |
| BATCH_110 | COMPLIANCE_DB_BACKUP_FULL | `2026-01-11` | uuid-day11 | SUCCESS |
| BATCH_120 | COMPLIANCE_DB_BACKUP_FULL | `2026-01-21` | uuid-day21 | SUCCESS |

---

## ❓ Common Questions

### Q1: What happens if I request an incremental backup but no full backup exists for the current period?

**Answer:** The system will throw an error:
```
Error: Base backup UUID not found for category 'HWA_EPR_DB_BACKUP_FULL' and period '2026-03'. 
Please perform a full backup first for the current period.
```

**Solution:** Trigger a full backup request first.

---

### Q2: Can different databases have different frequencies?

**Answer:** Yes! Each request can specify its own backup frequency:

**Request 1 - Monthly:**
```json
{
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "payload": {
    "backupType": "FULL",
    "backupFrequency": "MONTHLY"
  }
}
```

**Request 2 - Weekly:**
```json
{
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "payload": {
    "backupType": "FULL",
    "backupFrequency": "WEEKLY"
  }
}
```

**Request 3 - 10-Day Interval:**
```json
{
  "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
  "payload": {
    "backupType": "FULL",
    "backupFrequency": "10_DAYS"
  }
}
```

**Request 4 - 15-Day Interval:**
```json
{
  "batchCategoryCode": "AUDIT_DB_BACKUP_FULL",
  "payload": {
    "backupType": "FULL",
    "backupFrequency": "15_DAYS"
  }
}
```

---

### Q3: What happens if I change the frequency type in my requests?

**Answer:**
- ✅ Old backups remain unchanged
- ✅ New backups use the new frequency type from the request
- ✅ System automatically calculates the correct period based on the frequency type in each request

**Example:**
```
Previous Requests (MONTHLY):
- March backups: backup_period = "2026-03"

New Requests (WEEKLY):
- Old March backups: Still have backup_period = "2026-03" (unchanged)
- New backups: backup_period = "2026-W11", "2026-W12", etc.
```

---

### Q4: How do I know which period format is being used?

**Answer:** Check the logs:

```
INFO: Calculated backup period: 2026-03 for category: HWA_EPR_DB_BACKUP_FULL with frequency type: MONTHLY (format: yyyy-MM)
INFO: Calculated backup period: 2026-W10 for category: UAM_DB_BACKUP_FULL with frequency type: WEEKLY (format: yyyy-'W'ww)
INFO: Calculated backup period: 2026-01-11 for category: COMPLIANCE_DB_BACKUP_FULL with frequency type: CUSTOM (format: yyyy-MM-dd)
```

---

## 🎓 Quick Reference

| Frequency Type | Period Format | Example | When Full Backup Happens |
|---------------|---------------|---------|-------------------------|
| **MONTHLY** | `YYYY-MM` | `2026-03` | Once per calendar month |
| **WEEKLY** | `YYYY-Www` | `2026-W10` | Once per ISO week (Monday-Sunday) |
| **CUSTOM** | `YYYY-MM-DD` | `2026-01-11` | Every N days (configurable) |

---

## 💡 Best Practices

1. **Use MONTHLY for most databases** - Good balance between backup frequency and storage
2. **Use WEEKLY for critical databases** - More frequent recovery points
3. **Use CUSTOM for compliance** - Meet specific regulatory requirements
4. **Always trigger full backup first** - Before requesting incremental backups for a new period
5. **Monitor logs** - Ensure backups are using the correct period format

---

## 🆚 Side-by-Side Comparison

### Same Date (March 15, 2026) - Different Frequencies

| Aspect | MONTHLY | WEEKLY | CUSTOM (10 days) |
|--------|---------|--------|------------------|
| **Period Calculated** | `2026-03` | `2026-W11` | `2026-03-11` |
| **Full Backup Date** | March 1 | March 9 (Monday) | March 11 |
| **Next Full Backup** | April 1 | March 16 (Monday) | March 21 |
| **Incremental Backups** | All of March | March 9-15 | March 11-20 |
| **Storage Impact** | Low (1 full/month) | Medium (4-5 full/month) | Medium (3 full/month) |
| **Recovery Points** | Monthly | Weekly | Every 10 days |
| **Use Case** | Standard databases | Critical databases | Compliance requirements |

---

## 🔍 Detailed Example: Same Request, Different Outcomes

### Request 1: MONTHLY Frequency
```json
{
  "batchId": "BATCH_20260315_001",
  "batchCategoryCode": "DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "MONTHLY"
  }
}
```

**YAML Config:**
```yaml
MONTHLY:
  format: "yyyy-MM"
```

**What Happens:**
1. System reads frequencyType from request: `MONTHLY`
2. System reads format from YAML: `yyyy-MM`
3. System calculates period: `2026-03`
4. Queries database: `WHERE backup_period = '2026-03'`
5. Finds base UUID from March 1 full backup
6. Performs incremental backup using that base UUID
7. Stores record with `backup_period = '2026-03'`

**Database Record:**
```
batch_id: BATCH_20260315_001
backup_period: 2026-03
base_backup_uuid: uuid-from-march-1
```

---

### Request 2: WEEKLY Frequency
```json
{
  "batchId": "BATCH_20260315_002",
  "batchCategoryCode": "DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "WEEKLY"
  }
}
```

**YAML Config:**
```yaml
WEEKLY:
  format: "yyyy-'W'ww"
```

**What Happens:**
1. System reads frequencyType from request: `WEEKLY`
2. System reads format from YAML: `yyyy-'W'ww`
3. System calculates period: `2026-W11` (March 15 is in week 11)
4. Queries database: `WHERE backup_period = '2026-W11'`
5. Finds base UUID from March 9 (Monday) full backup
6. Performs incremental backup using that base UUID
7. Stores record with `backup_period = '2026-W11'`

**Database Record:**
```
batch_id: BATCH_20260315_001
backup_period: 2026-W11
base_backup_uuid: uuid-from-march-9
```

---

### Request 3: CUSTOM (10 days) Frequency
```json
{
  "batchId": "BATCH_20260315_003",
  "batchCategoryCode": "DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "CUSTOM",
    "intervalDays": 10
  }
}
```

**YAML Config:**
```yaml
CUSTOM:
  format: "yyyy-MM-dd"
  epoch-date: "2026-01-01"
```

**What Happens:**
1. System reads frequencyType from request: `CUSTOM`
2. System reads intervalDays from request: `10`
3. System reads format and epoch-date from YAML
4. System calculates period: `2026-03-11` (March 15 falls in days 11-20 interval)
5. Queries database: `WHERE backup_period = '2026-03-11'`
6. Finds base UUID from March 11 full backup
7. Performs incremental backup using that base UUID
8. Stores record with `backup_period = '2026-03-11'`

**Database Record:**
```
batch_id: BATCH_20260315_001
backup_period: 2026-03-11
base_backup_uuid: uuid-from-march-11
```

---

## 📞 Testing the Feature

### Test Case 1: Monthly Backup Flow

**Step 1: Trigger Full Backup (March 1)**
```json
{
  "batchId": "BATCH_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260301",
  "payload": {
    "backupType": "FULL",
    "frequencyType": "MONTHLY"
  }
}
```

**Expected Result:**
- ✅ Creates full backup
- ✅ Stores in DB with `backup_period = '2026-03'`
- ✅ Logs: "Calculated backup period: 2026-03 for category: HWA_EPR_DB_BACKUP_FULL with frequency type: MONTHLY (format: yyyy-MM)"

**Step 2: Trigger Incremental Backup (March 15)**
```json
{
  "batchId": "BATCH_002",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "MONTHLY"
  }
}
```

**Expected Result:**
- ✅ Queries for base UUID with `backup_period = '2026-03'`
- ✅ Finds base UUID from BATCH_001
- ✅ Performs incremental backup
- ✅ Stores in DB with `backup_period = '2026-03'`

---

### Test Case 2: Weekly Backup Flow

**Step 1: Trigger Full Backup (March 9 - Monday, Week 11)**
```json
{
  "batchId": "BATCH_W11",
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260309",
  "payload": {
    "backupType": "FULL",
    "frequencyType": "WEEKLY"
  }
}
```

**Expected Result:**
- ✅ Creates full backup
- ✅ Stores in DB with `backup_period = '2026-W11'`
- ✅ Logs: "Calculated backup period: 2026-W11 for category: UAM_DB_BACKUP_FULL with frequency type: WEEKLY (format: yyyy-'W'ww)"

**Step 2: Trigger Incremental Backup (March 12 - Thursday, Week 11)**
```json
{
  "batchId": "BATCH_W11_INC",
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260312",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "WEEKLY"
  }
}
```

**Expected Result:**
- ✅ Queries for base UUID with `backup_period = '2026-W11'`
- ✅ Finds base UUID from BATCH_W11
- ✅ Performs incremental backup
- ✅ Stores in DB with `backup_period = '2026-W11'`

**Step 3: Trigger Full Backup (March 16 - Monday, Week 12)**
```json
{
  "batchId": "BATCH_W12",
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260316",
  "payload": {
    "backupType": "FULL",
    "frequencyType": "WEEKLY"
  }
}
```

**Expected Result:**
- ✅ Creates NEW full backup (new week!)
- ✅ Stores in DB with `backup_period = '2026-W12'`
- ✅ Logs: "Calculated backup period: 2026-W12 for category: UAM_DB_BACKUP_FULL with frequency type: WEEKLY (format: yyyy-'W'ww)"

---

## 🎬 Summary

### Key Takeaways

1. **Same Code, Different Behavior**: The same backup request behaves differently based on configuration
2. **Period is Key**: The `backup_period` column stores the period, which determines backup grouping
3. **Automatic Calculation**: System automatically calculates the correct period based on frequency type
4. **No Manual Intervention**: Users don't need to specify the period - it's calculated automatically
5. **Flexible & Powerful**: Each request can specify its own backup frequency

### What Changed vs. Old System

| Aspect | Old System | New System |
|--------|-----------|------------|
| **Frequency** | Fixed (Monthly only) | Configurable (Monthly/Weekly/Custom) |
| **Period Format** | Always `YYYY-MM` | `YYYY-MM`, `YYYY-Www`, or `YYYY-MM-DD` |
| **Configuration** | Hardcoded | YAML-based (format patterns) |
| **Request** | No frequency field | `frequencyType` in every request |
| **Flexibility** | None | Per-request configuration |
| **Database Queries** | Same | Same (no changes needed!) |
| **Java Code** | Complex classes | 2 simple files (85 lines total) |

---

## 🎨 YAML-Driven Architecture Benefits

### Why YAML-Driven?

**1. No Java Code Changes Needed**
- Want to change date format? Just edit YAML!
- Want to add a new frequency type? Just add to YAML!
- No recompilation, no deployment of new code

**2. Simple & Clean**
- Only 2 Java files (85 lines total)
- All logic in configuration
- Easy to understand and maintain

**3. Highly Flexible**
- Each request specifies its own frequency
- Same database can use different frequencies
- Easy to test different configurations

### Example: Adding a New Frequency Type

**Want bi-weekly backups? Just add to YAML:**

```yaml
backup:
  period-calculation:
    configs:
      MONTHLY:
        format: "yyyy-MM"
      WEEKLY:
        format: "yyyy-'W'ww"
      BIWEEKLY:  # ← New frequency type!
        format: "yyyy-'W'ww"
        description: "Bi-weekly backup period"
      CUSTOM:
        format: "yyyy-MM-dd"
        epoch-date: "2026-01-01"
```

**Then use it in requests:**
```json
{
  "payload": {
    "backupType": "FULL",
    "frequencyType": "BIWEEKLY"
  }
}
```

**No Java code changes required!** ✅

---

## 📋 Complete YAML Configuration Reference

```yaml
backup:
  # Period Calculation Configuration
  # This configuration defines how to calculate backup periods based on frequency type
  # The period calculation is done using simple date formatting patterns from YAML
  # All logic is configuration-driven - no hardcoded Java classes needed!
  period-calculation:
    configs:
      # MONTHLY: Returns current year-month (YYYY-MM)
      # Example: 2026-03
      MONTHLY:
        format: "yyyy-MM"
        description: "Monthly backup period - format: YYYY-MM"

      # WEEKLY: Returns current ISO week (YYYY-Www)
      # Example: 2026-W11
      WEEKLY:
        format: "yyyy-'W'ww"
        description: "Weekly backup period - format: YYYY-Www (ISO week)"

      # CUSTOM: Returns interval start date (YYYY-MM-DD)
      # Calculated as: (days since epoch / intervalDays) * intervalDays
      # Example: For 10-day intervals, March 15 (day 74) -> 2026-03-11 (day 70)
      CUSTOM:
        format: "yyyy-MM-dd"
        description: "Custom interval backup period - format: YYYY-MM-DD"
        epoch-date: "2026-01-01"  # Reference date for interval calculation
```

---

## 🚀 Request Examples - All Scenarios

### 1. Monthly Full Backup
```bash
curl -X POST http://localhost:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_001",
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchTransactionDate": "20260301",
    "payload": {
      "backupType": "FULL",
      "frequencyType": "MONTHLY"
    }
  }'
```

### 2. Monthly Incremental Backup
```bash
curl -X POST http://localhost:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_002",
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchTransactionDate": "20260315",
    "payload": {
      "backupType": "INCREMENTAL",
      "frequencyType": "MONTHLY"
    }
  }'
```

### 3. Weekly Full Backup
```bash
curl -X POST http://localhost:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_W11",
    "batchCategoryCode": "UAM_DB_BACKUP_FULL",
    "batchTransactionDate": "20260309",
    "payload": {
      "backupType": "FULL",
      "frequencyType": "WEEKLY"
    }
  }'
```

### 4. Weekly Incremental Backup
```bash
curl -X POST http://localhost:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_W11_INC",
    "batchCategoryCode": "UAM_DB_BACKUP_FULL",
    "batchTransactionDate": "20260312",
    "payload": {
      "backupType": "INCREMENTAL",
      "backupFrequency": "WEEKLY"
    }
  }'
```

### 5. Custom (10-day) Full Backup
```bash
curl -X POST http://localhost:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_D1",
    "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
    "batchTransactionDate": "20260101",
    "payload": {
      "backupType": "FULL",
      "backupFrequency": "10_DAYS"
    }
  }'
```

### 6. Custom (10-day) Incremental Backup
```bash
curl -X POST http://localhost:8080/api/backup \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "BATCH_D1_INC",
    "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
    "batchTransactionDate": "20260105",
    "payload": {
      "backupType": "INCREMENTAL",
      "backupFrequency": "10_DAYS"
    }
  }'
```

---

**Need more examples? Check the logs when running backups - they show exactly what period is being calculated!**

