# 🎯 Complete Implementation Guide - Configurable Backup Frequency

## 📌 Overview

The Backup Orchestrator Service now supports **configurable backup frequencies** with a **request-based approach**.

### Key Features:
- ✅ **Request-based frequency**: Specify frequency in each backup request
- ✅ **Three frequency types**: MONTHLY, WEEKLY, CUSTOM (N-day intervals)
- ✅ **Fallback configuration**: YAML config used when request doesn't specify frequency
- ✅ **Backward compatible**: Existing requests without frequency still work
- ✅ **Dynamic period calculation**: System automatically calculates backup periods

---

## 🚀 Quick Start

### 1. Request Format (PRIMARY METHOD)

Specify frequency directly in your backup request:

```json
{
  "batchId": "BATCH_20260315_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "payload": {
    "backupType": "INCREMENTAL",
    "frequencyType": "MONTHLY"  // ← Specify frequency here
  }
}
```

### 2. Fallback Configuration (OPTIONAL)

If you don't want to specify frequency in every request, configure it in `application.yml`:

```yaml
backup:
  frequency:
    databases:
      HWA_EPR_DB_BACKUP_FULL:
        type: MONTHLY
```

---

## 📝 Request Payload Fields

### Required Fields

| Field | Type | Required | Values | Description |
|-------|------|----------|--------|-------------|
| `batchId` | String | ✅ Yes | Any | Unique batch identifier |
| `batchCategoryCode` | String | ✅ Yes | Any | Database category code |
| `batchTransactionDate` | String | ✅ Yes | YYYYMMDD | Transaction date |
| `payload.backupType` | String | ✅ Yes | `FULL`, `INCREMENTAL` | Type of backup |
| `payload.frequencyType` | String | ⚠️ Recommended | `MONTHLY`, `WEEKLY`, `CUSTOM` | Backup frequency |
| `payload.intervalDays` | Integer | ⚠️ Conditional | 1-365 | Required only for `CUSTOM` |

---

## 📋 Complete Examples

### Example 1: MONTHLY Frequency

**Full Backup:**
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

**Incremental Backup:**
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

**What Happens:**
- Full backup on March 1: Creates period `2026-03`
- Incremental on March 15: Uses base UUID from period `2026-03`
- All March backups use the same period: `2026-03`

---

### Example 2: WEEKLY Frequency

**Full Backup (Monday, Week 11):**
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

**Incremental Backup (Thursday, Week 11):**
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

**What Happens:**
- Full backup on March 9 (Monday): Creates period `2026-W11`
- Incremental on March 12 (Thursday): Uses base UUID from period `2026-W11`
- All Week 11 backups use the same period: `2026-W11`
- Next Monday (March 16): New period `2026-W12` starts

---

### Example 3: CUSTOM Frequency (10 Days)

**Full Backup:**
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

**Incremental Backup:**
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

**What Happens:**
- Full backup on March 11: Creates period `2026-03-11` (days 71-80 from epoch)
- Incremental on March 15: Uses base UUID from period `2026-03-11`
- March 21: New period `2026-03-21` starts (days 81-90)

---

## 🔄 How It Works

### Flow Diagram

```
1. Request arrives with frequencyType in payload
   ↓
2. System extracts frequencyType and intervalDays
   ↓
3. System calculates backup period:
   - MONTHLY: YYYY-MM (e.g., "2026-03")
   - WEEKLY: YYYY-Www (e.g., "2026-W11")
   - CUSTOM: YYYY-MM-DD (e.g., "2026-03-11")
   ↓
4. For FULL backup:
   - Insert into full_backup_tracker with calculated period
   ↓
5. For INCREMENTAL backup:
   - Query full_backup_tracker for base UUID using calculated period
   - Use base UUID for incremental backup
```

### Period Calculation Examples

| Date | Frequency | Calculated Period | Explanation |
|------|-----------|-------------------|-------------|
| 2026-03-15 | MONTHLY | `2026-03` | Current month |
| 2026-03-15 | WEEKLY | `2026-W11` | ISO Week 11 |
| 2026-03-15 | CUSTOM (10 days) | `2026-03-11` | Days 71-80 interval |

---

## 🗄️ Database Schema

### Tables

**full_backup_tracker:**
```sql
CREATE TABLE epricing.full_backup_tracker (
    batch_id VARCHAR(50),
    category_code VARCHAR(100),
    business_date DATE,
    backup_month VARCHAR(20),  -- Stores period in various formats
    base_backup_uuid VARCHAR(100),
    backup_status VARCHAR(20),
    ...
);
```

**incremental_backup_tracker:**
```sql
CREATE TABLE epricing.incremental_backup_tracker (
    batch_id VARCHAR(50),
    category_code VARCHAR(100),
    business_date DATE,
    backup_month VARCHAR(20),  -- Stores period in various formats
    base_backup_uuid VARCHAR(100),
    backup_status VARCHAR(20),
    ...
);
```

### Sample Data

**MONTHLY backups:**
| batch_id | category_code | backup_month | base_backup_uuid |
|----------|--------------|--------------|------------------|
| BATCH_001 | HWA_EPR_DB_BACKUP_FULL | `2026-03` | uuid-march |

**WEEKLY backups:**
| batch_id | category_code | backup_month | base_backup_uuid |
|----------|--------------|--------------|------------------|
| BATCH_002 | UAM_DB_BACKUP_FULL | `2026-W11` | uuid-week11 |
| BATCH_003 | UAM_DB_BACKUP_FULL | `2026-W12` | uuid-week12 |

**CUSTOM backups:**
| batch_id | category_code | backup_month | base_backup_uuid |
|----------|--------------|--------------|------------------|
| BATCH_004 | COMPLIANCE_DB_BACKUP_FULL | `2026-03-11` | uuid-day11 |
| BATCH_005 | COMPLIANCE_DB_BACKUP_FULL | `2026-03-21` | uuid-day21 |

---

## ⚙️ Configuration (Fallback)

### application.yml

```yaml
backup:
  frequency:
    # Default frequency if not specified in request
    default-type: MONTHLY
    
    # Database-specific fallback configurations
    databases:
      HWA_EPR_DB_BACKUP_FULL:
        type: MONTHLY
      
      UAM_DB_BACKUP_FULL:
        type: WEEKLY
      
      COMPLIANCE_DB_BACKUP_FULL:
        type: CUSTOM
        interval-days: 10
```

**Note:** This configuration is used ONLY when `frequencyType` is NOT provided in the request payload.

---

## 🧪 Testing

### Test Case 1: MONTHLY Backup Flow

**Step 1:** Full backup on March 1
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

**Expected:**
- Period calculated: `2026-03`
- Record inserted with `backup_month = '2026-03'`

**Step 2:** Incremental backup on March 15
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

**Expected:**
- Period calculated: `2026-03`
- Query: `SELECT base_backup_uuid FROM full_backup_tracker WHERE backup_month = '2026-03'`
- Uses base UUID from BATCH_001

---

## 📊 Summary

### Key Points

1. **Request-based**: Frequency is specified in the request payload (recommended)
2. **Fallback config**: YAML configuration used when request doesn't specify frequency
3. **Three formats**: MONTHLY (`YYYY-MM`), WEEKLY (`YYYY-Www`), CUSTOM (`YYYY-MM-DD`)
4. **Automatic calculation**: System calculates period based on date + frequency
5. **Database agnostic**: Same queries work for all frequency types

### Benefits

- ✅ **Flexible**: Each request can have different frequency
- ✅ **Dynamic**: No need to update configuration for new databases
- ✅ **Testable**: Easy to test different frequencies
- ✅ **Backward compatible**: Existing requests still work

---

**For more details, see:**
- `UPDATED_REQUEST_FORMAT_GUIDE.md` - Request format details
- `SAMPLE_BACKUP_REQUESTS.json` - Complete JSON samples
- `BACKUP_FREQUENCY_EXAMPLES.md` - Detailed examples
- `TESTING_GUIDE_WITH_SAMPLES.md` - Testing guide

