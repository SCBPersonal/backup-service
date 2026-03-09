# 📘 Backup Orchestrator Service - Complete Technical Guide

> **Version:** 3.0
> **Last Updated:** 2026-03-09
> **Author:** SCB ePricing Team

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Components](#core-components)
4. [API Reference](#api-reference)
5. [Database Schema](#database-schema)
6. [Configuration](#configuration)
7. [Backup Workflows](#backup-workflows)
8. [Cron Expression Guide](#cron-expression-guide)
9. [Error Handling](#error-handling)
10. [Deployment](#deployment)
11. [Testing](#testing)

---

## 🎯 Overview

### Purpose

The **Backup Orchestrator Service** is a Spring Boot microservice that orchestrates YugabyteDB database backups through integration with **YugabyteDB Anywhere (YBA)** REST API.

### Key Features

✅ **Cron Expression-Based Scheduling** - Pass cron expressions in API requests
✅ **Full & Incremental Backups** - Support for both backup types
✅ **Async Polling** - Background job completion monitoring
✅ **Base UUID Tracking** - Automatic base backup UUID management
✅ **Multi-Database Support** - Configure multiple databases
✅ **Reactive Programming** - Built with Spring WebFlux
✅ **Comprehensive Testing** - 80%+ test coverage

### Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.x |
| Language | Java | 17+ |
| Build Tool | Gradle | 8.11.1 |
| Database | YugabyteDB | - |
| Reactive | Spring WebFlux | - |
| HTTP Client | WebClient | - |

---

## 🏗️ Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    External Scheduler/Client                 │
│                  (Sends API Request with Cron)               │
└────────────────────────────┬────────────────────────────────┘
                             │ POST /backupProcess
                             │ {cronExpression: "0 0 2 1 * *"}
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                      BackupController                        │
│                   (REST API Endpoint)                        │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                       BackupService                          │
│  ┌────────────────────────────────────────────────────┐     │
│  │ 1. Extract cronExpression from payload            │     │
│  │ 2. Validate batch parameters                      │     │
│  │ 3. Initiate backup via YbaClient                  │     │
│  │ 4. Handle success/failure                         │     │
│  └────────────────────────────────────────────────────┘     │
└────────────────────────────┬────────────────────────────────┘
                             │
                ┌────────────┼────────────┐
                │            │            │
                ▼            ▼            ▼
┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐
│   YbaClient      │ │ BackupDao    │ │ Validation       │
│                  │ │ Service      │ │ Service          │
│ - fullBackup()   │ │              │ │                  │
│ - incremental()  │ │ - insert()   │ │ - validate()     │
│ - fetchLast()    │ │ - update()   │ │                  │
└────────┬─────────┘ └──────┬───────┘ └──────────────────┘
         │                  │
         │                  │
         ▼                  ▼
┌──────────────────┐ ┌──────────────────────────────────────┐
│  YBA REST API    │ │  YugabyteDB (Tracking Database)      │
│                  │ │  ┌────────────────────────────────┐  │
│ - POST /backups  │ │  │ full_backup_tracker            │  │
│ - GET /backups   │ │  │ - batch_id, category_code      │  │
│ - GET /tasks     │ │  │ - backup_period, backup_status │  │
│                  │ │  │ - base_backup_uuid, task_uuid  │  │
└──────────────────┘ │  └────────────────────────────────┘  │
                     │  ┌────────────────────────────────┐  │
                     │  │ incremental_backup_tracker     │  │
                     │  │ - batch_id, category_code      │  │
                     │  │ - backup_period, backup_status │  │
                     │  │ - base_backup_uuid, task_uuid  │  │
                     │  └────────────────────────────────┘  │
                     └──────────────────────────────────────┘
         ▲
         │ (Async Polling)
         │
┌────────┴─────────┐
│ BackupPoller     │
│ Service          │
│                  │
│ - Polls YBA API  │
│ - Updates UUID   │
│ - Updates Status │
└──────────────────┘
```

### Component Interaction Flow

```
1. API Request → BackupController
2. BackupController → BackupService.execute()
3. BackupService → Extract cronExpression from payload
4. BackupService → CronExpressionParser.calculateBackupPeriod()
5. BackupService → YbaClient.backupInitiate()
6. YbaClient → YbaConfigService.resolve(categoryCode)
7. YbaClient → BackupDaoService.insertBackupDetails()
8. YbaClient → WebClient.post() to YBA API
9. YBA API → Returns {taskUUID, resourceUUID}
10. YbaClient → BackupDaoService.updateBackupResponse()
11. BackupPollerService → Polls YBA API for job completion
12. BackupPollerService → Updates base_backup_uuid when complete
```

---

## 🧩 Core Components

### 1. BackupController
**Location:** `src/main/java/com/scb/backup/controller/BackupController.java`
**Responsibility:** REST API endpoint for triggering backup processes

**Endpoint:**
```java
@PostMapping("/backupProcess")
public Mono<BatchStartResponse> backupProcess(@RequestBody String json)
```

**Request Example:**
```json
{
  "batchId": "BATCH_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260309",
  "payload": {
    "cronExpression": "0 0 2 1 * *"
  }
}
```

---

### 2. BackupService
**Location:** `src/main/java/com/scb/backup/service/BackupService.java`
**Responsibility:** Core backup orchestration logic

**Key Methods:**
- `process(Map<String, Object> batchParams)` - Main entry point
- `processBackup(...)` - Reactive backup workflow
- `handleBackupSuccess(...)` - Success handler
- `handleBackupFailure(...)` - Error handler

---

### 3. YbaClient
**Location:** `src/main/java/com/scb/backup/client/YbaClient.java`
**Responsibility:** HTTP client for YBA API

**Key Methods:**
- `backupInitiate()` - Determines backup type and initiates
- `fullBackup()` - Executes full backup
- `performIncrementalBackup()` - Executes incremental backup
- `fetchLastBackup()` - Retrieves last backup metadata

---

### 4. CronExpressionParser
**Location:** `src/main/java/com/scb/backup/utils/CronExpressionParser.java`
**Responsibility:** Parse cron expressions and calculate backup periods

**Key Methods:**

| Method | Return Example |
|--------|----------------|
| `calculateBackupPeriod(cron)` | `"2026-04-01-0200"` |
| `calculateBackupInterval(cron)` | `"2026-04-01 02:00:00 (Cron: 0 0 2 1 * *)"` |
| `getNextExecutionTime(cron)` | `2026-04-01T02:00:00` |
| `determineFrequencyType(cron)` | `"MONTHLY"/"WEEKLY"/"CUSTOM"` |

---

### 5. BackupPollerService
**Location:** `src/main/java/com/scb/backup/service/BackupPollerService.java`
**Responsibility:** Async polling for backup job completion

**Configuration:**
```yaml
backup:
  poller:
    enabled: true
    initial-delay-ms: 5000
    polling-interval-ms: 30000
    thread-pool-size: 5
```

**Workflow:**
1. Scheduled task runs every 30 seconds
2. Query full_backup_tracker for IN_PROGRESS records
3. Call YBA API to check job status
4. Update base_backup_uuid when complete

---

### 6. YbaConfigService
**Location:** `src/main/java/com/scb/backup/service/YbaConfigService.java`
**Responsibility:** Resolves database-specific backup configurations

---

### 7. BackupDaoService
**Location:** `src/main/java/com/scb/backup/dao/BackupDaoService.java`
**Responsibility:** Database operations for backup tracking

**Key Methods:**
- `insertFullBackupDetails()` - Insert into full_backup_tracker
- `updateFullBackupWithBaseUuid()` - Update base UUID
- `getBaseBackupUuidFromFullTracker()` - Fetch base UUID
- `insertIncrementalBackupDetails()` - Insert into incremental_backup_tracker
- `updateIncrementalBackupStatus()` - Update incremental status

---

### 8. BackupValidationService
**Location:** `src/main/java/com/scb/backup/service/BackupValidationService.java`
**Responsibility:** Validates batch parameters

**Validations:**
- ✅ batchId is not null/empty
- ✅ batchCategoryCode is not null/empty
- ✅ batchTransactionDate is valid (YYYYMMDD)
- ✅ cronExpression is valid (if provided)

---

## 📡 API Reference

### POST /backupProcess

**Description:** Initiates a database backup operation

**Request Body:**
```json
{
  "batchId": "BATCH_001",
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20260309",
  "payload": {
    "cronExpression": "0 0 2 1 * *"
  }
}
```

**Request Parameters:**

| Parameter | Required | Type | Description | Example |
|-----------|----------|------|-------------|---------|
| `batchId` | ✅ Yes | String | Unique batch identifier | `"BATCH_001"` |
| `batchCategoryCode` | ✅ Yes | String | Backup category code | `"HWA_EPR_DB_BACKUP_FULL"` |
| `batchTransactionDate` | ✅ Yes | String | Business date (YYYYMMDD) | `"20260309"` |
| `payload.cronExpression` | ✅ Yes | String | Cron expression (6 fields) | `"0 0 2 1 * *"` |

**Response:**
```json
{
  "batchId": "BATCH_001",
  "executionStatus": "SUCCESS",
  "extensionFields": {}
}
```

**Status Codes:**
- `200 OK` - Backup initiated successfully
- `400 Bad Request` - Invalid parameters
- `500 Internal Server Error` - Technical error

---



## 🗄️ Database Schema

### Table 1: full_backup_tracker

**Purpose:** Track full backup operations and store base UUID

**Schema:**
```sql
CREATE TABLE epricing.full_backup_tracker (
    batch_id VARCHAR(255) NOT NULL,
    category_code VARCHAR(255) NOT NULL,
    business_date DATE NOT NULL,
    backup_period VARCHAR(100) NOT NULL,
    backup_interval VARCHAR(100),
    backup_status VARCHAR(50) NOT NULL,
    task_uuid VARCHAR(255),
    base_backup_uuid VARCHAR(255),
    full_backup_response TEXT,
    error_message TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    db_name VARCHAR(255),
    PRIMARY KEY (batch_id, category_code)
);

CREATE INDEX idx_full_backup_period ON epricing.full_backup_tracker(backup_period);
CREATE INDEX idx_full_backup_interval ON epricing.full_backup_tracker(backup_interval);
```

**Key Columns:**

| Column | Type | Description | Example |
|--------|------|-------------|---------|
| `batch_id` | VARCHAR | Unique batch identifier | `"BATCH_001"` |
| `category_code` | VARCHAR | Backup category | `"HWA_EPR_DB_BACKUP_FULL"` |
| `business_date` | DATE | Business date | `2026-03-09` |
| `backup_period` | VARCHAR | Backup period identifier | `"2026-04-01-0200"` |
| `backup_interval` | VARCHAR | Human-readable interval | `"2026-04-01 02:00:00 (Cron: 0 0 2 1 * *)"` |
| `backup_status` | VARCHAR | Status | `"IN_PROGRESS"/"SUCCESS"/"FAILED"` |
| `task_uuid` | VARCHAR | YBA task UUID | `"abc-123-def"` |
| `base_backup_uuid` | VARCHAR | Base backup UUID | `"xyz-456-uvw"` |
| `full_backup_response` | TEXT | YBA API response JSON | `{...}` |
| `db_name` | VARCHAR | Database name | `"hbl_gcp_uat_epr_db"` |

**Lifecycle:**
1. Insert with `IN_PROGRESS` status when backup starts
2. Update with `full_backup_response` after YBA API call
3. BackupPollerService polls for completion
4. Update with `base_backup_uuid` and `SUCCESS` status when complete

---

### Table 2: incremental_backup_tracker

**Purpose:** Track incremental backup operations

**Schema:**
```sql
CREATE TABLE epricing.incremental_backup_tracker (
    batch_id VARCHAR(255) NOT NULL,
    category_code VARCHAR(255) NOT NULL,
    business_date DATE NOT NULL,
    backup_period VARCHAR(100) NOT NULL,
    backup_interval VARCHAR(100),
    backup_status VARCHAR(50) NOT NULL,
    task_uuid VARCHAR(255),
    base_backup_uuid VARCHAR(255),
    incremental_backup_response TEXT,
    error_message TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    PRIMARY KEY (batch_id, category_code)
);

CREATE INDEX idx_incremental_backup_period ON epricing.incremental_backup_tracker(backup_period);
CREATE INDEX idx_incremental_backup_interval ON epricing.incremental_backup_tracker(backup_interval);
```

**Key Columns:**

| Column | Type | Description | Example |
|--------|------|-------------|---------|
| `batch_id` | VARCHAR | Unique batch identifier | `"BATCH_002"` |
| `category_code` | VARCHAR | Backup category | `"HWA_EPR_DB_BACKUP_INCRE"` |
| `backup_period` | VARCHAR | Backup period identifier | `"2026-04-01-0200"` |
| `base_backup_uuid` | VARCHAR | Reference to full backup | `"xyz-456-uvw"` |
| `backup_status` | VARCHAR | Status | `"IN_PROGRESS"/"SUCCESS"/"FAILED"` |
| `incremental_backup_response` | TEXT | YBA API response JSON | `{...}` |

**Lifecycle:**
1. Fetch `base_backup_uuid` from full_backup_tracker
2. Insert with `IN_PROGRESS` status
3. Call YBA API with `baseBackupUUID`
4. Update with `incremental_backup_response`
5. BackupPollerService polls for completion
6. Update status to `SUCCESS` when complete

---

### SQL Queries (from application.yml)

**Insert Full Backup:**
```sql
INSERT INTO epricing.full_backup_tracker(
    batch_id, category_code, business_date, backup_period,
    backup_interval, backup_status, task_uuid,
    full_backup_response, start_time, db_name
) VALUES(
    :batch_id, :batchCategoryCode, :businessDate, :backupPeriod,
    :backupInterval, :backupStatus, :taskUUID,
    :fullBackupResponse, now(), :dbName
);
```

**Update Full Backup with Base UUID:**
```sql
UPDATE epricing.full_backup_tracker
SET base_backup_uuid = :baseBackupUUID,
    backup_status = :backupStatus,
    end_time = now()
WHERE category_code = :categoryCode
  AND backup_period = :backupPeriod
  AND batch_id = :batchId;
```

**Get Base Backup UUID:**
```sql
SELECT base_backup_uuid
FROM epricing.full_backup_tracker
WHERE backup_period = :backupPeriod
  AND db_name = :dbName
  AND backup_status = 'SUCCESS'
ORDER BY end_time DESC
LIMIT 1;
```

**Insert Incremental Backup:**
```sql
INSERT INTO epricing.incremental_backup_tracker(
    batch_id, category_code, business_date, backup_period,
    backup_interval, backup_status, start_time
) VALUES(
    :batchId, :categoryCode, :businessDate, :backupPeriod,
    :backupInterval, :backupStatus, now()
);
```

**Update Incremental Backup Status:**
```sql
UPDATE epricing.incremental_backup_tracker
SET backup_status = :backupStatus,
    base_backup_uuid = :baseBackupUUID,
    task_uuid = :taskUUID,
    incremental_backup_response = :backupResponse,
    end_time = now()
WHERE batch_id = :batchId
  AND category_code = :categoryCode;
```

---

## ⚙️ Configuration

### Application Configuration (application.yml)

**Server Configuration:**
```yaml
server:
  port: ${PORT:10022}
```

**Database Configuration:**
```yaml
spring:
  datasource:
    url: ${DATASOURCE_URL:jdbc:yugabytedb://localhost:5433/yugabyte}
    username: ${DATASOURCE_USERNAME:admin}
    password: ${DATASOURCE_PASSWORD}
    driver-class-name: com.yugabyte.Driver
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
```

**YBA Configuration:**
```yaml
yba:
  hwa-base-urls: ${YBA_BASE_URLS:https://yba-host}
  hwa-customer-id: ${YBA_CUSTOMER_ID:8d0093b7}
  databases:
    HWA_EPR_DB_BACKUP_FULL:
      full-backup-url: ${yba.hwa-base-urls}/api/v1/customers/${yba.hwa-customer-id}/backups
      job-completion-check-url: ${yba.hwa-base-urls}/api/v1/customers/${yba.hwa-customer-id}/tasks/{taskUuid}
      last-backup-url: ${yba.hwa-base-urls}/api/v1/customers/${yba.hwa-customer-id}/backups/page?limit=1&direction=DESC
      storage-config-uuid: ${YBA_STORAGE_CONFIG_UUID}
      api-token: ${YBA_API_TOKEN}
      universe-uuid: ${YBA_UNIVERSE_UUID}
      backup-type: PGSQL_TABLE_TYPE
      backup-category-type: full_backup
      db-name: ${YBA_DB_NAME:hbl_gcp_uat_epr_db}
      expiry-ms: 86400000

    HWA_EPR_DB_BACKUP_INCRE:
      incremental-backup-url: ${yba.hwa-base-urls}/api/v1/customers/${yba.hwa-customer-id}/backups
      job-completion-check-url: ${yba.hwa-base-urls}/api/v1/customers/${yba.hwa-customer-id}/tasks/{taskUuid}
      storage-config-uuid: ${YBA_STORAGE_CONFIG_UUID}
      api-token: ${YBA_API_TOKEN}
      universe-uuid: ${YBA_UNIVERSE_UUID}
      backup-type: PGSQL_TABLE_TYPE
      backup-category-type: incremental_backup
      db-name: ${YBA_DB_NAME:hbl_gcp_uat_epr_db}
      expiry-ms: 86400000
```

**Backup Poller Configuration:**
```yaml
backup:
  poller:
    enabled: ${BACKUP_POLLER_ENABLED:true}
    initial-delay-ms: ${BACKUP_POLLER_INITIAL_DELAY_MS:5000}
    polling-interval-ms: ${BACKUP_POLLER_INTERVAL_MS:30000}
    thread-pool-size: ${BACKUP_POLLER_THREAD_POOL_SIZE:5}
```

---

### Environment Variables

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `APP_ENV` | Environment (dev/uat/prod) | ✅ Yes | `dev` |
| `PORT` | Server port | No | `10022` |
| `DATASOURCE_URL` | Database connection URL | ✅ Yes | `jdbc:yugabytedb://localhost:5433/yugabyte` |
| `DATASOURCE_USERNAME` | Database username | ✅ Yes | `admin` |
| `DATASOURCE_PASSWORD` | Database password | ✅ Yes | `password` |
| `YBA_BASE_URLS` | YBA base URL | ✅ Yes | `https://yba-host` |
| `YBA_CUSTOMER_ID` | YBA customer ID | ✅ Yes | `8d0093b7` |
| `YBA_API_TOKEN` | YBA authentication token | ✅ Yes | `3.6c54f209...` |
| `YBA_UNIVERSE_UUID` | YBA universe identifier | ✅ Yes | `b055c770...` |
| `YBA_STORAGE_CONFIG_UUID` | YBA storage configuration | ✅ Yes | `d4a25962...` |
| `YBA_DB_NAME` | Database name | ✅ Yes | `hbl_gcp_uat_epr_db` |
| `BACKUP_POLLER_ENABLED` | Enable backup poller | No | `true` |
| `BACKUP_POLLER_INTERVAL_MS` | Polling interval (ms) | No | `30000` |

---

## 🔄 Backup Workflows

### Full Backup Workflow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. API Request Received                                     │
│    POST /backupProcess                                      │
│    {cronExpression: "0 0 2 1 * *"}                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. BackupService.process()                                  │
│    - Extract cronExpression from payload                    │
│    - Add to batchParams                                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. CronExpressionParser.calculateBackupPeriod()            │
│    Input: "0 0 2 1 * *"                                    │
│    Output: "2026-04-01-0200"                               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. YbaClient.fullBackup()                                   │
│    - Insert into full_backup_tracker (IN_PROGRESS)          │
│    - Call YBA API: POST /backups                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. YBA API Response                                         │
│    {taskUUID: "abc-123", resourceUUID: "xyz-456"}          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Update full_backup_tracker                               │
│    - Set task_uuid = "abc-123"                             │
│    - Set full_backup_response = {...}                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. BackupPollerService (Async)                             │
│    - Polls every 30 seconds                                 │
│    - GET /tasks/{taskUUID}                                  │
│    - Check job status                                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 8. Job Complete                                             │
│    - Extract base_backup_uuid from response                 │
│    - Update full_backup_tracker:                            │
│      * base_backup_uuid = "xyz-456"                        │
│      * backup_status = "SUCCESS"                           │
│      * end_time = now()                                    │
└─────────────────────────────────────────────────────────────┘
```

---

### Incremental Backup Workflow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. API Request Received                                     │
│    POST /backupProcess                                      │
│    {cronExpression: "0 0 2 1 * *"}                         │
│    Category: HWA_EPR_DB_BACKUP_INCRE                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Calculate Backup Period                                  │
│    "2026-04-01-0200"                                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Fetch Base Backup UUID                                   │
│    SELECT base_backup_uuid                                  │
│    FROM full_backup_tracker                                 │
│    WHERE backup_period = "2026-04-01-0200"                 │
│      AND backup_status = 'SUCCESS'                         │
│    Result: "xyz-456"                                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Insert into incremental_backup_tracker                   │
│    - backup_status = "IN_PROGRESS"                         │
│    - backup_period = "2026-04-01-0200"                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Call YBA API                                             │
│    POST /backups                                            │
│    {baseBackupUUID: "xyz-456", ...}                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Update incremental_backup_tracker                        │
│    - task_uuid = "def-789"                                 │
│    - base_backup_uuid = "xyz-456"                          │
│    - incremental_backup_response = {...}                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│ 7. BackupPollerService                                     │
│    - Polls for job completion                               │
│    - Updates status to SUCCESS when complete                │
└─────────────────────────────────────────────────────────────┘
```

---


## 🕐 Cron Expression Guide

For detailed cron expression examples and usage, see **[CRON_EXPRESSION_GUIDE.md](CRON_EXPRESSION_GUIDE.md)**

### Quick Reference

| Schedule Type | Cron Expression | Backup Period Example |
|---------------|-----------------|----------------------|
| **Monthly** | `0 0 2 1 * *` | `2026-04-01-0200` |
| **Weekly** | `0 0 2 * * MON` | `2026-03-09-0200` |
| **Bi-Weekly** | `0 0 2 1,15 * *` | `2026-03-15-0200` |
| **Quarterly** | `0 0 2 1 1,4,7,10 *` | `2026-04-01-0200` |
| **Custom Date** | `0 0 3 15 6 *` | `2026-06-15-0300` |

---

## 🚨 Error Handling

### Exception Types

| Error Type | Cause | HTTP Status | Handling |
|------------|-------|-------------|----------|
| **Validation Error** | Missing/invalid parameters | 400 | Throw IllegalArgumentException |
| **Configuration Error** | Missing YBA config | 500 | Throw DbBackupException |
| **API Error** | YBA API failure | 500 | Log error, update status to FAILED |
| **Database Error** | DB insert/update failure | 500 | Throw DbBackupException |
| **Cron Expression Error** | Invalid cron syntax | 400 | Throw IllegalArgumentException |

---

### Error Response Examples

**Missing Cron Expression:**
```json
{
  "error": "cronExpression is required in the request payload for category: HWA_EPR_DB_BACKUP_FULL",
  "status": 400
}
```

**Invalid Cron Expression:**
```json
{
  "error": "Invalid cron expression: INVALID. Error: Cron expression must consist of 6 fields",
  "status": 400
}
```

**YBA API Failure:**
```json
{
  "error": "Failed to initiate backup via YBA API",
  "status": 500,
  "details": "Connection timeout"
}
```

---

### Logging

**Key Log Points:**
- ✅ Backup request received
- ✅ Cron expression parsed
- ✅ Backup period calculated
- ✅ Backup initiated (IN_PROGRESS)
- ✅ YBA API call success/failure
- ✅ Backup status updated (SUCCESS/FAILED)
- ✅ Error scenarios with stack traces

**Log Example:**
```
INFO  - Backup request received for category: HWA_EPR_DB_BACKUP_FULL
INFO  - Calculated backup period: 2026-04-01-0200 for cron: 0 0 2 1 * *
INFO  - Backup initiated with task UUID: abc-123-def
INFO  - Backup status updated to SUCCESS for batch: BATCH_001
```

---

## 🚀 Deployment

### Build

**Using Gradle:**
```bash
./gradlew clean build
```

**Skip Tests:**
```bash
./gradlew clean build -x test
```

**Build with Tests:**
```bash
./gradlew clean build test
```

---

### Run Locally

**Using JAR:**
```bash
java -jar build/libs/backup-orchestrator-service-0.0.2-SNAPSHOT.jar
```

**With Environment Variables:**
```bash
export APP_ENV=dev
export DATASOURCE_URL=jdbc:yugabytedb://localhost:5433/yugabyte
export DATASOURCE_USERNAME=admin
export DATASOURCE_PASSWORD=password
export YBA_BASE_URLS=https://yba-host
export YBA_CUSTOMER_ID=8d0093b7
export YBA_API_TOKEN=your-token
export YBA_UNIVERSE_UUID=your-uuid
export YBA_STORAGE_CONFIG_UUID=your-storage-uuid
export YBA_DB_NAME=hbl_gcp_uat_epr_db

java -jar build/libs/backup-orchestrator-service-0.0.2-SNAPSHOT.jar
```

---

### Docker Build

**Build Image:**
```bash
docker build -t backup-orchestrator-service:3.0 .
```

**Dockerfile:**
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY build/libs/backup-orchestrator-service-0.0.2-SNAPSHOT.jar app.jar
EXPOSE 10022
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### Docker Run

```bash
docker run -p 10022:10022 \
  -e APP_ENV=dev \
  -e DATASOURCE_URL=jdbc:yugabytedb://localhost:5433/yugabyte \
  -e DATASOURCE_USERNAME=admin \
  -e DATASOURCE_PASSWORD=password \
  -e YBA_BASE_URLS=https://yba-host \
  -e YBA_CUSTOMER_ID=8d0093b7 \
  -e YBA_API_TOKEN=your-token \
  -e YBA_UNIVERSE_UUID=your-uuid \
  -e YBA_STORAGE_CONFIG_UUID=your-storage-uuid \
  -e YBA_DB_NAME=hbl_gcp_uat_epr_db \
  backup-orchestrator-service:3.0
```

---

### Kubernetes Deployment

**deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backup-orchestrator-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: backup-orchestrator-service
  template:
    metadata:
      labels:
        app: backup-orchestrator-service
    spec:
      containers:
      - name: backup-orchestrator-service
        image: backup-orchestrator-service:3.0
        ports:
        - containerPort: 10022
        env:
        - name: APP_ENV
          value: "prod"
        - name: DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: backup-secrets
              key: datasource-url
        - name: DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: backup-secrets
              key: datasource-username
        - name: DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: backup-secrets
              key: datasource-password
        - name: YBA_API_TOKEN
          valueFrom:
            secretKeyRef:
              name: backup-secrets
              key: yba-api-token
```

---

## 🧪 Testing

### Unit Tests

**Run All Tests:**
```bash
./gradlew test
```

**Run Specific Test Class:**
```bash
./gradlew test --tests BackupServiceTest
```

**Test Coverage:**
```bash
./gradlew jacocoTestReport
```

**Coverage Report Location:**
```
build/reports/jacoco/test/html/index.html
```

---

### Integration Tests

**Test Full Backup Flow:**
```bash
curl -X POST http://localhost:10022/backupProcess \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "TEST_FULL_001",
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchTransactionDate": "20260309",
    "payload": {
      "cronExpression": "0 0 2 1 * *"
    }
  }'
```

**Test Incremental Backup Flow:**
```bash
curl -X POST http://localhost:10022/backupProcess \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "TEST_INCR_001",
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_INCRE",
    "batchTransactionDate": "20260309",
    "payload": {
      "cronExpression": "0 0 2 1 * *"
    }
  }'
```

---

### Verify Database Records

**Check Full Backup Tracker:**
```sql
SELECT
    batch_id,
    category_code,
    backup_period,
    backup_interval,
    backup_status,
    base_backup_uuid
FROM epricing.full_backup_tracker
WHERE batch_id = 'TEST_FULL_001';
```

**Check Incremental Backup Tracker:**
```sql
SELECT
    batch_id,
    category_code,
    backup_period,
    backup_status,
    base_backup_uuid
FROM epricing.incremental_backup_tracker
WHERE batch_id = 'TEST_INCR_001';
```

---

## 📊 Monitoring & Metrics

### Key Metrics to Monitor

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| **Backup Success Rate** | % of successful backups | < 95% |
| **Backup Duration** | Time to complete backup | > 30 minutes |
| **API Response Time** | YBA API call latency | > 5 seconds |
| **Error Count** | Number of failed backups | > 5 per hour |
| **Poller Lag** | Time between job completion and status update | > 2 minutes |

---

### Health Check Endpoint

**Endpoint:**
```
GET /actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

---

## 📚 References

- **Repository:** [https://github.com/SCBPersonal/backup-service](https://github.com/SCBPersonal/backup-service)
- **Cron Expression Guide:** [CRON_EXPRESSION_GUIDE.md](CRON_EXPRESSION_GUIDE.md)
- **Spring WebFlux:** [https://docs.spring.io/spring-framework/reference/web/webflux.html](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- **YugabyteDB Anywhere API:** YBA REST API Documentation
- **Spring Cron Documentation:** [https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html)

---

## 📝 License

Copyright © 2026 SCB Personal

---

**Version:** 3.0
**Last Updated:** 2026-03-09
**Author:** SCB ePricing Team

