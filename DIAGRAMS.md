# Backup Orchestrator Service - Architecture Diagrams

This document contains visual diagrams for the Backup Orchestrator Service architecture and workflows.

## 1. High-Level Architecture

```mermaid
graph TB
    Client[Scheduler/Client] -->|POST /backupProcess| Controller[BackupController]
    Controller -->|execute| BatchService[GenericBatchService]
    BatchService -->|process| BackupService[BackupService]
    BackupService -->|validate| ValidationService[BackupValidationService]
    BackupService -->|resolve config| ConfigService[YbaConfigService]
    BackupService -->|initiate backup| YbaClient[YbaClient]
    YbaClient -->|HTTP Request| YBA[YugabyteDB Anywhere API]
    BackupService -->|insert/update| DaoService[BackupDaoService]
    DaoService -->|JDBC| Database[(YugabyteDB)]
    
    style Client fill:#e1f5ff
    style Controller fill:#fff4e6
    style BackupService fill:#e8f5e9
    style YbaClient fill:#f3e5f5
    style YBA fill:#ffebee
    style Database fill:#e0f2f1
```

## 2. Component Class Diagram

```mermaid
classDiagram
    class BackupController {
        -BackupService backupService
        +backupProcess(String json) Mono~BatchStartResponse~
    }
    
    class BackupService {
        -YbaClient ybaClient
        -BackupDaoService daoService
        -BackupValidationService validationService
        +process(Map batchParams) void
        +processBackup(String batchId, String businessDate, String categoryCode) Mono~Void~
        -handleBackupSuccess() Mono~Void~
        -handleBackupFailure() Mono~Void~
    }
    
    class YbaClient {
        -WebClient webClient
        -YbaConfigService configService
        +backupInitiate(String categoryCode, Map params) Mono~String~
        +fullBackup(YbaDynamicConfig config) Mono~String~
        +incrementalBackup(YbaDynamicConfig config) Mono~String~
        +fetchLastBackup(YbaDynamicConfig config) Mono~String~
    }
    
    class YbaConfigService {
        -YbaProperties properties
        +resolve(String categoryCode) YbaDynamicConfig
    }
    
    class BackupDaoService {
        -NamedParameterJdbcTemplate jdbcTemplate
        +insertBackupDetails(Map details, String status, String type) void
        +updateBackupStatus(String batchId, String status, Date date, String response) void
    }
    
    class BackupValidationService {
        +validateBatchParams(Map params) void
        +validateBackupConfig(YbaDynamicConfig config, String categoryCode) void
    }
    
    BackupController --> BackupService
    BackupService --> YbaClient
    BackupService --> BackupDaoService
    BackupService --> BackupValidationService
    YbaClient --> YbaConfigService
```

## 3. Full Backup Sequence Diagram

```mermaid
sequenceDiagram
    participant Client
    participant Controller as BackupController
    participant Service as BackupService
    participant Validation as ValidationService
    participant Config as YbaConfigService
    participant YbaClient
    participant DAO as BackupDaoService
    participant YBA as YBA API
    participant DB as Database
    
    Client->>Controller: POST /backupProcess<br/>{categoryCode, transactionDate}
    Controller->>Service: execute(json)
    Service->>Validation: validateBatchParams()
    Validation-->>Service: validated
    Service->>Config: resolve(categoryCode)
    Config-->>Service: YbaDynamicConfig
    Service->>DAO: insertBackupDetails(IN_PROGRESS)
    DAO->>DB: INSERT backup record
    Service->>YbaClient: backupInitiate()
    YbaClient->>YBA: POST /backups<br/>{universeUUID, storageConfigUUID}
    YBA-->>YbaClient: {taskUUID, resourceUUID}
    YbaClient-->>Service: backup response
    Service->>DAO: updateBackupStatus(SUCCESS)
    DAO->>DB: UPDATE backup status
    Service-->>Controller: BatchStartResponse
    Controller-->>Client: Response
```

## 4. Incremental Backup Sequence Diagram

```mermaid
sequenceDiagram
    participant Service as BackupService
    participant YbaClient
    participant YBA as YBA API
    participant DAO as BackupDaoService
    participant DB as Database
    
    Service->>YbaClient: performIncrementalBackup()
    YbaClient->>YBA: GET /backups?limit=1&direction=DESC
    YBA-->>YbaClient: {baseBackupUUID, ...}
    YbaClient->>YbaClient: extract baseBackupUUID
    YbaClient->>YBA: POST /backups/incremental<br/>{baseBackupUUID, universeUUID}
    YBA-->>YbaClient: {taskUUID, resourceUUID}
    YbaClient-->>Service: backup response
    Service->>DAO: updateBackupStatus(SUCCESS)
    DAO->>DB: UPDATE status
```

## 5. Error Handling Flow

```mermaid
graph TD
    RuntimeException[RuntimeException] --> DbBackupException[DbBackupException]
    DbBackupException --> ValidationError[Validation Errors]
    DbBackupException --> ConfigError[Configuration Errors]
    DbBackupException --> ApiError[YBA API Errors]
    DbBackupException --> DatabaseError[Database Errors]
    
    style RuntimeException fill:#ffebee
    style DbBackupException fill:#fff3e0
    style ValidationError fill:#e8f5e9
    style ConfigError fill:#e1f5ff
    style ApiError fill:#f3e5f5
    style DatabaseError fill:#fce4ec
```

## 6. Backup State Machine

```mermaid
stateDiagram-v2
    [*] --> Requested: Backup Request Received
    Requested --> Validating: Validate Parameters
    Validating --> ConfigResolving: Validation Success
    Validating --> Failed: Validation Error
    ConfigResolving --> InProgress: Config Resolved
    ConfigResolving --> Failed: Config Error
    InProgress --> ApiCall: Insert DB Record
    ApiCall --> Success: YBA API Success
    ApiCall --> Failed: YBA API Error
    Success --> [*]: Update Status
    Failed --> [*]: Update Status
```

## 7. Data Flow Diagram

```mermaid
graph LR
    A[Client Request] --> B{Validation}
    B -->|Valid| C[Config Resolution]
    B -->|Invalid| Z[Error Response]
    C --> D[DB Insert: IN_PROGRESS]
    D --> E{Backup Type}
    E -->|Full| F[Full Backup API Call]
    E -->|Incremental| G[Fetch Last Backup]
    G --> H[Incremental Backup API Call]
    F --> I{API Response}
    H --> I
    I -->|Success| J[DB Update: SUCCESS]
    I -->|Failure| K[DB Update: FAILED]
    J --> L[Success Response]
    K --> M[Error Response]
    
    style A fill:#e1f5ff
    style B fill:#fff4e6
    style E fill:#fff4e6
    style I fill:#fff4e6
    style J fill:#e8f5e9
    style K fill:#ffebee
```


