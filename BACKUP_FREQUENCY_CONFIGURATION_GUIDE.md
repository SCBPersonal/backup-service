# Backup Frequency Configuration Guide

## Overview

The Backup Orchestrator Service now supports **configurable backup frequencies** instead of being hardcoded to monthly backups. You can configure full backups to run:
- **Monthly** (default) - Once per month
- **Weekly** - Once per week  
- **Custom** - Every N days (e.g., every 10 days)

Incremental backups automatically follow the full backup schedule.

---

## Configuration

### YAML Configuration

Add backup frequency settings in `application.yml`:

```yaml
backup:
  frequency:
    # Default frequency type if not specified for a database
    default-type: MONTHLY  # Options: MONTHLY, WEEKLY, CUSTOM
    
    # Database-specific frequency configurations
    databases:
      # Monthly full backups (default)
      HWA_EPR_DB_BACKUP_FULL:
        type: MONTHLY
      
      # Weekly full backups
      UAM_DB_BACKUP_FULL:
        type: WEEKLY
      
      # Custom interval - Full backup every 10 days
      CUSTOM_DB_BACKUP_FULL:
        type: CUSTOM
        interval-days: 10
```

### Environment Variables

You can override frequency settings using environment variables:

```bash
# Set default frequency type
export BACKUP_DEFAULT_FREQUENCY=WEEKLY

# Set frequency for specific database
export HWA_EPR_BACKUP_FREQUENCY_TYPE=MONTHLY
```

---

## Frequency Types

### 1. MONTHLY (Default)

**Description**: Full backup once per month

**Backup Period Format**: `YYYY-MM` (e.g., `2026-03`)

**Example**:
```yaml
HWA_EPR_DB_BACKUP_FULL:
  type: MONTHLY
```

**Behavior**:
- Full backup performed once per calendar month
- All backups in March 2026 use period `2026-03`
- Incremental backups reference the full backup from the same month

---

### 2. WEEKLY

**Description**: Full backup once per week

**Backup Period Format**: `YYYY-Www` (e.g., `2026-W10`)

**Example**:
```yaml
UAM_DB_BACKUP_FULL:
  type: WEEKLY
```

**Behavior**:
- Full backup performed once per ISO week
- Week 10 of 2026 uses period `2026-W10`
- Incremental backups reference the full backup from the same week

---

### 3. CUSTOM

**Description**: Full backup every N days (configurable interval)

**Backup Period Format**: `YYYY-MM-DD` (e.g., `2026-03-05`)

**Example**:
```yaml
CUSTOM_DB_BACKUP_FULL:
  type: CUSTOM
  interval-days: 10  # Full backup every 10 days
```

**Behavior**:
- Full backup performed every N days
- Period represents the start date of the interval
- Uses epoch date `2026-01-01` as reference point

**Example with 10-day intervals**:
- Days 1-10 (Jan 1-10): Period = `2026-01-01`
- Days 11-20 (Jan 11-20): Period = `2026-01-11`
- Days 21-30 (Jan 21-30): Period = `2026-01-21`

---

## How It Works

### Backup Period Calculation

The system calculates the current backup period based on the configured frequency:

1. **Request arrives** for a backup operation
2. **System looks up** frequency configuration for the database category
3. **Period is calculated** using `BackupPeriodCalculator`:
   - MONTHLY: Current month in `YYYY-MM` format
   - WEEKLY: Current week in `YYYY-Www` format
   - CUSTOM: Start date of current N-day interval in `YYYY-MM-DD` format
4. **Period is used** as the key to store/retrieve base backup UUID

### Full Backup Flow

```
1. Calculate backup period (e.g., "2026-03" for MONTHLY)
2. Insert record into full_backup_tracker with period
3. Call YBA API for full backup
4. Store task UUID and response
5. BackupPollerService polls for completion
6. Update with base_backup_uuid when complete
```

### Incremental Backup Flow

```
1. Calculate backup period (e.g., "2026-03" for MONTHLY)
2. Query full_backup_tracker for base_backup_uuid using period
3. If base UUID found:
   - Proceed with incremental backup using base UUID
4. If base UUID NOT found:
   - Fail with error: "Full backup required for current period"
```

---

## Database Schema

The `backup_month` column in both tables now stores the backup period:

```sql
-- full_backup_tracker table
CREATE TABLE epricing.full_backup_tracker (
    ...
    backup_month VARCHAR(20),  -- Stores: "2026-03" or "2026-W10" or "2026-03-05"
    base_backup_uuid VARCHAR(255),
    ...
);

-- incremental_backup_tracker table  
CREATE TABLE epricing.incremental_backup_tracker (
    ...
    backup_month VARCHAR(20),  -- Stores: "2026-03" or "2026-W10" or "2026-03-05"
    base_backup_uuid VARCHAR(255),
    ...
);
```

**Note**: The column name remains `backup_month` for backward compatibility, but it now stores the backup period in various formats.

---

## Migration Guide

### Upgrading from Monthly-Only to Configurable Frequency

**Step 1**: Add frequency configuration to `application.yml`

```yaml
backup:
  frequency:
    default-type: MONTHLY  # Keep existing behavior
    databases:
      HWA_EPR_DB_BACKUP_FULL:
        type: MONTHLY
```

**Step 2**: Deploy the updated service

**Step 3**: (Optional) Change frequency for specific databases

```yaml
backup:
  frequency:
    databases:
      UAM_DB_BACKUP_FULL:
        type: WEEKLY  # Change to weekly
```

**Step 4**: Existing data remains compatible
- Old records with `YYYY-MM` format continue to work
- New records use the configured format

---

## Examples

### Example 1: Monthly Backups (Default)

**Configuration**:
```yaml
HWA_EPR_DB_BACKUP_FULL:
  type: MONTHLY
```

**Timeline**:
- March 1-31, 2026: All backups use period `2026-03`
- April 1-30, 2026: All backups use period `2026-04`

---

### Example 2: Weekly Backups

**Configuration**:
```yaml
UAM_DB_BACKUP_FULL:
  type: WEEKLY
```

**Timeline**:
- Week 10 (Mar 2-8, 2026): All backups use period `2026-W10`
- Week 11 (Mar 9-15, 2026): All backups use period `2026-W11`

---

### Example 3: Custom 10-Day Intervals

**Configuration**:
```yaml
CUSTOM_DB_BACKUP_FULL:
  type: CUSTOM
  interval-days: 10
```

**Timeline** (starting from epoch 2026-01-01):
- Jan 1-10: Period = `2026-01-01`
- Jan 11-20: Period = `2026-01-11`
- Jan 21-30: Period = `2026-01-21`
- Jan 31 - Feb 9: Period = `2026-01-31`

---

## Troubleshooting

### Error: "Base backup UUID not found for current period"

**Cause**: No successful full backup exists for the current backup period

**Solution**: Trigger a full backup for the current period before attempting incremental backup

### Error: "interval-days must be specified for CUSTOM frequency type"

**Cause**: CUSTOM frequency type configured without `interval-days`

**Solution**: Add `interval-days` to configuration:
```yaml
type: CUSTOM
interval-days: 10
```

---

## API Reference

### BackupFrequencyProperties

Configuration class for backup frequency settings.

**Location**: `src/main/java/com/scb/backup/config/BackupFrequencyProperties.java`

### BackupPeriodCalculator

Utility class for calculating backup periods.

**Location**: `src/main/java/com/scb/backup/utils/BackupPeriodCalculator.java`

**Key Methods**:
- `calculateCurrentPeriod(FrequencyConfig)`: Calculate period for current date
- `calculatePeriod(LocalDate, FrequencyConfig)`: Calculate period for specific date
- `parsePeriod(String, FrequencyType)`: Parse period string back to LocalDate

---

## Best Practices

1. **Use MONTHLY for most databases** - Provides good balance between backup frequency and storage
2. **Use WEEKLY for critical databases** - More frequent recovery points
3. **Use CUSTOM for special requirements** - E.g., compliance requirements for backups every 10 days
4. **Keep incremental frequency aligned** - Incremental backups automatically follow full backup schedule
5. **Monitor backup periods** - Ensure full backups complete successfully for each period

---

## Version History

- **v2.0** (2026-03-05): Added configurable backup frequency support
- **v1.0** (2026-02-04): Initial release with monthly-only backups

