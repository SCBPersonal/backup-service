# 🕐 Cron Expression Guide for Backup Orchestrator Service

> **Version:** 3.0  
> **Last Updated:** 2026-03-09  
> **Author:** SCB ePricing Team

---

## 📋 Overview

The Backup Orchestrator Service uses **cron expressions** in API requests to determine backup periods. Users pass a cron expression in the payload, and the service calculates the next execution time to use as the backup period identifier.

---

## 🔢 Cron Expression Format

### **6-Field Format:**
```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12 or JAN-DEC)
│ │ │ │ │ ┌───────────── day of week (0-7 or MON-SUN)
│ │ │ │ │ │
* * * * * *
```

### **Special Characters:**
- `*` = All values
- `-` = Range (e.g., `1-5`)
- `,` = List (e.g., `1,15`)
- `/` = Increments (e.g., `*/5`)
- `?` = No specific value (day-of-month or day-of-week only)

---

## 💡 Common Examples

### **1. MONTHLY - 1st of Every Month at 2 AM**
```json
{
  "payload": {
    "cronExpression": "0 0 2 1 * *"
  }
}
```

**Explanation:**
- `0` = 0 seconds
- `0` = 0 minutes
- `2` = 2 AM
- `1` = 1st day of month
- `*` = Every month
- `*` = Any day of week

**Next Execution:** April 1, 2026 at 2:00 AM  
**Backup Period:** `2026-04-01-0200`

---

### **2. WEEKLY - Every Monday at 2 AM**
```json
{
  "payload": {
    "cronExpression": "0 0 2 * * MON"
  }
}
```

**Explanation:**
- `0` = 0 seconds
- `0` = 0 minutes
- `2` = 2 AM
- `*` = Any day of month
- `*` = Every month
- `MON` = Monday

**Next Execution:** March 9, 2026 (Monday) at 2:00 AM  
**Backup Period:** `2026-03-09-0200`

---

### **3. CUSTOM - Specific Date (June 15, 2026 at 3 AM)**
```json
{
  "payload": {
    "cronExpression": "0 0 3 15 6 *"
  }
}
```

**Explanation:**
- `0` = 0 seconds
- `0` = 0 minutes
- `3` = 3 AM
- `15` = 15th day of month
- `6` = June
- `*` = Any day of week

**Next Execution:** June 15, 2026 at 3:00 AM  
**Backup Period:** `2026-06-15-0300`

---

### **4. BI-WEEKLY - 1st and 15th of Month at 2 AM**
```json
{
  "payload": {
    "cronExpression": "0 0 2 1,15 * *"
  }
}
```

**Explanation:**
- `1,15` = 1st and 15th day of month

**Next Execution:** March 15, 2026 at 2:00 AM  
**Backup Period:** `2026-03-15-0200`

---

### **5. QUARTERLY - Jan 1, Apr 1, Jul 1, Oct 1 at 2 AM**
```json
{
  "payload": {
    "cronExpression": "0 0 2 1 1,4,7,10 *"
  }
}
```

**Explanation:**
- `1` = 1st day of month
- `1,4,7,10` = January, April, July, October

**Next Execution:** April 1, 2026 at 2:00 AM  
**Backup Period:** `2026-04-01-0200`

---

### **6. EVERY FRIDAY at 2 AM**
```json
{
  "payload": {
    "cronExpression": "0 0 2 * * FRI"
  }
}
```

**Next Execution:** Next Friday at 2:00 AM  
**Backup Period:** `2026-03-13-0200`

---

### **7. WEEKDAYS ONLY (Mon-Fri) at 2 AM**
```json
{
  "payload": {
    "cronExpression": "0 0 2 * * MON-FRI"
  }
}
```

**Next Execution:** Next weekday at 2:00 AM  
**Backup Period:** `2026-03-09-0200`

---

### **8. LAST DAY OF MONTH at 2 AM**
```json
{
  "payload": {
    "cronExpression": "0 0 2 L * *"
  }
}
```

**Next Execution:** March 31, 2026 at 2:00 AM  
**Backup Period:** `2026-03-31-0200`

---

## 📊 Quick Reference Table

| Schedule Type | Cron Expression | Description |
|---------------|-----------------|-------------|
| **Monthly** | `0 0 2 1 * *` | 1st of every month at 2 AM |
| **Weekly** | `0 0 2 * * MON` | Every Monday at 2 AM |
| **Bi-Weekly** | `0 0 2 1,15 * *` | 1st and 15th at 2 AM |
| **Quarterly** | `0 0 2 1 1,4,7,10 *` | Jan 1, Apr 1, Jul 1, Oct 1 at 2 AM |
| **Custom Date** | `0 0 3 15 6 *` | June 15 at 3 AM |
| **Every Friday** | `0 0 2 * * FRI` | Every Friday at 2 AM |
| **Last Day of Month** | `0 0 2 L * *` | Last day of month at 2 AM |
| **Weekdays Only** | `0 0 2 * * MON-FRI` | Monday to Friday at 2 AM |

---

## 🔍 Complete API Request Examples

### **Example 1: Full Backup - Monthly**
```bash
curl -X POST http://localhost:10022/backupProcess \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "MONTHLY_FULL_001",
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchTransactionDate": "20260309",
    "payload": {
      "cronExpression": "0 0 2 1 * *"
    }
  }'
```

---

### **Example 2: Incremental Backup - Weekly**
```bash
curl -X POST http://localhost:10022/backupProcess \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "WEEKLY_INCR_001",
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_INCRE",
    "batchTransactionDate": "20260309",
    "payload": {
      "cronExpression": "0 0 2 * * MON"
    }
  }'
```

---

## 🚨 Error Handling

### **Missing Cron Expression**
```json
{
  "payload": {}
}
```

**Error:**
```
cronExpression is required in the request payload for category: HWA_EPR_DB_BACKUP_FULL. 
Examples: '0 0 2 1 * *' (monthly), '0 0 2 * * MON' (weekly), '0 0 3 15 6 *' (custom date)
```

---

### **Invalid Cron Expression**
```json
{
  "payload": {
    "cronExpression": "INVALID"
  }
}
```

**Error:**
```
Invalid cron expression: INVALID. Error: Cron expression must consist of 6 fields
```

---

## 🎯 Best Practices

1. **Always use 6-field format** - Include seconds (usually `0`)
2. **Test cron expressions** - Use online tools before deploying
3. **Use descriptive batch IDs** - Include schedule type (e.g., `MONTHLY_FULL_001`)
4. **Document your cron expressions** - Add comments in your code
5. **Validate before sending** - Check cron syntax is correct

---

## 🔧 Testing Your Cron Expression

### **Online Cron Expression Generator:**
- https://crontab.guru/ (5-field format - add `0` at the beginning for 6-field)
- https://www.freeformatter.com/cron-expression-generator-quartz.html

### **Example Conversion:**
- **5-field (crontab.guru):** `0 2 1 * *`
- **6-field (our format):** `0 0 2 1 * *` (add `0` for seconds)

---

**Version:** 3.0  
**Last Updated:** 2026-03-09  
**Author:** SCB ePricing Team

