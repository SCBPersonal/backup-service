# ✅ Updated Request Format Guide

## 🎯 Key Change: Frequency in Request Payload

The backup frequency is now specified **in the request payload** instead of being configured in `application.yml`.

This makes the system more flexible - each backup request can specify its own frequency type!

---

## 📝 Request Format

### Basic Structure

```json
{
  "batchId": "BATCH_20260315_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "FULL" or "INCREMENTAL",
    "frequencyType": "MONTHLY" or "WEEKLY" or "CUSTOM",
    "intervalDays": 10  // Only required for CUSTOM type
  }
}
```

### Required Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `batchId` | String | ✅ Yes | Unique batch identifier |
| `batchCategoryCode` | String | ✅ Yes | Database category code |
| `batchTransactionDate` | String | ✅ Yes | Transaction date (YYYYMMDD) |
| `payload.backupType` | String | ✅ Yes | `FULL` or `INCREMENTAL` |
| `payload.frequencyType` | String | ✅ Yes | `MONTHLY`, `WEEKLY`, or `CUSTOM` |
| `payload.intervalDays` | Integer | ⚠️ Conditional | Required only when `frequencyType` = `CUSTOM` |

---

## 📋 Sample Requests

### 1. MONTHLY Frequency - Full Backup

```json
{
  "batchId": "BATCH_20260301_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260301",
  "payload": {
    "backupType": "FULL",
    "frequencyType": "MONTHLY"
  }
}
```

**Expected Period**: `2026-03`

---

### 2. MONTHLY Frequency - Incremental Backup

```json
{
  "batchId": "BATCH_20260315_002",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "MONTHLY"
  }
}
```

**Expected Period**: `2026-03`  
**Base UUID From**: Full backup with period `2026-03`

---

### 3. WEEKLY Frequency - Full Backup

```json
{
  "batchId": "BATCH_20260309_W11",
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260309",
  "payload": {
    "backupType": "FULL",
    "frequencyType": "WEEKLY"
  }
}
```

**Expected Period**: `2026-W11` (March 9 is Monday of Week 11)

---

### 4. WEEKLY Frequency - Incremental Backup

```json
{
  "batchId": "BATCH_20260312_W11_INC",
  "batchCategoryCode": "UAM_DB_BACKUP_FULL",
  "batchTransactionDate": "20260312",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "WEEKLY"
  }
}
```

**Expected Period**: `2026-W11` (March 12 is Thursday of Week 11)  
**Base UUID From**: Full backup with period `2026-W11`

---

### 5. CUSTOM Frequency (10 days) - Full Backup

```json
{
  "batchId": "BATCH_20260311_D11",
  "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
  "batchTransactionDate": "20260311",
  "payload": {
    "backupType": "FULL",
    "frequencyType": "CUSTOM",
    "intervalDays": 10
  }
}
```

**Expected Period**: `2026-03-11` (Start of 10-day interval)

---

### 6. CUSTOM Frequency (10 days) - Incremental Backup

```json
{
  "batchId": "BATCH_20260315_D11_INC",
  "batchCategoryCode": "COMPLIANCE_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "CUSTOM",
    "intervalDays": 10
  }
}
```

**Expected Period**: `2026-03-11` (March 15 falls in days 71-80 interval)  
**Base UUID From**: Full backup with period `2026-03-11`

---

## 🔄 How It Works

### Step-by-Step Flow

1. **Request arrives** with `frequencyType` in payload
2. **System extracts** `frequencyType` and `intervalDays` (if provided)
3. **System calculates period** based on:
   - Current date (from `batchTransactionDate`)
   - Frequency type (from `payload.frequencyType`)
   - Interval days (from `payload.intervalDays` for CUSTOM)
4. **System uses period** for all database operations

### Example: WEEKLY Request on March 15, 2026

```
Input:
  frequencyType: "WEEKLY"
  batchTransactionDate: "20260315"

Calculation:
  Current date: 2026-03-15
  ISO Week number: 11
  Calculated period: "2026-W11"

Database Query:
  SELECT base_backup_uuid 
  FROM full_backup_tracker 
  WHERE backup_month = '2026-W11'
```

---

## 🆚 Old vs New Format

### ❌ Old Format (Configuration-based)

**application.yml:**
```yaml
backup:
  frequency:
    databases:
      HWA_EPR_DB_BACKUP_FULL:
        type: MONTHLY
```

**Request:**
```json
{
  "batchId": "BATCH_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL"
  }
}
```

### ✅ New Format (Request-based)

**Request:**
```json
{
  "batchId": "BATCH_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "MONTHLY"
  }
}
```

**Benefits:**
- ✅ More flexible - each request can have different frequency
- ✅ No need to update application.yml for new databases
- ✅ Easier to test different frequencies
- ✅ Dynamic frequency selection per request

---

## ⚠️ Important Notes

1. **Frequency Type is Required**: Every request must include `frequencyType` in the payload
2. **Interval Days for CUSTOM**: When using `CUSTOM` frequency, `intervalDays` is mandatory
3. **Case Insensitive**: `frequencyType` values are case-insensitive (`MONTHLY`, `monthly`, `Monthly` all work)
4. **Fallback to Configuration**: If `frequencyType` is not provided in payload, system falls back to `application.yml` configuration
5. **Period Calculation**: The system automatically calculates the period - you don't specify it

---

## 🧪 Testing Examples

### Test 1: Same Database, Different Frequencies

**Request 1 (MONTHLY):**
```json
{
  "batchId": "BATCH_001",
  "batchCategoryCode": "TEST_DB",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "FULL",
    "frequencyType": "MONTHLY"
  }
}
```
**Result**: Period = `2026-03`

**Request 2 (WEEKLY):**
```json
{
  "batchId": "BATCH_002",
  "batchCategoryCode": "TEST_DB",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "FULL",
    "frequencyType": "WEEKLY"
  }
}
```
**Result**: Period = `2026-W11`

---

## 📊 Summary Table

| Frequency Type | Period Format | Example | intervalDays Required? |
|---------------|---------------|---------|----------------------|
| `MONTHLY` | `YYYY-MM` | `2026-03` | ❌ No |
| `WEEKLY` | `YYYY-Www` | `2026-W11` | ❌ No |
| `CUSTOM` | `YYYY-MM-DD` | `2026-03-11` | ✅ Yes |

---

**For complete JSON samples, see:** `SAMPLE_BACKUP_REQUESTS.json`  
**For detailed examples, see:** `BACKUP_FREQUENCY_EXAMPLES.md`  
**For testing guide, see:** `TESTING_GUIDE_WITH_SAMPLES.md`

