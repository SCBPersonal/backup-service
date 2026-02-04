# Backup Orchestrator Service

## Overview

The **Backup Orchestrator Service** is a Spring Boot-based microservice that orchestrates database backup operations for YugabyteDB clusters through integration with **YugabyteDB Anywhere (YBA)** REST API.

## Key Features

- ✅ Automated full and incremental backup orchestration
- ✅ Multi-database configuration support
- ✅ Real-time backup status tracking
- ✅ Integration with YugabyteDB Anywhere (YBA) API
- ✅ Reactive programming with Spring WebFlux
- ✅ Comprehensive error handling and validation

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.x |
| Language | Java | 17+ |
| Build Tool | Gradle | 8.11.1 |
| Database | YugabyteDB | - |
| Reactive | Spring WebFlux | - |
| HTTP Client | WebClient | - |

## Architecture

### High-Level Architecture

```
┌─────────────────┐
│ Scheduler/Client│
└────────┬────────┘
         │ POST /backupProcess
         ▼
┌─────────────────────────────────────┐
│      BackupController               │
└────────┬────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│      BackupService                  │
│  ┌──────────────────────────────┐   │
│  │ - Validation                 │   │
│  │ - Config Resolution          │   │
│  │ - Backup Initiation          │   │
│  │ - Status Tracking            │   │
│  └──────────────────────────────┘   │
└────────┬────────────────────────────┘
         │
         ├──────────────┬──────────────┐
         ▼              ▼              ▼
┌──────────────┐ ┌──────────┐ ┌──────────────┐
│  YbaClient   │ │   DAO    │ │ Validation   │
└──────┬───────┘ └────┬─────┘ └──────────────┘
       │              │
       ▼              ▼
┌──────────────┐ ┌──────────────┐
│  YBA API     │ │  YugabyteDB  │
└──────────────┘ └──────────────┘
```

## Core Components

### 1. BackupController
- **Package**: `com.scb.backup`
- **Endpoint**: `POST /backupProcess`
- **Responsibility**: REST API endpoint for triggering backup processes

### 2. BackupService
- **Package**: `com.scb.backup.service`
- **Responsibility**: Core backup orchestration logic
- **Key Methods**:
  - `process()` - Main entry point
  - `processBackup()` - Reactive backup workflow
  - `handleBackupSuccess()` - Success handler
  - `handleBackupFailure()` - Error handler

### 3. YbaClient
- **Package**: `com.scb.backup.client`
- **Responsibility**: HTTP client for YugabyteDB Anywhere API
- **Key Methods**:
  - `backupInitiate()` - Initiates backup based on type
  - `fullBackup()` - Executes full backup
  - `incrementalBackup()` - Executes incremental backup
  - `fetchLastBackup()` - Retrieves last backup metadata

### 4. YbaConfigService
- **Package**: `com.scb.backup.service`
- **Responsibility**: Resolves database-specific backup configurations

### 5. BackupDaoService
- **Package**: `com.scb.backup.dao`
- **Responsibility**: Database operations for backup tracking

### 6. BackupValidationService
- **Package**: `com.scb.backup.service`
- **Responsibility**: Validates batch parameters and backup configurations

## Configuration

### Application Configuration (application.yml)

```yaml
server.port: 8989

spring:
  main:
    banner-mode: "off"
  datasource:
    url: ${DATASOURCE_URL}
    username: ${DATASOURCE_USERNAME}
    password: ${DATASOURCE_PASSWORD}
    driver-class-name: com.yugabyte.Driver

yba:
  databases:
    db1:
      full-backup-url: ${YBA_FULL_BACKUP_URL}
      storage-config-uuid: ${YBA_STORAGE_CONFIG_UUID}
      api-token: ${YBA_API_TOKEN}
      universe-uuid: ${YBA_UNIVERSE_UUID}
      backup-type: PGSQL_TABLE_TYPE
      backup-category-type: full_backup
      db-name: ${YBA_DB_NAME}
      expiry-ms: 86400000
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| APP_ENV | Environment (dev/uat/prod) | Yes |
| DATASOURCE_URL | Database connection URL | Yes |
| DATASOURCE_USERNAME | Database username | Yes |
| DATASOURCE_PASSWORD | Database password | Yes |
| YBA_FULL_BACKUP_URL | YBA full backup endpoint | Yes |
| YBA_API_TOKEN | YBA authentication token | Yes |
| YBA_UNIVERSE_UUID | YBA universe identifier | Yes |
| YBA_STORAGE_CONFIG_UUID | YBA storage configuration | Yes |

## API Contracts

### Backup Request

```json
POST /backupProcess
Content-Type: application/json

{
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20250114"
}
```

### YBA Full Backup Request

```json
POST {fullBackupUrl}
Headers:
  X-AUTH-YW-API-TOKEN: {apiToken}
  Content-Type: application/json

Body:
{
  "universeUUID": "{universeUuid}",
  "storageConfigUUID": "{storageConfigUuid}",
  "backupType": "PGSQL_TABLE_TYPE",
  "timeBeforeDelete": 86400000
}
```

### YBA Incremental Backup Request

```json
POST {incrementalBackupUrl}
Headers:
  X-AUTH-YW-API-TOKEN: {apiToken}
  Content-Type: application/json

Body:
{
  "baseBackupUUID": "{lastBackupUuid}",
  "universeUUID": "{universeUuid}",
  "storageConfigUUID": "{storageConfigUuid}",
  "backupType": "PGSQL_TABLE_TYPE",
  "timeBeforeDelete": 86400000
}
```

## Backup Process Flows

### Full Backup Flow

1. Client sends POST request to `/backupProcess`
2. BackupController receives request and delegates to BackupService
3. BackupService validates batch parameters
4. YbaConfigService resolves database configuration
5. BackupDaoService inserts backup record with status `IN_PROGRESS`
6. YbaClient initiates full backup via YBA API
7. YBA API returns task UUID and resource UUID
8. BackupDaoService updates backup status to `SUCCESS`
9. Response returned to client

### Incremental Backup Flow

1. Client sends POST request to `/backupProcess`
2. BackupService validates and resolves configuration
3. YbaClient fetches last backup UUID from YBA API
4. YbaClient extracts `baseBackupUUID` from response
5. YbaClient initiates incremental backup with `baseBackupUUID`
6. YBA API returns task UUID and resource UUID
7. BackupDaoService updates backup status to `SUCCESS`
8. Response returned to client

## Database Schema

```sql
CREATE TABLE epricing.batch_db_schedule_event_tracker (
    batch_id VARCHAR,
    backup_job_categorycode VARCHAR,
    backup_status VARCHAR,
    backup_type VARCHAR,
    business_date DATE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    backup_response TEXT
);
```

## Build & Deployment

### Build

```bash
./gradlew clean build
```

### Run Locally

```bash
java -jar build/libs/backup-orchestrator-service-0.0.2-SNAPSHOT.jar
```

### Docker Build

```bash
docker build -t backup-orchestrator-service:latest .
```

### Docker Run

```bash
docker run -p 8989:8989 \
  -e DATASOURCE_URL=jdbc:yugabytedb://localhost:5433/yugabyte \
  -e DATASOURCE_USERNAME=yugabyte \
  -e DATASOURCE_PASSWORD=yugabyte \
  -e YBA_FULL_BACKUP_URL=https://yba-host/api/v1/customers/xxx/backups \
  -e YBA_API_TOKEN=your-api-token \
  -e YBA_UNIVERSE_UUID=your-universe-uuid \
  -e YBA_STORAGE_CONFIG_UUID=your-storage-config-uuid \
  backup-orchestrator-service:latest
```

## Error Handling

### Exception Types

| Error Type | Cause | Handling |
|------------|-------|----------|
| Validation Error | Missing/invalid parameters | Throw IllegalArgumentException |
| Configuration Error | Missing YBA config | Throw DbBackupException |
| API Error | YBA API failure | Log error, update status to FAILED |
| Database Error | DB insert/update failure | Throw DbBackupException |

## Monitoring & Logging

### Key Log Points

- Backup request received
- Backup initiated (IN_PROGRESS)
- YBA API call success/failure
- Backup status updated (SUCCESS/FAILED)
- Error scenarios with stack traces

### Metrics to Monitor

- Backup success rate
- Backup duration
- API response times
- Error counts by type

## References

- **Repository**: [https://github.com/SCBPersonal/backup-service](https://github.com/SCBPersonal/backup-service)
- **Confluence Documentation**: [Backup Orchestrator Service - Technical Documentation](https://fyndna.atlassian.net/wiki/spaces/FE/pages/1829830699)
- **YugabyteDB Anywhere API**: YBA REST API Documentation
- **Spring WebFlux**: [https://docs.spring.io/spring-framework/reference/web/webflux.html](https://docs.spring.io/spring-framework/reference/web/webflux.html)

## License

Copyright © 2026 SCB Personal


