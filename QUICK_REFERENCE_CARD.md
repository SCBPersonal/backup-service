# 🚀 Backup Frequency - Quick Reference Card

## 🎯 Single Parameter Approach

**Use `backupFrequency` parameter in API requests:**

```json
{
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260315",
  "batchCategoryParameters": {
    "backupType": "FULL",
    "backupFrequency": "MONTHLY"  // Single parameter!
  }
}
```

**Supported Values**:
- `MONTHLY` - Monthly backups
- `WEEKLY` - Weekly backups
- `10_DAYS`, `15_DAYS`, `20_DAYS` - Custom intervals
- Any `{N}_DAYS` format (e.g., `7_DAYS`, `30_DAYS`)

---

## 📋 Configuration Cheat Sheet

### MONTHLY (Default)
```yaml
backup:
  frequency:
    databases:
      YOUR_DB_NAME:
        type: MONTHLY
```
- **Period Format**: `YYYY-MM` (e.g., `2026-03`)
- **Full Backup**: Once per calendar month
- **When to Use**: Standard databases, low-medium criticality

---

### WEEKLY
```yaml
backup:
  frequency:
    databases:
      YOUR_DB_NAME:
        type: WEEKLY
```
- **Period Format**: `YYYY-Www` (e.g., `2026-W11`)
- **Full Backup**: Once per ISO week (Monday-Sunday)
- **When to Use**: Critical databases, frequent recovery points needed

---

### CUSTOM (N-Day Intervals)
```yaml
backup:
  frequency:
    databases:
      YOUR_DB_NAME:
        type: CUSTOM
        interval-days: 10  # Change this number
```
- **Period Format**: `YYYY-MM-DD` (e.g., `2026-03-11`)
- **Full Backup**: Every N days from epoch (2026-01-01)
- **When to Use**: Compliance requirements, specific intervals

---

## 🔍 How to Check Current Configuration

### View Logs
```
INFO: Calculated backup period: 2026-03 for category: HWA_EPR_DB_BACKUP_FULL with frequency type: MONTHLY
INFO: Calculated backup period: 2026-W11 for category: UAM_DB_BACKUP_FULL with frequency type: WEEKLY
INFO: Calculated backup period: 2026-03-11 for category: COMPLIANCE_DB with frequency type: CUSTOM
```

### Query Database
```sql
-- See what periods are stored
SELECT DISTINCT backup_period, category_code
FROM epricing.full_backup_tracker
ORDER BY backup_period DESC;

-- Results will show:
-- 2026-03      (MONTHLY)
-- 2026-W11     (WEEKLY)
-- 2026-03-11   (CUSTOM)
```

---

## 🎯 Common Scenarios

### Scenario 1: "I want monthly backups" (Default)
```yaml
type: MONTHLY
```
✅ Full backup on 1st of each month  
✅ All incremental backups in that month use the same base

---

### Scenario 2: "I want weekly backups"
```yaml
type: WEEKLY
```
✅ Full backup every Monday  
✅ Incremental backups Mon-Sun use that week's base

---

### Scenario 3: "I want backups every 7 days"
```yaml
type: CUSTOM
interval-days: 7
```
✅ Full backup every 7 days from 2026-01-01  
✅ Incremental backups within each 7-day window use that period's base

---

### Scenario 4: "I want backups every 15 days"
```yaml
type: CUSTOM
interval-days: 15
```
✅ Full backup every 15 days from 2026-01-01  
✅ Periods: 2026-01-01, 2026-01-16, 2026-01-31, 2026-02-15, etc.

---

## ⚠️ Important Rules

### Rule 1: Full Backup First
❌ **WRONG**: Request incremental backup without full backup for current period  
✅ **RIGHT**: Always trigger full backup first for a new period

### Rule 2: Period Calculation is Automatic
❌ **WRONG**: Manually specify the period in your request  
✅ **RIGHT**: System calculates period based on current date + frequency config

### Rule 3: One Configuration Per Database
❌ **WRONG**: Multiple frequency configs for same database  
✅ **RIGHT**: One frequency type per database category code

---

## 🔧 Troubleshooting

### Error: "Base backup UUID not found"
**Cause**: No full backup exists for the current period  
**Solution**: Trigger a full backup request first

**Example:**
```
Current date: March 15, 2026
Frequency: WEEKLY
Current period: 2026-W11

Error: No full backup found for period 2026-W11

Fix: Trigger full backup for week 11 first
```

---

### Error: "Frequency configuration not found"
**Cause**: Database not configured in application.yml  
**Solution**: Add frequency configuration for the database

**Example:**
```yaml
backup:
  frequency:
    databases:
      YOUR_DB_NAME:  # Add this
        type: MONTHLY
```

---

## 📊 Period Calculation Examples

### Today: March 15, 2026

| Frequency Type | Configuration | Calculated Period | Explanation |
|---------------|---------------|-------------------|-------------|
| MONTHLY | `type: MONTHLY` | `2026-03` | Current month |
| WEEKLY | `type: WEEKLY` | `2026-W11` | Current ISO week |
| CUSTOM (7 days) | `type: CUSTOM`<br/>`interval-days: 7` | `2026-03-13` | Days 71-77 from epoch |
| CUSTOM (10 days) | `type: CUSTOM`<br/>`interval-days: 10` | `2026-03-11` | Days 71-80 from epoch |
| CUSTOM (15 days) | `type: CUSTOM`<br/>`interval-days: 15` | `2026-03-07` | Days 66-80 from epoch |

---

## 🎓 Quick Decision Tree

```
Do you need backups more than once per month?
│
├─ NO → Use MONTHLY
│
└─ YES → Do you need backups every week?
    │
    ├─ YES → Use WEEKLY
    │
    └─ NO → Do you need specific day intervals?
        │
        └─ YES → Use CUSTOM with interval-days
```

---

## 📞 Testing Checklist

### Before Deployment
- [ ] Configuration added to application.yml
- [ ] Full backup triggered for current period
- [ ] Incremental backup tested successfully
- [ ] Logs show correct period format
- [ ] Database records show correct backup_month values

### After Deployment
- [ ] Monitor first full backup
- [ ] Monitor first incremental backup
- [ ] Verify period transitions (e.g., month change, week change)
- [ ] Check backup_month values in database
- [ ] Verify base UUID retrieval works correctly

---

## 🔗 Related Documentation

- **Full Guide**: `BACKUP_FREQUENCY_CONFIGURATION_GUIDE.md`
- **Examples**: `BACKUP_FREQUENCY_EXAMPLES.md`
- **Implementation**: `CONFIGURABLE_BACKUP_FREQUENCY_IMPLEMENTATION_SUMMARY.md`
- **Deployment**: `IMPLEMENTATION_COMPLETE_SUMMARY.md`

---

## 💡 Pro Tips

1. **Start with MONTHLY** - It's the safest and most storage-efficient
2. **Test in non-prod first** - Verify period calculations before production
3. **Monitor the first period transition** - Ensure new full backups are created
4. **Check logs regularly** - They show exactly what period is being used
5. **Document your choice** - Record why you chose a specific frequency for each database

---

**Last Updated**: March 5, 2026  
**Version**: 1.0.0

