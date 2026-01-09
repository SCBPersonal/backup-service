# Backup Orchestrator Service - Master Technical Specification

## Document Purpose
This document serves as a comprehensive technical specification for the **backup-orchestrator-service**, designed for developer onboarding and as context for making modifications to the backup service. It covers architecture, components, data flows, configurations, API contracts, database schemas, error handling, dependencies, and deployment.

---

## 1. SERVICE OVERVIEW

### 1.1 Purpose
The **backup-orchestrator-service** is a Spring Boot-based microservice that orchestrates database backup operations for YugabyteDB clusters through integration with **YugabyteDB Anywhere (YBA)** REST API. It provides:

- **Automated backup scheduling** via batch processing framework
- **Multi-database support** with configurable backup strategies (full/incremental)
- **Backup job tracking** with persistent state management in YugabyteDB
- **Reactive processing** using Spring WebFlux and Project Reactor
- **Resilient operations** with retry mechanisms for transient failures

### 1.2 Architecture Overview
The service follows a **layered reactive architecture**:

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                            │
│              (BackupController - Port 8989)                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│                  Service Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │BackupService │  │YbaConfigSvc  │  │ValidationSvc │      │
│  │(Batch Exec)  │  │(Config Mgmt) │  │(Validation)  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              Integration Layer                               │
│  ┌──────────────┐              ┌──────────────┐            │
│  │  YbaClient   │              │BackupDaoSvc  │            │
│  │(WebClient)   │              │(JDBC)        │            │
│  └──────┬───────┘              └──────┬───────┘            │
└─────────┼──────────────────────────────┼───────────────────┘
          │                              │
┌─────────▼──────────┐         ┌─────────▼──────────┐
│  YugabyteDB        │         │  YugabyteDB        │
│  Anywhere (YBA)    │         │  (Tracking DB)     │
│  REST API          │         │  batch_db_schedule │
└────────────────────┘         └────────────────────┘
```

### 1.3 Key Responsibilities
1. **Backup Orchestration**: Initiate full and incremental backups via YBA API
2. **Configuration Management**: Resolve database-specific backup configurations dynamically
3. **State Tracking**: Persist backup job status (IN_PROGRESS, SUCCESS, FAILED) to database
4. **Validation**: Validate batch parameters and backup configurations before execution
5. **Error Handling**: Handle failures gracefully with retry mechanisms and exception tracking
6. **Batch Integration**: Extend generic batch framework for backup-specific workflows

---

## 2. CORE COMPONENTS

### 2.1 BackupController
**Location**: `src/main/java/com/hdfc/backup/BackupController.java`

**Responsibility**: REST API endpoint for triggering backup processes

**Key Methods**:
- `POST /backupProcess`: Accepts JSON payload, delegates to BackupService

**Implementation Details**:
```java
@PostMapping("/backupProcess")
public Mono<BatchStartResponse> backupProcess(@RequestBody String json) {
    return Mono.fromCallable(() -> {
        var response = backupService.execute(json);
        return response;
    }).doOnError(throwable -> log.error("Backup process failed - RequestID: {}", throwable));
}
```

**Design Notes**:
- Uses `Mono.fromCallable()` to wrap synchronous batch execution
- Logs errors but does not transform exceptions
- Timing logic present but incomplete (StopWatch not logged)

---

### 2.2 BackupService
**Location**: `src/main/java/com/hdfc/backup/service/BackupService.java`

**Responsibility**: Core backup orchestration logic, extends GenericBatchService

**Key Methods**:
- `process(String batchId, String businessDate, String categoryCode)`: Main entry point from batch framework
- `processBackup(String batchId, String businessDate, String categoryCode)`: Reactive backup workflow
- `handleBackupSuccess(...)`: Updates status to SUCCESS on completion
- `handleBackupFailure(...)`: Updates status to FAILED on error

**Reactive Flow**:
```java
public Mono<Void> processBackup(String batchId, String businessDate, String categoryCode) {
    Map<String, Object> batchParams = AppUtils.createBatchParams(batchId, businessDate, categoryCode);
    
    validationService.validateBatchParams(batchParams);
    
    return ybaClient.backupInitiate(categoryCode, batchParams)
        .flatMap(ydbRes -> handleBackupSuccess(batchId, businessDate, ydbRes))
        .onErrorResume(e -> handleBackupFailure(batchId, businessDate, e))
        .then();
}
```

**Integration Points**:
- **YbaClient**: Initiates backup via YBA REST API
- **BackupDaoService**: Persists backup status to database
- **BatchExecutionDao**: Updates batch execution status
- **BackupValidationService**: Validates parameters before execution

---

### 2.3 YbaClient
**Location**: `src/main/java/com/hdfc/backup/client/YbaClient.java`

**Responsibility**: HTTP client for YugabyteDB Anywhere (YBA) REST API integration

**Key Methods**:
- `backupInitiate(String categoryCode, Map<String, Object> batchParams)`: Determines backup type and initiates
- `fullBackup(YbaDynamicConfig config)`: Executes full backup via YBA API
- `incrementalBackup(YbaDynamicConfig config)`: Executes incremental backup
- `performIncrementalBackup(YbaDynamicConfig config)`: Fetches last backup UUID, then initiates incremental
- `fetchLastBackup(YbaDynamicConfig config)`: Retrieves most recent backup metadata

**HTTP Request Structure** (Full Backup):
```java
POST {fullBackupUrl}
Headers:
  X-AUTH-YW-API-TOKEN: {apiToken}
  Content-Type: application/json
Body:
{
  "universeUUID": "{universeUuid}",
  "storageConfigUUID": "{storageConfigUuid}",
  "backupType": "PGSQL_TABLE_TYPE",
  "timeBeforeDelete": {expiryMs}
}
```

**Incremental Backup Flow**:
1. Call `fetchLastBackup()` to get last backup UUID
2. Extract `baseBackupUUID` from response
3. Call `incrementalBackup()` with `baseBackupUUID` in request body

---

### 2.4 BackupDaoService
**Location**: `src/main/java/com/hdfc/backup/dao/BackupDaoService.java`

**Responsibility**: Database operations for backup job tracking

**Key Methods**:
- `insertBackupDetails(Map<String, Object> batchParams, String status, String backupType)`: Insert new backup record
- `updateBackupStatus(String batchId, String status, Date businessDate, String ydbResponse)`: Update backup status

**SQL Queries** (from `application.yml`):
```sql
-- Insert
INSERT INTO epricing.batch_db_schedule_event_tracker(
  batch_id, backup_job_categorycode, backup_status, backup_type,
  business_date, start_time
) VALUES (
  :batch_id, :batchCategoryCode, :backup_status, :backup_type,
  :businessDate, now()
)

-- Update
UPDATE batch_db_schedule_event_tracker
SET backup_status = :status,
    end_time = now(),
    backup_response = :ydbResponse
WHERE batch_id = :batch_id
  AND business_date = :businessDate
```

---

### 2.5 YbaConfigService
**Location**: `src/main/java/com/hdfc/backup/service/YbaConfigService.java`

**Responsibility**: Configuration resolution for database-specific YBA settings

**Key Methods**:
- `init()`: @PostConstruct method that builds configuration map from YbaProperties
- `resolve(String dbName)`: Retrieves YbaDynamicConfig by database name (case-insensitive)

**Configuration Loading**:
```java
@PostConstruct
public void init() {
    ybaProperties.getDatabases().forEach((key, value) -> {
        YbaDynamicConfig config = YbaDynamicConfig.builder()
            .apiToken(value.getApiToken())
            .universeUuid(value.getUniverseUuid())
            .storageConfigUuid(value.getStorageConfigUuid())
            .fullBackupUrl(value.getFullBackupUrl())
            .incrementalBackupUrl(value.getIncrementalBackupUrl())
            .lastBackupUrl(value.getLastBackupUrl())
            .backupType(value.getBackupType())
            .backupCategoryType(value.getBackupCategoryType())
            .dbName(value.getDbName())
            .expiryMs(value.getExpiryMs())
            .build();
        configMap.put(key.toUpperCase(), config);
    });
}
```

---

### 2.6 BackupValidationService
**Location**: `src/main/java/com/hdfc/backup/service/BackupValidationService.java`

**Responsibility**: Validate batch parameters and backup configurations

**Validation Rules**:
1. **Batch Parameters**: batchId, businessDate, categoryCode must not be null/empty
2. **Backup Configuration**: apiToken and universeUuid must be present
3. Throws `DbBackupException` on validation failure

---

### 2.7 GenericBatchService (Framework)
**Location**: `src/main/java/com/hdfcbank/epricing/batch/core/lib/service/GenericBatchService.java`

**Responsibility**: Abstract base class for batch processing

**Key Methods**:
- `execute(String json)`: Parses JSON payload, creates/retrieves batch, calls `process()`
- `process(String batchId, String businessDate, String categoryCode)`: Abstract method implemented by BackupService

**JSON Payload Structure**:
```json
{
  "batchExecutionId": "BATCH_001",  // Optional: if present, retrieves existing batch
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
  "batchTransactionDate": "20250325"
}
```

**Batch Lifecycle**:
1. Parse JSON using JPathUtils
2. Check if `batchExecutionId` exists → retrieve existing batch
3. If not, create new batch with generated UUID
4. Call `process()` method (implemented by BackupService)
5. Update batch status (COMPLETED/FAILED)

---

### 2.8 DatabaseRetryHandler (AOP)
**Location**: `src/main/java/com/hdfcbank/epricing/batch/core/lib/util/DatabaseRetryHandler.java`

**Responsibility**: Aspect-Oriented Programming (AOP) for automatic retry on database operations

**Pointcut**: All methods in classes annotated with `@Repository`

**Retry Policy** (CustomRetryPolicy):
- **Retryable SQL States**: 40001 (serialization failure), 40P01 (deadlock), 57P01 (admin shutdown), 08006 (connection failure), XX000 (internal error)
- **Max Retry Attempts**: 3 (configurable)
- **Backoff**: Exponential backoff with initial interval 1000ms, multiplier 2.0, max interval 10000ms

**Implementation**:
```java
@Around("@within(org.springframework.stereotype.Repository) && execution(* *(..))")
public Object aroundRepositoryMethods(ProceedingJoinPoint pjp) throws Throwable {
    return retryTemplate.execute(context -> {
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            log.error("Retry starts for method: {}, if policy satisfies for exception: {}",
                      pjp.getSignature(), t);
            throw new RuntimeException(t);
        }
    }, context -> {
        log.warn("All retries failed for method: " + pjp.getSignature());
        throw new BatchExecutionException("All retries exhausted", context.getLastThrowable());
    });
}
```

---

### 2.9 GlobalExceptionHandler
**Location**: `src/main/java/com/hdfc/backup/exception/GlobalExceptionHandler.java`

**Responsibility**: Global exception handling for REST controllers

**Exception Handlers**:
- `@ExceptionHandler(DbBackupException.class)`: Catches backup-specific exceptions

**Response Structure**:
```java
BatchStartResponse errorResponse = new BatchStartResponse();
errorResponse.setStatus(AppConstants.BATCH_FAILED_STATUS);
Map<String, Object> extensionFields = new HashMap<>();
extensionFields.put(AppConstants.ERROR_MESSAGE, e.getMessage());
errorResponse.setExtensionFields(extensionFields);
return new ResponseEntity<>(errorResponse, HttpStatus.OK); // ⚠️ Should be 500
```

**Design Issue**: Returns HTTP 200 for errors (should return 500 for server errors)

---

### 2.10 Utility Classes

#### AppUtils
**Location**: `src/main/java/com/hdfc/backup/utils/AppUtils.java`

**Methods**:
- `getBusinessDate(String date)`: Removes hyphens from date string (e.g., "2025-03-25" → "20250325")
- `toDate(String date)`: Parses date string using pattern "yyyyMMdd"
- `createBatchParams(String batchId, String businessDate, String categoryCode)`: Creates parameter map

#### JPathUtils
**Location**: `src/main/java/com/hdfc/backup/utils/JPathUtils.java`

**Methods**:
- `get(String json, String expression)`: Extracts values from JSON using JsonPath expressions

**Example**:
```java
String json = "{\"batchCategoryCode\": \"HWA_EPR_DB_BACKUP_FULL\"}";
Object value = JPathUtils.get(json, "$.batchCategoryCode");
// Returns: "HWA_EPR_DB_BACKUP_FULL"
```

#### AppConstants
**Location**: `src/main/java/com/hdfc/backup/utils/AppConstants.java`

**Constants**:
- **Batch Status**: BATCH_COMPLETED_STATUS, BATCH_FAILED_STATUS
- **Backup Status**: BACKUP_INPROGRESS_STATUS, BACKUP_SUCCESS_STATUS, BACKUP_FAILED_STATUS
- **Backup Types**: FULL_BACKUP ("full_backup"), INCREMENTAL_BACKUP ("incremental_backup")
- **Field Names**: BATCH_ID, BUSINESS_DATE, CATEGORY_CODE, ERROR_MESSAGE
- **Date Pattern**: "yyyyMMdd"
- **Timezone**: "Asia/Kolkata"

---

## 3. DATA FLOW

### 3.1 End-to-End Backup Request Flow

```
┌─────────────┐
│   Client    │
│  (Scheduler)│
└──────┬──────┘
       │ POST /backupProcess
       │ {"batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
       │  "batchTransactionDate": "20250325"}
       ▼
┌──────────────────────────────────────────────────────────┐
│ BackupController.backupProcess()                         │
│  - Wraps in Mono.fromCallable()                          │
│  - Calls backupService.execute(json)                     │
└──────┬───────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ GenericBatchService.execute()                            │
│  - Parse JSON (JPathUtils)                               │
│  - Create/retrieve batch (BatchExecutionDao)             │
│  - Extract: batchId, businessDate, categoryCode          │
└──────┬───────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ BackupService.process()                                  │
│  - Calls processBackup() and subscribes (fire-and-forget)│
└──────┬───────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ BackupService.processBackup() [Reactive Chain]          │
│  1. Create batch params (AppUtils.createBatchParams)     │
│  2. Validate params (BackupValidationService)            │
│  3. Initiate backup (YbaClient.backupInitiate)           │
│  4. Handle success/failure                               │
└──────┬───────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ YbaClient.backupInitiate()                               │
│  1. Resolve config (YbaConfigService.resolve)            │
│  2. Insert backup record (BackupDaoService)              │
│  3. Determine backup type (full/incremental)             │
│  4. Call fullBackup() or performIncrementalBackup()      │
└──────┬───────────────────────────────────────────────────┘
       │
       ├─── Full Backup ────────────────────────────────────┐
       │                                                     │
       │   ┌─────────────────────────────────────────────┐  │
       │   │ YbaClient.fullBackup()                      │  │
       │   │  - POST {fullBackupUrl}                     │  │
       │   │  - Headers: X-AUTH-YW-API-TOKEN             │  │
       │   │  - Body: universeUUID, storageConfigUUID,   │  │
       │   │          backupType, timeBeforeDelete       │  │
       │   └─────────────────────────────────────────────┘  │
       │                                                     │
       └─── Incremental Backup ────────────────────────────┐
                                                            │
           ┌─────────────────────────────────────────────┐  │
           │ YbaClient.performIncrementalBackup()        │  │
           │  1. fetchLastBackup() - GET {lastBackupUrl} │  │
           │  2. Extract baseBackupUUID from response    │  │
           │  3. incrementalBackup() - POST with UUID    │  │
           └─────────────────────────────────────────────┘  │
                                                            │
       ┌────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ YugabyteDB Anywhere (YBA) REST API                       │
│  - Processes backup request                              │
│  - Returns JSON response with backup job details         │
└──────┬───────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│ BackupService.handleBackupSuccess()                      │
│  - Update backup status to SUCCESS                       │
│  - Update batch status to COMPLETED                      │
│  - Store YBA response in backup_response column          │
└──────────────────────────────────────────────────────────┘
       │
       ▼ (On Error)
┌──────────────────────────────────────────────────────────┐
│ BackupService.handleBackupFailure()                      │
│  - Update backup status to FAILED                        │
│  - Update batch status to FAILED                         │
│  - Store error message in backup_response                │
└──────────────────────────────────────────────────────────┘
```

### 3.2 Reactive Chain Execution

The backup process uses **Project Reactor** for non-blocking, asynchronous execution:

```java
// Reactive chain in BackupService.processBackup()
Mono.fromCallable(() -> configService.resolve(categoryCode))  // 1. Resolve config
    .flatMap(config -> {
        backupDaoService.insertBackupDetails(...);             // 2. Insert DB record
        if (FULL_BACKUP.equals(config.getBackupCategoryType())) {
            return fullBackup(config);                         // 3a. Full backup
        } else {
            return performIncrementalBackup(config);           // 3b. Incremental backup
        }
    })
    .flatMap(ydbRes -> handleBackupSuccess(...))              // 4. Success handler
    .onErrorResume(e -> handleBackupFailure(...))             // 5. Error handler
    .then()                                                    // 6. Complete signal
    .subscribe();                                              // 7. Fire-and-forget
```

**Key Reactive Operators**:
- `Mono.fromCallable()`: Wraps synchronous operations
- `flatMap()`: Chains asynchronous operations
- `onErrorResume()`: Error handling without breaking the chain
- `then()`: Converts to `Mono<Void>` (completion signal only)
- `subscribe()`: Triggers execution (fire-and-forget pattern)

---

## 4. CONFIGURATION

### 4.1 Environment Variables

#### Required Variables (Per Database)

**Database 1 (db1)**:
```bash
YBA_FULL_BACKUP_URL=https://yba.example.com/api/v1/customers/cust123/backups
YBA_STORAGE_CONFIG_UUID=storage-uuid-123
YBA_API_TOKEN=api-token-secret-123
YBA_UNIVERSE_UUID=universe-uuid-123
YBA_DB_NAME=hbl_gcp_uat_epr_db
```

**Database 2 (db2)** - Incremental Backup:
```bash
YBA_FULL_BACKUP_URL_2=https://yba.example.com/api/v1/customers/cust456/backups
YBA_INCREMENTAL_BACKUP_URL_2=https://yba.example.com/api/v1/customers/cust456/backups/incremental
YBA_LAST_BACKUP_URL_2=https://yba.example.com/api/v1/customers/cust456/backups?limit=1&direction=DESC
YBA_STORAGE_CONFIG_UUID_2=storage-uuid-456
YBA_API_TOKEN_2=api-token-secret-456
YBA_UNIVERSE_UUID_2=universe-uuid-456
YBA_DB_NAME_2=hbl_gcp_uat_epr_db_2
```

**UAM Database**:
```bash
UAM_FULL_BACKUP_URL=https://yba.example.com/api/v1/customers/cust789/backups
UAM_INCREMENTAL_BACKUP_URL=https://yba.example.com/api/v1/customers/cust789/backups/incremental
UAM_LAST_BACKUP_URL=https://yba.example.com/api/v1/customers/cust789/backups?limit=1&direction=DESC
UAM_STORAGE_CONFIG_UUID=storage-uuid-789
UAM_API_TOKEN=api-token-secret-789
UAM_UNIVERSE_UUID=universe-uuid-789
UAM_DB_NAME=hbl_gcp_uat_epr_db
```

#### Database Connection Variables
```bash
DATASOURCE_URL=jdbc:yugabytedb://136.119.245.107:5432/postgresdb?ApplicationName=backupOrchestrator&currentSchema=epricing&load-balance=true
DATASOURCE_USERNAME=admin
DATASOURCE_PASSWORD=<secret>
DATASOURCE_DRIVER_CLASS_NAME=com.yugabyte.Driver
DATASOURCE_SSL_ENABLE=false
DATASOURCE_SSL_MODE=disable
```

#### Application Variables
```bash
APPLICATION_NAME=backup-orchestrator-service
APP_ENV=uat  # dev/uat/prod
SERVER_PORT=8989
```

### 4.2 YBA Configuration Properties

**Configuration Structure** (`application.yml`):
```yaml
yba:
  databases:
    db1:
      full-backup-url: ${YBA_FULL_BACKUP_URL:}
      storage-config-uuid: ${YBA_STORAGE_CONFIG_UUID:}
      api-token: ${YBA_API_TOKEN:}
      universe-uuid: ${YBA_UNIVERSE_UUID:}
      backup-type: PGSQL_TABLE_TYPE
      backup-category-type: full_backup
      db-name: ${YBA_DB_NAME:hbl_gcp_uat_epr_db}
      expiry-ms: 86400000  # 24 hours

    db2:
      full-backup-url: ${YBA_FULL_BACKUP_URL_2:}
      incremental-backup-url: ${YBA_INCREMENTAL_BACKUP_URL_2:}
      last-backup-url: ${YBA_LAST_BACKUP_URL_2:}
      storage-config-uuid: ${YBA_STORAGE_CONFIG_UUID_2:}
      api-token: ${YBA_API_TOKEN_2:}
      universe-uuid: ${YBA_UNIVERSE_UUID_2:}
      backup-type: PGSQL_TABLE_TYPE
      backup-category-type: incremental_backup
      db-name: ${YBA_DB_NAME_2:hbl_gcp_uat_epr_db_2}
      expiry-ms: 86400000

    uam-db:
      full-backup-url: ${UAM_FULL_BACKUP_URL:}
      incremental-backup-url: ${UAM_INCREMENTAL_BACKUP_URL:}
      last-backup-url: ${UAM_LAST_BACKUP_URL:}
      storage-config-uuid: ${UAM_STORAGE_CONFIG_UUID:}
      api-token: ${UAM_API_TOKEN:}
      universe-uuid: ${UAM_UNIVERSE_UUID:}
      backup-type: PGSQL_TABLE_TYPE
      backup-category-type: full_backup
      db-name: ${UAM_DB_NAME:hbl_gcp_uat_epr_db}
      expiry-ms: 172800000  # 48 hours
```

**Property Descriptions**:
- `full-backup-url`: YBA REST API endpoint for full backups
- `incremental-backup-url`: YBA REST API endpoint for incremental backups
- `last-backup-url`: YBA REST API endpoint to fetch last backup metadata
- `storage-config-uuid`: YBA storage configuration identifier
- `api-token`: YBA API authentication token
- `universe-uuid`: YBA universe identifier
- `backup-type`: Backup type (always `PGSQL_TABLE_TYPE` for PostgreSQL)
- `backup-category-type`: `full_backup` or `incremental_backup`
- `db-name`: Database name for logging/tracking
- `expiry-ms`: Backup retention period in milliseconds

### 4.3 WebClient Configuration

**Timeout Settings**:
```yaml
yba:
  connection-timeout: 30000  # 30 seconds
  read-timeout: 60000        # 60 seconds
  max-retry-attempts: 3
```

**HikariCP Connection Pool**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 4.4 SQL Query Configuration

**Queries** (from `application.yml`):
```yaml
data:
  db-schedule-backup-insert:
    query: >
      INSERT INTO epricing.batch_db_schedule_event_tracker(
        batch_id, backup_job_categorycode, backup_status, backup_type,
        business_date, start_time
      ) VALUES (
        :batch_id, :batchCategoryCode, :backup_status, :backup_type,
        :businessDate, now()
      )

  update-schedule-backup:
    query: >
      UPDATE batch_db_schedule_event_tracker
      SET backup_status = :status,
          end_time = now(),
          backup_response = :ydbResponse
      WHERE batch_id = :batch_id
        AND business_date = :businessDate
```

---

## 5. API CONTRACTS

### 5.1 REST API Endpoint

#### POST /backupProcess

**Description**: Initiates a backup process for a specified database

**Request**:
```http
POST http://localhost:8989/backupProcess
Content-Type: application/json

{
  "batchExecutionId": "BATCH_001",           // Optional: existing batch ID
  "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",  // Required: maps to YBA config key
  "batchTransactionDate": "20250325"         // Required: business date (yyyyMMdd)
}
```

**Request Fields**:
- `batchExecutionId` (optional): If provided, retrieves existing batch; otherwise creates new batch
- `batchCategoryCode` (required): Category code that maps to YBA database configuration (e.g., "db1", "db2", "uam-db")
- `batchTransactionDate` (required): Business date in format "yyyyMMdd"

**Response** (Success):
```json
{
  "id": {
    "batchExecutionId": "550e8400-e29b-41d4-a716-446655440000",
    "batchExecutionDate": "2025-03-25"
  },
  "batchCategory": "HWA_EPR_DB_BACKUP_FULL",
  "batchMode": "SCHEDULED",
  "status": "COMPLETED",
  "startTime": "2025-03-25T10:30:00",
  "endTime": "2025-03-25T10:35:00",
  "extensionFields": {
    "backup_response": "{\"taskUUID\": \"task-123\", \"backupUUID\": \"backup-456\"}"
  }
}
```

**Response** (Failure):
```json
{
  "status": "FAILED",
  "extensionFields": {
    "error_message": "Backup configuration not found for category: INVALID_CATEGORY"
  }
}
```

**HTTP Status Codes**:
- `200 OK`: Request processed (check `status` field for actual result)
- `500 Internal Server Error`: Unexpected server error

---

### 5.2 YBA REST API Integration

#### Full Backup Request

**Endpoint**: `POST {fullBackupUrl}`

**Headers**:
```http
X-AUTH-YW-API-TOKEN: {apiToken}
Content-Type: application/json
```

**Request Body**:
```json
{
  "universeUUID": "universe-uuid-123",
  "storageConfigUUID": "storage-uuid-123",
  "backupType": "PGSQL_TABLE_TYPE",
  "timeBeforeDelete": 86400000
}
```

**Response**:
```json
{
  "taskUUID": "task-uuid-789",
  "backupUUID": "backup-uuid-456",
  "status": "Running",
  "createTime": "2025-03-25T10:30:00Z"
}
```

#### Incremental Backup Request

**Step 1**: Fetch Last Backup
```http
GET {lastBackupUrl}
X-AUTH-YW-API-TOKEN: {apiToken}
```

**Response**:
```json
[
  {
    "backupUUID": "backup-uuid-previous",
    "createTime": "2025-03-24T10:30:00Z",
    "state": "Completed"
  }
]
```

**Step 2**: Initiate Incremental Backup
```http
POST {incrementalBackupUrl}
X-AUTH-YW-API-TOKEN: {apiToken}
Content-Type: application/json

{
  "universeUUID": "universe-uuid-123",
  "storageConfigUUID": "storage-uuid-123",
  "backupType": "PGSQL_TABLE_TYPE",
  "baseBackupUUID": "backup-uuid-previous",
  "timeBeforeDelete": 86400000
}
```

---

## 6. DATABASE SCHEMA

### 6.1 Backup Tracking Table

**Table**: `batch_db_schedule_event_tracker`

**Schema** (Inferred from SQL queries):
```sql
CREATE TABLE epricing.batch_db_schedule_event_tracker (
    batch_id VARCHAR(255) NOT NULL,
    backup_job_categorycode VARCHAR(255) NOT NULL,
    backup_status VARCHAR(50) NOT NULL,  -- IN_PROGRESS, SUCCESS, FAILED
    backup_type VARCHAR(50),              -- full_backup, incremental_backup
    business_date DATE NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    backup_response TEXT,                 -- JSON response from YBA API
    PRIMARY KEY (batch_id, business_date)
);
```

**Columns**:
- `batch_id`: Unique batch execution identifier (UUID)
- `backup_job_categorycode`: Category code (e.g., "HWA_EPR_DB_BACKUP_FULL")
- `backup_status`: Current status of backup job
- `backup_type`: Type of backup (full_backup/incremental_backup)
- `business_date`: Business date for the backup
- `start_time`: Backup initiation timestamp
- `end_time`: Backup completion timestamp
- `backup_response`: YBA API response (JSON stored as text)

**Status Values**:
- `IN_PROGRESS`: Backup initiated, waiting for completion
- `SUCCESS`: Backup completed successfully
- `FAILED`: Backup failed

---

### 6.2 Batch Execution Table

**Table**: `batch_execution_details`

**Schema** (from `V1__batch_execution_details.sql`):
```sql
CREATE TABLE epricing.batch_execution_details (
    batch_id VARCHAR(255) NOT NULL,
    batch_category VARCHAR(255) NOT NULL,
    business_date DATE NOT NULL,
    batch_mode VARCHAR(50),               -- SCHEDULED, MANUAL
    batch_status VARCHAR(50),             -- COMPLETED, FAILED
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    extension_fields JSONB,               -- Additional metadata
    PRIMARY KEY (batch_id, business_date)
) PARTITION BY RANGE (business_date);
```

**Partitioning**: Range partitioned by `business_date` for performance

**Extension Fields** (JSONB):
```json
{
  "backup_response": "{...}",
  "error_message": "Error details if failed"
}
```

---

### 6.3 Exception Details Table

**Table**: `exception_details`

**Schema** (from `V7__exception_details.sql`):
```sql
CREATE TABLE epricing.exception_details (
    exception_id VARCHAR(255) PRIMARY KEY,
    batch_id VARCHAR(255) NOT NULL,
    execution_type VARCHAR(50),
    error_message TEXT,
    extension_fields JSONB,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    business_date DATE NOT NULL
) PARTITION BY RANGE (business_date);
```

**Purpose**: Tracks exceptions during batch execution

---

## 7. ERROR HANDLING

### 7.1 Exception Hierarchy

```
Throwable
  └── RuntimeException
       ├── DbBackupException (custom)
       │    - Thrown for backup-specific errors
       │    - Caught by GlobalExceptionHandler
       │
       └── BatchExecutionException (custom)
            - Thrown when all retries exhausted
            - Thrown by DatabaseRetryHandler
```

### 7.2 Error Handling Patterns

#### Validation Errors
**Trigger**: Missing or invalid batch parameters, missing YBA configuration

**Handler**: `BackupValidationService`

**Exception**: `DbBackupException`

**Example**:
```java
if (StringUtils.isEmpty(batchId)) {
    throw new DbBackupException("Batch ID is required");
}
if (config.getApiToken() == null) {
    throw new DbBackupException("YBA API token not configured for: " + categoryCode);
}
```

#### YBA API Errors
**Trigger**: HTTP errors from YBA REST API (4xx, 5xx)

**Handler**: `YbaClient` with reactive error handling

**Pattern**:
```java
webClient.post()
    .uri(config.getFullBackupUrl())
    .bodyValue(requestBody)
    .retrieve()
    .onStatus(HttpStatusCode::is4xxClientError, response ->
        Mono.error(new DbBackupException("YBA API client error: " + response.statusCode())))
    .onStatus(HttpStatusCode::is5xxServerError, response ->
        Mono.error(new DbBackupException("YBA API server error: " + response.statusCode())))
    .bodyToMono(JsonNode.class);
```

#### Database Errors
**Trigger**: SQL exceptions during DAO operations

**Handler**: `DatabaseRetryHandler` (AOP aspect)

**Retryable SQL States**:
- `40001`: Serialization failure (transaction conflict)
- `40P01`: Deadlock detected
- `57P01`: Admin shutdown
- `08006`: Connection failure
- `XX000`: Internal error

**Retry Configuration**:
```java
@Bean
public RetryTemplate retryTemplate() {
    return RetryTemplate.builder()
        .customPolicy(new CustomRetryPolicy())  // Max 3 attempts
        .exponentialBackoff(1000, 2.0, 10000)   // 1s, 2s, 4s, 8s, 10s (max)
        .build();
}
```

**Non-Retryable Errors**: All other SQL states fail immediately

#### Reactive Chain Errors
**Trigger**: Any exception in reactive chain

**Handler**: `onErrorResume()` operator

**Pattern**:
```java
ybaClient.backupInitiate(categoryCode, batchParams)
    .flatMap(ydbRes -> handleBackupSuccess(...))
    .onErrorResume(e -> {
        log.error("Backup failed for batchId: {}", batchId, e);
        return handleBackupFailure(batchId, businessDate, e);
    })
    .then();
```

### 7.3 Failure Scenarios

#### Scenario 1: Invalid Configuration
**Cause**: Missing YBA configuration for category code

**Flow**:
1. `YbaConfigService.resolve()` returns null
2. `BackupValidationService` throws `DbBackupException`
3. `GlobalExceptionHandler` catches exception
4. Returns HTTP 200 with `status: FAILED` and error message

**Database State**:
- Batch status: `FAILED`
- Extension fields: `{"error_message": "Configuration not found"}`

#### Scenario 2: YBA API Timeout
**Cause**: YBA API does not respond within 60 seconds

**Flow**:
1. WebClient read timeout triggers
2. `onErrorResume()` catches `TimeoutException`
3. `handleBackupFailure()` updates database
4. Backup status: `FAILED`, batch status: `FAILED`

**Database State**:
- `backup_status`: `FAILED`
- `backup_response`: "Read timeout error"

#### Scenario 3: Database Connection Failure
**Cause**: YugabyteDB cluster unreachable (SQL state 08006)

**Flow**:
1. `BackupDaoService` method throws `SQLException`
2. `DatabaseRetryHandler` intercepts via AOP
3. Retries 3 times with exponential backoff
4. If all retries fail, throws `BatchExecutionException`
5. Propagates to caller

**Mitigation**: Retry mechanism handles transient network issues

#### Scenario 4: Incremental Backup Without Base
**Cause**: No previous backup exists for incremental backup

**Flow**:
1. `fetchLastBackup()` returns empty array
2. `incrementalBackup()` attempts to extract `baseBackupUUID`
3. Throws `NullPointerException` or `IndexOutOfBoundsException`
4. `onErrorResume()` catches and updates status to `FAILED`

**Recommendation**: Add validation to check if last backup exists before initiating incremental

---

## 8. DEPENDENCIES

### 8.1 Core Frameworks

**Spring Boot**: `3.3.7`, `3.4.2`, `3.4.4`, `3.5.3` (multiple modules)
- **spring-boot-starter-web**: REST API support
- **spring-boot-starter-webflux**: Reactive web client
- **spring-boot-starter-jdbc**: JDBC database access
- **spring-boot-starter-aop**: Aspect-Oriented Programming

**Java**: `21` (with toolchain configuration)

**Gradle**: `8.11.1`, `8.12.1`, `8.14` (build tool)

### 8.2 Reactive Libraries

**Project Reactor**:
- `reactor-core`: Core reactive types (Mono, Flux)
- `reactor-netty`: HTTP client for WebClient

**Spring WebFlux**:
- `spring-webflux`: Reactive web framework
- `WebClient`: Non-blocking HTTP client

### 8.3 Database Libraries

**YugabyteDB Driver**: `com.yugabyte.Driver`
- JDBC driver for YugabyteDB
- Supports load balancing and topology awareness

**HikariCP**: Connection pooling (bundled with Spring Boot)

**Flyway**: Database migration tool
- Version-controlled schema migrations
- Located in `src/main/resources/db/migration/`

**Spring JDBC**:
- `NamedParameterJdbcTemplate`: Named parameter support for SQL queries

### 8.4 Utility Libraries

**Lombok**: `org.projectlombok:lombok`
- `@Slf4j`: Logging
- `@RequiredArgsConstructor`: Constructor injection
- `@Builder`: Builder pattern for DTOs

**Jackson**: JSON processing
- `ObjectMapper`: JSON serialization/deserialization
- `JsonNode`: JSON tree model

**JsonPath**: `com.jayway.jsonpath:json-path`
- JSON query language for extracting values

**Spring Retry**: `org.springframework.retry:spring-retry`
- `RetryTemplate`: Retry logic framework
- `@Retryable`: Annotation-based retry

### 8.5 Testing Libraries

**JUnit 5**: `org.junit.jupiter:junit-jupiter`
- Unit testing framework

**Mockito**: `org.mockito:mockito-core`
- Mocking framework for tests

**Reactor Test**: `io.projectreactor:reactor-test`
- `StepVerifier`: Testing reactive streams

### 8.6 Dependency Versions

**Key Dependencies** (from `build.gradle`):
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'com.yugabyte:jdbc-yugabytedb:42.3.5-yb-5'
    implementation 'org.flywaydb:flyway-core'
    implementation 'com.jayway.jsonpath:json-path:2.8.0'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'
}
```

**Batch Core Library**: `com.scb:epricing-batch-core-lib:4.0.1`
- Internal library providing `GenericBatchService`, `BatchExecutionDao`, retry mechanisms

---

## 9. DEPLOYMENT CONTEXT

### 9.1 Kubernetes Deployment

**Deployment Type**: Kubernetes with Helm charts

**Helm Chart Location**: `charts/charts/`

**Key Resources**:
- **Deployment**: `backup-orchestrator-service-deployment.yaml`
- **Service**: `backup-orchestrator-service-service.yaml`
- **ConfigMap**: `backup-orchestrator-service-configmap.yaml`
- **Secret**: Managed via Dapr (GCP Secret Manager integration)

### 9.2 ConfigMap Structure

**File**: `charts/charts/templates/backup-orchestrator-service-configmap.yaml`

**Environment Variables**:
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: backup-orchestrator-service-config
data:
  # Application Settings
  APPLICATION_NAME: "backup-orchestrator-service"
  APP_ENV: "{{ .Values.environment }}"  # dev/uat/prod
  SERVER_PORT: "8989"

  # Database Connection
  DATASOURCE_URL: "{{ .Values.datasource.url }}"
  DATASOURCE_USERNAME: "{{ .Values.datasource.username }}"
  DATASOURCE_DRIVER_CLASS_NAME: "com.yugabyte.Driver"
  DATASOURCE_SSL_ENABLE: "{{ .Values.datasource.ssl.enable }}"
  DATASOURCE_SSL_MODE: "{{ .Values.datasource.ssl.mode }}"

  # YBA Configuration - Database 1
  YBA_FULL_BACKUP_URL: "{{ .Values.yba.db1.fullBackupUrl }}"
  YBA_STORAGE_CONFIG_UUID: "{{ .Values.yba.db1.storageConfigUuid }}"
  YBA_UNIVERSE_UUID: "{{ .Values.yba.db1.universeUuid }}"
  YBA_DB_NAME: "{{ .Values.yba.db1.dbName }}"

  # YBA Configuration - Database 2
  YBA_FULL_BACKUP_URL_2: "{{ .Values.yba.db2.fullBackupUrl }}"
  YBA_INCREMENTAL_BACKUP_URL_2: "{{ .Values.yba.db2.incrementalBackupUrl }}"
  YBA_LAST_BACKUP_URL_2: "{{ .Values.yba.db2.lastBackupUrl }}"
  YBA_STORAGE_CONFIG_UUID_2: "{{ .Values.yba.db2.storageConfigUuid }}"
  YBA_UNIVERSE_UUID_2: "{{ .Values.yba.db2.universeUuid }}"
  YBA_DB_NAME_2: "{{ .Values.yba.db2.dbName }}"

  # YBA Configuration - UAM Database
  UAM_FULL_BACKUP_URL: "{{ .Values.yba.uamDb.fullBackupUrl }}"
  UAM_INCREMENTAL_BACKUP_URL: "{{ .Values.yba.uamDb.incrementalBackupUrl }}"
  UAM_LAST_BACKUP_URL: "{{ .Values.yba.uamDb.lastBackupUrl }}"
  UAM_STORAGE_CONFIG_UUID: "{{ .Values.yba.uamDb.storageConfigUuid }}"
  UAM_UNIVERSE_UUID: "{{ .Values.yba.uamDb.universeUuid }}"
  UAM_DB_NAME: "{{ .Values.yba.uamDb.dbName }}"
```

### 9.3 Secret Management

**Secret Provider**: Dapr with GCP Secret Manager

**Secrets**:
- `DATASOURCE_PASSWORD`: Database password
- `YBA_API_TOKEN`: YBA API token for db1
- `YBA_API_TOKEN_2`: YBA API token for db2
- `UAM_API_TOKEN`: YBA API token for UAM database

**Dapr Configuration**:
```yaml
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: gcpsecretmanager
spec:
  type: secretstores.gcp.secretmanager
  metadata:
    - name: type
      value: "service_account"
    - name: project_id
      value: "{{ .Values.gcp.projectId }}"
```

**Secret Reference in Deployment**:
```yaml
env:
  - name: DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: backup-orchestrator-secrets
        key: datasource-password
  - name: YBA_API_TOKEN
    valueFrom:
      secretKeyRef:
        name: backup-orchestrator-secrets
        key: yba-api-token
```

### 9.4 Helm Values Structure

**File**: `charts/charts/values.yaml`

```yaml
environment: uat  # dev/uat/prod

replicaCount: 2

image:
  repository: gcr.io/project-id/backup-orchestrator-service
  tag: "1.0.0"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 8989

resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"

datasource:
  url: "jdbc:yugabytedb://yb-cluster:5432/postgresdb?ApplicationName=backupOrchestrator&currentSchema=epricing&load-balance=true"
  username: "admin"
  ssl:
    enable: false
    mode: "disable"

yba:
  db1:
    fullBackupUrl: "https://yba.example.com/api/v1/customers/cust123/backups"
    storageConfigUuid: "storage-uuid-123"
    universeUuid: "universe-uuid-123"
    dbName: "hbl_gcp_uat_epr_db"

  db2:
    fullBackupUrl: "https://yba.example.com/api/v1/customers/cust456/backups"
    incrementalBackupUrl: "https://yba.example.com/api/v1/customers/cust456/backups/incremental"
    lastBackupUrl: "https://yba.example.com/api/v1/customers/cust456/backups?limit=1&direction=DESC"
    storageConfigUuid: "storage-uuid-456"
    universeUuid: "universe-uuid-456"
    dbName: "hbl_gcp_uat_epr_db_2"

  uamDb:
    fullBackupUrl: "https://yba.example.com/api/v1/customers/cust789/backups"
    incrementalBackupUrl: "https://yba.example.com/api/v1/customers/cust789/backups/incremental"
    lastBackupUrl: "https://yba.example.com/api/v1/customers/cust789/backups?limit=1&direction=DESC"
    storageConfigUuid: "storage-uuid-789"
    universeUuid: "universe-uuid-789"
    dbName: "hbl_gcp_uat_epr_db"

gcp:
  projectId: "hbl-poc-enterprisefac-pm-prj"
```

### 9.5 Environment-Specific Configuration

**Profile Activation** (`application.yml`):
```yaml
spring:
  profiles:
    active: ${APP_ENV:dev}  # dev/uat/prod

---
spring:
  config:
    activate:
      on-profile: dev
# Dev-specific overrides

---
spring:
  config:
    activate:
      on-profile: uat
# UAT-specific overrides

---
spring:
  config:
    activate:
      on-profile: prod
# Production-specific overrides
```

### 9.6 Deployment Commands

**Install/Upgrade Helm Chart**:
```bash
helm upgrade --install backup-orchestrator-service ./charts/charts \
  --namespace epricing \
  --values ./charts/charts/values-uat.yaml \
  --set image.tag=1.0.0
```

**Verify Deployment**:
```bash
kubectl get pods -n epricing -l app=backup-orchestrator-service
kubectl logs -n epricing -l app=backup-orchestrator-service --tail=100
```

**Check ConfigMap**:
```bash
kubectl get configmap backup-orchestrator-service-config -n epricing -o yaml
```

**Check Secrets**:
```bash
kubectl get secrets backup-orchestrator-secrets -n epricing
```

### 9.7 Health Checks

**Liveness Probe**:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8989
  initialDelaySeconds: 30
  periodSeconds: 10
```

**Readiness Probe**:
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8989
  initialDelaySeconds: 20
  periodSeconds: 5
```

**Actuator Endpoints** (Spring Boot):
- `/actuator/health`: Overall health status
- `/actuator/info`: Application information
- `/actuator/metrics`: Application metrics

---

## 10. OPERATIONAL CONSIDERATIONS

### 10.1 Monitoring and Logging

**Logging Framework**: SLF4J with Logback

**Log Levels**:
- `INFO`: Backup initiation, completion, configuration loading
- `ERROR`: Backup failures, API errors, database errors
- `DEBUG`: Detailed request/response, reactive chain execution

**Key Log Messages**:
```java
log.info("Backup process initiated for batchId: {}, categoryCode: {}", batchId, categoryCode);
log.info("Backup completed successfully for batchId: {}", batchId);
log.error("Backup failed for batchId: {}", batchId, exception);
log.debug("YBA API request: {}", requestBody);
log.debug("YBA API response: {}", responseBody);
```

**Structured Logging** (Recommended):
- Add correlation IDs for request tracing
- Include business date, batch ID in all log messages
- Use JSON log format for centralized logging (ELK, Splunk)

### 10.2 Metrics and Observability

**Recommended Metrics** (via Micrometer):
- `backup.initiated.count`: Counter for backup initiations
- `backup.success.count`: Counter for successful backups
- `backup.failure.count`: Counter for failed backups
- `backup.duration`: Timer for backup execution time
- `yba.api.request.duration`: Timer for YBA API calls
- `database.retry.count`: Counter for database retry attempts

**Example Instrumentation**:
```java
@Timed(value = "backup.duration", description = "Time taken to complete backup")
public Mono<Void> processBackup(String batchId, String businessDate, String categoryCode) {
    // ...
}
```

### 10.3 Troubleshooting Guide

#### Issue: Backup Status Stuck in IN_PROGRESS

**Symptoms**:
- Database shows `backup_status = 'IN_PROGRESS'`
- No error logs
- YBA API call succeeded

**Root Cause**: Fire-and-forget `.subscribe()` pattern doesn't wait for completion

**Resolution**:
1. Check YBA console for actual backup status
2. Manually update database status if needed:
   ```sql
   UPDATE batch_db_schedule_event_tracker
   SET backup_status = 'SUCCESS', end_time = now()
   WHERE batch_id = 'BATCH_001';
   ```

**Long-term Fix**: Implement polling mechanism to check YBA backup status

#### Issue: Database Connection Error (08001)

**Symptoms**:
- Error: "Connection attempt failed in cluster for db getting 08001"
- Service cannot start or process backups

**Root Causes**:
1. Missing environment variables (`DATASOURCE_URL`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD`)
2. YugabyteDB cluster unreachable
3. SSL configuration issues

**Resolution**:
1. Verify environment variables in ConfigMap
2. Test connectivity: `telnet <db-host> 5432`
3. Check SSL settings: Set `DATASOURCE_SSL_ENABLE=false` for testing
4. Review HikariCP connection pool logs

#### Issue: YBA API Returns 401 Unauthorized

**Symptoms**:
- Error: "YBA API client error: 401"
- Backup status: FAILED

**Root Cause**: Invalid or expired YBA API token

**Resolution**:
1. Verify `YBA_API_TOKEN` in secrets
2. Regenerate token in YBA console
3. Update Kubernetes secret:
   ```bash
   kubectl create secret generic backup-orchestrator-secrets \
     --from-literal=yba-api-token=<new-token> \
     --dry-run=client -o yaml | kubectl apply -f -
   ```
4. Restart pods to pick up new secret

#### Issue: Incremental Backup Fails with NullPointerException

**Symptoms**:
- Error: "NullPointerException" in `performIncrementalBackup()`
- No previous backup exists

**Root Cause**: Attempting incremental backup without base backup

**Resolution**:
1. Run full backup first
2. Add validation in code to check if last backup exists:
   ```java
   if (lastBackupResponse.isEmpty()) {
       throw new DbBackupException("No base backup found for incremental backup");
   }
   ```

### 10.4 Performance Tuning

**WebClient Connection Pool**:
```java
ConnectionProvider provider = ConnectionProvider.builder("yba-client")
    .maxConnections(50)
    .pendingAcquireMaxCount(100)
    .build();
```

**HikariCP Tuning**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Increase for high concurrency
      minimum-idle: 10
      connection-timeout: 30000
```

**Reactive Backpressure**:
- Use `Flux.buffer()` for batch processing multiple backups
- Implement rate limiting for YBA API calls

### 10.5 Security Best Practices

1. **Never hardcode credentials**: Use environment variables and secret management
2. **Rotate API tokens regularly**: Update YBA API tokens every 90 days
3. **Enable SSL for database**: Set `DATASOURCE_SSL_ENABLE=true` in production
4. **Use RBAC**: Restrict Kubernetes service account permissions
5. **Audit logging**: Log all backup operations with user/service identity
6. **Network policies**: Restrict egress to YBA API endpoints only

---

## 11. FUTURE ENHANCEMENTS

### 11.1 Recommended Improvements

1. **Backup Status Polling**:
   - Implement periodic polling to check YBA backup job status
   - Update database when backup completes (instead of fire-and-forget)

2. **Webhook Integration**:
   - Configure YBA to send webhooks on backup completion
   - Implement webhook endpoint to receive status updates

3. **Backup Validation**:
   - Verify backup integrity after completion
   - Implement restore testing for critical backups

4. **Metrics Dashboard**:
   - Create Grafana dashboard for backup metrics
   - Alert on backup failures, long-running backups

5. **Multi-Tenancy Support**:
   - Support multiple YBA instances
   - Tenant-specific backup policies

6. **Backup Scheduling**:
   - Integrate with Kubernetes CronJob for automated scheduling
   - Support cron expressions for flexible scheduling

7. **Error Handling Improvements**:
   - Return HTTP 500 for server errors (not 200)
   - Implement circuit breaker for YBA API calls
   - Add dead letter queue for failed backups

8. **Testing**:
   - Enable and fix commented-out tests
   - Add integration tests with Testcontainers
   - Implement contract testing for YBA API

---

## 12. QUICK REFERENCE

### 12.1 Common Commands

**Trigger Backup**:
```bash
curl -X POST http://localhost:8989/backupProcess \
  -H "Content-Type: application/json" \
  -d '{
    "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
    "batchTransactionDate": "20250325"
  }'
```

**Check Backup Status**:
```sql
SELECT batch_id, backup_status, backup_type, start_time, end_time
FROM batch_db_schedule_event_tracker
WHERE business_date = '2025-03-25'
ORDER BY start_time DESC;
```

**View Logs**:
```bash
kubectl logs -n epricing -l app=backup-orchestrator-service --tail=100 -f
```

### 12.2 Key Files Reference

| Component | File Path |
|-----------|-----------|
| REST Controller | `src/main/java/com/hdfc/backup/BackupController.java` |
| Backup Service | `src/main/java/com/hdfc/backup/service/BackupService.java` |
| YBA Client | `src/main/java/com/hdfc/backup/client/YbaClient.java` |
| DAO Service | `src/main/java/com/hdfc/backup/dao/BackupDaoService.java` |
| Configuration | `src/main/resources/application.yml` |
| Helm Chart | `charts/charts/values.yaml` |
| Database Migrations | `src/main/resources/db/migration/` |

### 12.3 Contact and Support

**Development Team**: SCB ePricing Team

**Documentation**: This master prompt serves as the primary technical reference

**Version**: 1.0.0

**Last Updated**: 2025-03-25

---

## APPENDIX A: Glossary

- **YBA**: YugabyteDB Anywhere - Enterprise management platform for YugabyteDB
- **Batch**: A scheduled or manual execution unit for backup operations
- **Category Code**: Identifier mapping to specific database backup configuration
- **Business Date**: The logical date for which the backup is performed
- **Full Backup**: Complete database backup
- **Incremental Backup**: Backup of changes since last full/incremental backup
- **Fire-and-Forget**: Reactive pattern where `.subscribe()` triggers execution without waiting
- **Mono**: Reactive type representing 0 or 1 element
- **Flux**: Reactive type representing 0 to N elements
- **AOP**: Aspect-Oriented Programming - Cross-cutting concerns like retry logic

---

## APPENDIX B: Reactive Programming Patterns

### Pattern 1: Mono.fromCallable()
**Use Case**: Wrap synchronous blocking operations in reactive chain

```java
Mono.fromCallable(() -> configService.resolve(categoryCode))
    .flatMap(config -> performAsyncOperation(config));
```

### Pattern 2: flatMap() for Chaining
**Use Case**: Chain dependent asynchronous operations

```java
fetchLastBackup(config)
    .flatMap(lastBackup -> incrementalBackup(config, lastBackup));
```

### Pattern 3: onErrorResume() for Error Handling
**Use Case**: Handle errors without breaking reactive chain

```java
ybaClient.backupInitiate(...)
    .onErrorResume(e -> {
        log.error("Backup failed", e);
        return handleFailure(e);
    });
```

### Pattern 4: then() for Completion Signal
**Use Case**: Convert to Mono<Void> when only completion matters

```java
processBackup(...)
    .flatMap(result -> updateDatabase(result))
    .then();  // Returns Mono<Void>
```

---

**END OF MASTER PROMPT**


