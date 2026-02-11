# Backup Orchestrator Service - Visual Diagrams

This document contains all the visual diagrams for the Backup Orchestrator Service architecture, database design, and workflows.

---

## Table of Contents
1. [Full Backup Sequence Diagram](#1-full-backup-sequence-diagram)
2. [Incremental Backup Sequence Diagram](#2-incremental-backup-sequence-diagram)
3. [Three-Table Architecture Flow](#3-three-table-architecture-flow)
4. [Backup Lifecycle State Diagram](#4-backup-lifecycle-state-diagram)
5. [Enhanced ER Diagram](#5-enhanced-er-diagram)
6. [Detailed Database Schema with Constraints](#6-detailed-database-schema-with-constraints)
7. [Database Operations Flow](#7-database-operations-flow)

---

## 1. Full Backup Sequence Diagram

Shows the complete flow of a full backup operation with 6 color-coded phases.

```mermaid
sequenceDiagram
    autonumber
    participant Client as 📱 Client<br/>(Batch Scheduler)
    participant Controller as 🎮 BackupController<br/>(REST API)
    participant Service as ⚙️ BackupService<br/>(Orchestrator)
    participant YbaClient as 🔌 YbaClient<br/>(Integration)
    participant FullDB as 💾 full_backup_tracker<br/>(Database)
    participant YBA as ☁️ YugabyteDB Anywhere<br/>(YBA API)
    participant Poller as 🔄 BackupPollerService<br/>(Async Poller)
    participant BatchDB as 📊 Batch Execution Table<br/>(Batch Framework)

    rect rgb(230, 240, 255)
        Note over Client,BatchDB: Phase 1: Backup Initiation
        Client->>+Controller: POST /backupProcess<br/>{batchId, businessDate, categoryCode}
        Controller->>+Service: execute(json)
        Service->>Service: Validate parameters
        Service->>+YbaClient: backupInitiate(batchId, businessDate, categoryCode)
    end

    rect rgb(255, 240, 230)
        Note over YbaClient,YBA: Phase 2: Full Backup Execution
        YbaClient->>YbaClient: Determine backup type<br/>(FULL vs INCREMENTAL)
        YbaClient->>+FullDB: INSERT INTO full_backup_tracker<br/>(status=IN_PROGRESS, task_uuid)
        FullDB-->>-YbaClient: Record inserted
        
        YbaClient->>+YBA: POST /api/v1/customers/{cUUID}/universes/{uUUID}/backups<br/>{storageConfigUUID, backupType, keyspaceTableList}
        YBA-->>-YbaClient: {taskUUID, resourceUUID, status: "Running"}
        
        YbaClient->>+FullDB: UPDATE full_backup_tracker<br/>SET full_backup_response = JSON
        FullDB-->>-YbaClient: Updated
        YbaClient-->>-Service: Backup initiated successfully
    end

    rect rgb(240, 255, 240)
        Note over Service,BatchDB: Phase 3: Response & Batch Update
        Service->>+BatchDB: UPDATE batch_execution_table<br/>SET status = 'COMPLETED'
        BatchDB-->>-Service: Updated
        Service-->>-Controller: BatchStartResponse
        Controller-->>-Client: {status: "INITIATED", taskUUID}
    end

    rect rgb(255, 245, 230)
        Note over Poller,YBA: Phase 4: Async Polling (Parallel Thread)
        loop Every 30 seconds (configurable)
            Poller->>+YBA: GET /api/v1/customers/{cUUID}/tasks/{taskUUID}
            YBA-->>-Poller: {status: "Running"}
        end
        
        Poller->>+YBA: GET /api/v1/customers/{cUUID}/tasks/{taskUUID}
        YBA-->>-Poller: {status: "Success", completionTime}
    end

    rect rgb(240, 230, 255)
        Note over Poller,FullDB: Phase 5: Base UUID Retrieval & Update
        Poller->>+YBA: POST /api/v1/customers/{cUUID}/universes/{uUUID}/backups/page<br/>{direction: "DESC", limit: 1, sortBy: "createTime",<br/>filter: {universeUUIDList: [...]}}
        YBA-->>-Poller: {entities: [{backupUUID, commonBackupInfo: {baseBackupUUID}}]}

        Poller->>Poller: Extract baseBackupUUID from entities[0]

        Poller->>+FullDB: UPDATE full_backup_tracker<br/>SET base_backup_uuid = 'uuid',<br/>backup_status = 'SUCCESS',<br/>end_time = NOW()
        FullDB-->>-Poller: Updated with base UUID
    end

    rect rgb(255, 230, 240)
        Note over Poller,FullDB: Phase 6: Completion
        Poller->>Poller: Log success & stop polling
    end
```

**Key Phases:**
- 🔵 **Phase 1**: Backup Initiation - Request received and validated
- 🟠 **Phase 2**: Full Backup Execution - YBA API called, tracker updated
- 🟢 **Phase 3**: Response & Batch Update - Batch framework updated
- 🟡 **Phase 4**: Async Polling - Parallel thread monitors job status
- 🟣 **Phase 5**: Base UUID Retrieval - Critical for incremental backups
- 🔴 **Phase 6**: Completion - Polling stopped

---

## 2. Incremental Backup Sequence Diagram

Shows the incremental backup flow with base UUID dependency and error handling.

```mermaid
sequenceDiagram
    autonumber
    participant Client as 📱 Client<br/>(Batch Scheduler)
    participant Controller as 🎮 BackupController<br/>(REST API)
    participant Service as ⚙️ BackupService<br/>(Orchestrator)
    participant YbaClient as 🔌 YbaClient<br/>(Integration)
    participant FullDB as 💾 full_backup_tracker<br/>(Database)
    participant IncrDB as 📦 incremental_backup_tracker<br/>(Database)
    participant YBA as ☁️ YugabyteDB Anywhere<br/>(YBA API)
    participant BatchDB as 📊 Batch Execution Table<br/>(Batch Framework)

    rect rgb(230, 240, 255)
        Note over Client,BatchDB: Phase 1: Incremental Backup Request
        Client->>+Controller: POST /backupProcess<br/>{batchId, businessDate, categoryCode: "INCRE"}
        Controller->>+Service: execute(json)
        Service->>Service: Validate parameters
        Service->>+YbaClient: backupInitiate(batchId, businessDate, categoryCode)
    end

    rect rgb(255, 240, 230)
        Note over YbaClient,FullDB: Phase 2: Fetch Base UUID
        YbaClient->>YbaClient: Detect INCREMENTAL backup type
        YbaClient->>+FullDB: SELECT base_backup_uuid<br/>FROM full_backup_tracker<br/>WHERE category_code = 'FULL'<br/>AND backup_month = '2026-02'<br/>AND backup_status = 'SUCCESS'
        
        alt Base UUID Found
            FullDB-->>-YbaClient: base_backup_uuid = 'base-uuid-full-2026-02'
        else Base UUID Not Found
            FullDB-->>YbaClient: No records found
            YbaClient-->>Service: ❌ IllegalStateException:<br/>"Base backup UUID not found"
            Service-->>Controller: Error response
            Controller-->>Client: {status: "FAILED", error: "..."}
            Note over Client,BatchDB: ⚠️ Process terminates - Full backup required first
        end
    end

    rect rgb(240, 255, 240)
        Note over YbaClient,IncrDB: Phase 3: Incremental Backup Execution
        YbaClient->>+IncrDB: INSERT INTO incremental_backup_tracker<br/>(batch_id, base_backup_uuid,<br/>status=IN_PROGRESS, task_uuid)
        IncrDB-->>-YbaClient: Record inserted
        
        YbaClient->>+YBA: POST /api/v1/customers/{cUUID}/universes/{uUUID}/backups<br/>{storageConfigUUID, backupType,<br/>baseBackupUUID: 'base-uuid-full-2026-02',<br/>keyspaceTableList}
        YBA-->>-YbaClient: {taskUUID, resourceUUID, status: "Running"}
        
        YbaClient->>+IncrDB: UPDATE incremental_backup_tracker<br/>SET incremental_backup_response = JSON
        IncrDB-->>-YbaClient: Updated
        YbaClient-->>-Service: Incremental backup initiated
    end

    rect rgb(255, 245, 230)
        Note over Service,BatchDB: Phase 4: Response & Batch Update
        Service->>+BatchDB: UPDATE batch_execution_table<br/>SET status = 'COMPLETED'
        BatchDB-->>-Service: Updated
        Service-->>-Controller: BatchStartResponse
        Controller-->>-Client: {status: "INITIATED",<br/>baseBackupUUID, taskUUID}
    end

    rect rgb(240, 230, 255)
        Note over Client,IncrDB: Phase 5: Job Completion (Optional Polling)
        Note right of IncrDB: ℹ️ Incremental backups don't update<br/>base_backup_uuid (already exists in full_backup_tracker).<br/>Polling can verify completion but is optional.
    end
```

**Key Features:**
- ✅ Fetches base UUID from full_backup_tracker
- ❌ Error handling when base UUID not found
- ✅ Stores base_backup_uuid reference in incremental_backup_tracker
- ✅ No polling needed for base UUID (already exists)

---

## 3. Three-Table Architecture Flow

Overall system architecture showing all layers and database interactions.

```mermaid
graph TB
    subgraph Client["📱 Client Layer"]
        BS[Batch Scheduler]
    end

    subgraph API["🎮 REST API Layer (Port 8989)"]
        BC[BackupController<br/>/backupProcess]
    end

    subgraph Service["⚙️ Service Layer"]
        BKS[BackupService<br/>Orchestrator]
        YCS[YbaConfigService<br/>Config Management]
        VS[ValidationService<br/>Parameter Validation]
        BPS[BackupPollerService<br/>Async Polling Thread]
    end

    subgraph Integration["🔌 Integration Layer"]
        YC[YbaClient<br/>WebClient Integration]
        BDS[BackupDaoService<br/>JDBC Operations]
    end

    subgraph Database["💾 YugabyteDB - Tracking Database"]
        BET[(Batch Execution Table<br/>Managed by Batch Framework)]
        FBT[(full_backup_tracker<br/>Full Backup State + Base UUID)]
        IBT[(incremental_backup_tracker<br/>Incremental Backup State)]
    end

    subgraph External["☁️ External System"]
        YBA[YugabyteDB Anywhere<br/>REST API]
    end

    BS -->|1. POST Request<br/>JSON Payload| BC
    BC -->|2. execute| BKS
    BKS -->|3. Validate| VS
    BKS -->|4. Get Config| YCS
    BKS -->|5. backupInitiate| YC

    YC -->|6a. INSERT<br/>status=IN_PROGRESS| FBT
    YC -->|6b. INSERT<br/>status=IN_PROGRESS| IBT
    YC -->|7. POST /backups<br/>Initiate Backup| YBA
    YBA -->|8. taskUUID<br/>resourceUUID| YC
    YC -->|9a. UPDATE<br/>full_backup_response| FBT
    YC -->|9b. UPDATE<br/>incremental_backup_response| IBT

    YC -->|10. Return Success| BKS
    BKS -->|11. UPDATE<br/>status=COMPLETED| BET
    BKS -->|12. Response| BC
    BC -->|13. JSON Response| BS

    BPS -.->|14. Poll Job Status<br/>GET /tasks/{taskUUID}| YBA
    YBA -.->|15. Job Status| BPS
    BPS -.->|16. POST /backups/page<br/>Fetch Base UUID (Paginated)| YBA
    YBA -.->|17. {entities: [baseBackupUUID]}| BPS
    BPS -.->|18. UPDATE<br/>base_backup_uuid<br/>status=SUCCESS| FBT

    style BET fill:#e1f5ff,stroke:#0288d1,stroke-width:3px
    style FBT fill:#fff3e0,stroke:#f57c00,stroke-width:3px
    style IBT fill:#f3e5f5,stroke:#7b1fa2,stroke-width:3px
    style YBA fill:#e8f5e9,stroke:#388e3c,stroke-width:3px
    style BPS fill:#fff9c4,stroke:#f9a825,stroke-width:3px
    style YC fill:#fce4ec,stroke:#c2185b,stroke-width:3px
    style BKS fill:#e0f2f1,stroke:#00796b,stroke-width:3px
```

**Flow Steps:**
1. Client sends POST request with batch parameters
2. Controller delegates to BackupService
3. Validation of parameters
4. Configuration retrieval from YbaConfigService
5. YbaClient initiates backup
6. Database insert (full or incremental tracker)
7. YBA API call to initiate backup
8. YBA returns task UUID
9. Update tracker with response JSON
10. Return success to service
11. Update batch execution table
12. Return response to controller
13. JSON response to client
14-18. **Async polling** (dotted lines) - runs in parallel thread

**Color Legend:**
- 🔵 Blue: Batch Execution Table
- 🟠 Orange: Full Backup Tracker
- 🟣 Purple: Incremental Backup Tracker
- 🟢 Green: YBA External System
- 🟡 Yellow: Async Poller Service
- 🔴 Pink: YbaClient Integration
- 🟢 Teal: BackupService Orchestrator

---

## 4. Backup Lifecycle State Diagram

State machine showing backup lifecycle from request to completion.

```mermaid
stateDiagram-v2
    [*] --> RequestReceived: POST /backupProcess

    state "📥 Request Received" as RequestReceived
    state "✅ Validation" as Validation
    state "🔍 Type Detection" as TypeDetection
    state "📦 Full Backup Flow" as FullFlow
    state "📦 Incremental Backup Flow" as IncrFlow

    RequestReceived --> Validation: Extract parameters
    Validation --> TypeDetection: Parameters valid
    Validation --> Failed: ❌ Invalid parameters

    TypeDetection --> FullFlow: categoryCode contains 'FULL'
    TypeDetection --> IncrFlow: categoryCode contains 'INCRE'

    state FullFlow {
        [*] --> InsertFullRecord
        state "💾 Insert full_backup_tracker" as InsertFullRecord
        state "☁️ Call YBA API (Full)" as CallYBAFull
        state "📝 Update Response JSON" as UpdateFullResponse
        state "🔄 Polling Started" as PollingFull

        InsertFullRecord --> CallYBAFull: status=IN_PROGRESS
        CallYBAFull --> UpdateFullResponse: taskUUID received
        UpdateFullResponse --> PollingFull: Response stored

        state PollingFull {
            [*] --> CheckStatus
            state "🔍 Check Job Status" as CheckStatus
            state "⏳ Job Running" as JobRunning
            state "✅ Job Success" as JobSuccess
            state "❌ Job Failed" as JobFailed

            CheckStatus --> JobRunning: status=Running
            JobRunning --> CheckStatus: Wait 30s
            CheckStatus --> JobSuccess: status=Success
            CheckStatus --> JobFailed: status=Failure

            JobSuccess --> FetchBaseUUID
            state "🔑 Fetch Base UUID" as FetchBaseUUID
            FetchBaseUUID --> UpdateBaseUUID
            state "💾 Update base_backup_uuid" as UpdateBaseUUID
            UpdateBaseUUID --> [*]: status=SUCCESS

            JobFailed --> [*]: status=FAILED
        }

        PollingFull --> FullSuccess: Base UUID stored
        PollingFull --> FullFailed: Job failed

        state "✅ Full Backup Complete" as FullSuccess
        state "❌ Full Backup Failed" as FullFailed
    }

    state IncrFlow {
        [*] --> FetchBaseUUID_Incr
        state "🔍 Fetch Base UUID" as FetchBaseUUID_Incr
        state "💾 Insert incremental_backup_tracker" as InsertIncrRecord
        state "☁️ Call YBA API (Incremental)" as CallYBAIncr
        state "📝 Update Response JSON" as UpdateIncrResponse

        FetchBaseUUID_Incr --> InsertIncrRecord: Base UUID found
        FetchBaseUUID_Incr --> IncrFailed: ❌ No base UUID

        InsertIncrRecord --> CallYBAIncr: status=IN_PROGRESS<br/>with base_backup_uuid
        CallYBAIncr --> UpdateIncrResponse: taskUUID received
        UpdateIncrResponse --> IncrSuccess: Response stored

        state "✅ Incremental Backup Complete" as IncrSuccess
        state "❌ Incremental Backup Failed" as IncrFailed
    }

    FullFlow --> Completed: Success
    FullFlow --> Failed: Error
    IncrFlow --> Completed: Success
    IncrFlow --> Failed: Error

    state "✅ Completed" as Completed
    state "❌ Failed" as Failed

    Completed --> [*]
    Failed --> [*]

    note right of FullFlow
        Full backup stores base_backup_uuid
        for monthly incremental backups
    end note

    note right of IncrFlow
        Incremental backup requires
        existing base_backup_uuid
        from full backup
    end note
```

**State Transitions:**
- ✅ Request → Validation → Type Detection
- 🔵 Full Flow: Insert → Call YBA → Update → Poll → Update Base UUID
- 🟣 Incremental Flow: Fetch Base UUID → Insert → Call YBA → Update
- ❌ Error paths for validation failures and missing base UUID

---

## 5. Enhanced ER Diagram

Traditional entity-relationship diagram with all tables and relationships.

```mermaid
erDiagram
    BATCH_EXECUTION_TABLE ||--o{ FULL_BACKUP_TRACKER : "tracks"
    BATCH_EXECUTION_TABLE ||--o{ INCREMENTAL_BACKUP_TRACKER : "tracks"
    FULL_BACKUP_TRACKER ||--o{ INCREMENTAL_BACKUP_TRACKER : "provides base_uuid"

    BATCH_EXECUTION_TABLE {
        bigint id PK "Auto-increment"
        varchar batch_id UK "Unique batch identifier"
        varchar business_date "Execution date"
        varchar category_code "Backup category"
        varchar status "COMPLETED/FAILED"
        varchar error_message "Error details"
        timestamp created_at "Record creation time"
        timestamp updated_at "Last update time"
    }

    FULL_BACKUP_TRACKER {
        bigint id PK "Auto-increment"
        varchar batch_id "FK to batch execution"
        varchar category_code "Full backup category"
        date business_date "Backup execution date"
        varchar backup_month UK "YYYY-MM format"
        varchar backup_status "IN_PROGRESS/SUCCESS/FAILED"
        text full_backup_response "YBA API response JSON"
        varchar base_backup_uuid "Base UUID for incrementals"
        varchar task_uuid "YBA task identifier"
        text error_message "Error details if failed"
        timestamp start_time "Backup start time"
        timestamp end_time "Backup completion time"
        timestamp created_at "Record creation"
        timestamp updated_at "Last update"
    }

    INCREMENTAL_BACKUP_TRACKER {
        bigint id PK "Auto-increment"
        varchar batch_id "FK to batch execution"
        varchar category_code "Incremental backup category"
        date business_date "Backup execution date"
        varchar backup_month "YYYY-MM format"
        varchar base_backup_uuid FK "Reference to full backup"
        varchar backup_status "IN_PROGRESS/SUCCESS/FAILED"
        text incremental_backup_response "YBA API response JSON"
        varchar task_uuid "YBA task identifier"
        text error_message "Error details if failed"
        timestamp start_time "Backup start time"
        timestamp end_time "Backup completion time"
        timestamp created_at "Record creation"
        timestamp updated_at "Last update"
    }
```

**Relationships:**
- BATCH_EXECUTION_TABLE (1) → (0..many) FULL_BACKUP_TRACKER
- BATCH_EXECUTION_TABLE (1) → (0..many) INCREMENTAL_BACKUP_TRACKER
- FULL_BACKUP_TRACKER (1) → (0..many) INCREMENTAL_BACKUP_TRACKER

---

## 6. Detailed Database Schema with Constraints

Class diagram style showing all constraints, indexes, and field specifications.

```mermaid
classDiagram
    class BATCH_EXECUTION_TABLE {
        <<Managed by Batch Framework>>
        +bigint id PK
        +varchar(100) batch_id UK
        +varchar(20) business_date
        +varchar(100) category_code
        +varchar(20) status
        +text error_message
        +timestamp created_at
        +timestamp updated_at
        --
        Indexes:
        - idx_batch_id (batch_id)
        - idx_category_code (category_code)
        - idx_business_date (business_date)
    }

    class FULL_BACKUP_TRACKER {
        <<Full Backup State Management>>
        +bigint id PK
        +varchar(100) batch_id
        +varchar(100) category_code
        +date business_date
        +varchar(7) backup_month UK
        +varchar(20) backup_status
        +text full_backup_response
        +varchar(255) base_backup_uuid
        +varchar(255) task_uuid
        +text error_message
        +timestamp start_time
        +timestamp end_time
        +timestamp created_at
        +timestamp updated_at
        --
        Constraints:
        - UNIQUE(category_code, backup_month)
        - CHECK(backup_status IN ('IN_PROGRESS','SUCCESS','FAILED'))
        - CHECK(category_code LIKE '%FULL%')
        --
        Indexes:
        - idx_category_month (category_code, backup_month)
        - idx_base_uuid (base_backup_uuid)
        - idx_task_uuid (task_uuid)
        - idx_backup_status (backup_status)
    }

    class INCREMENTAL_BACKUP_TRACKER {
        <<Incremental Backup State Management>>
        +bigint id PK
        +varchar(100) batch_id
        +varchar(100) category_code
        +date business_date
        +varchar(7) backup_month
        +varchar(255) base_backup_uuid FK
        +varchar(20) backup_status
        +text incremental_backup_response
        +varchar(255) task_uuid
        +text error_message
        +timestamp start_time
        +timestamp end_time
        +timestamp created_at
        +timestamp updated_at
        --
        Constraints:
        - CHECK(backup_status IN ('IN_PROGRESS','SUCCESS','FAILED'))
        - CHECK(category_code LIKE '%INCRE%')
        - NOT NULL(base_backup_uuid)
        --
        Indexes:
        - idx_base_uuid (base_backup_uuid)
        - idx_category_month (category_code, backup_month)
        - idx_task_uuid (task_uuid)
        - idx_backup_status (backup_status)
        - idx_business_date (business_date)
    }

    class YBA_CONFIG_TABLE {
        <<YBA Configuration Management>>
        +bigint id PK
        +varchar(100) category_code UK
        +varchar(255) customer_uuid
        +varchar(255) universe_uuid
        +varchar(255) storage_config_uuid
        +varchar(100) keyspace_name
        +boolean sse
        +varchar(50) backup_type
        +bigint time_before_delete
        +varchar(20) expiry_time_unit
        +boolean is_active
        +timestamp created_at
        +timestamp updated_at
        --
        Constraints:
        - UNIQUE(category_code)
        - CHECK(backup_type IN ('PGSQL_TABLE_TYPE','YCQL_TABLE_TYPE'))
        - CHECK(expiry_time_unit IN ('MILLISECONDS','DAYS','HOURS'))
        --
        Indexes:
        - idx_category_code (category_code)
        - idx_is_active (is_active)
    }

    BATCH_EXECUTION_TABLE "1" --> "0..1" FULL_BACKUP_TRACKER : batch_id
    BATCH_EXECUTION_TABLE "1" --> "0..1" INCREMENTAL_BACKUP_TRACKER : batch_id
    FULL_BACKUP_TRACKER "1" --> "0..*" INCREMENTAL_BACKUP_TRACKER : base_backup_uuid
    YBA_CONFIG_TABLE "1" --> "0..*" FULL_BACKUP_TRACKER : category_code
    YBA_CONFIG_TABLE "1" --> "0..*" INCREMENTAL_BACKUP_TRACKER : category_code

    note for FULL_BACKUP_TRACKER "UNIQUE constraint on (category_code, backup_month)\nensures only ONE full backup per category per month.\nbase_backup_uuid is populated by BackupPollerService\nafter job completion."

    note for INCREMENTAL_BACKUP_TRACKER "Multiple incremental backups allowed per month.\nAll incremental backups in a month reference\nthe SAME base_backup_uuid from full_backup_tracker.\nNo UNIQUE constraint - allows daily incrementals."

    note for BATCH_EXECUTION_TABLE "Managed by Batch Core Library.\nBackupService only updates status field.\nDo not directly insert/delete records."
```

**Key Constraints:**
- ✅ **UNIQUE(category_code, backup_month)** on full_backup_tracker
- ✅ **CHECK** constraints for status values and category codes
- ✅ **NOT NULL** on base_backup_uuid in incremental_backup_tracker
- ✅ Multiple indexes for query performance optimization

**Important Notes:**
1. **full_backup_tracker**: Only one full backup per category per month (UNIQUE constraint)
2. **incremental_backup_tracker**: Multiple incrementals allowed (no UNIQUE constraint)
3. **batch_execution_table**: Managed by batch framework, service only updates status

---

## 7. Database Operations Flow

Visual flow showing database operations for full vs incremental backups.

```mermaid
graph TB
    subgraph Request["📥 Incoming Request"]
        REQ[POST /backupProcess<br/>batch_id, business_date, category_code]
    end

    subgraph Decision["🔍 Backup Type Detection"]
        CHECK{Category Code<br/>Contains?}
    end

    subgraph FullBackupFlow["🟦 FULL BACKUP DATABASE OPERATIONS"]
        style FullBackupFlow fill:#e3f2fd,stroke:#1976d2,stroke-width:3px

        F1[("1️⃣ INSERT<br/>full_backup_tracker")]
        F2[("2️⃣ UPDATE<br/>full_backup_response")]
        F3[("3️⃣ UPDATE<br/>batch_execution_table<br/>status=COMPLETED")]
        F4[("4️⃣ POLL YBA API<br/>(Async Thread)")]
        F5[("5️⃣ UPDATE<br/>base_backup_uuid<br/>status=SUCCESS")]

        F1 -->|Store task_uuid<br/>status=IN_PROGRESS| F2
        F2 -->|Store YBA response JSON| F3
        F3 -->|Batch completed| F4
        F4 -->|Job completed| F5

        style F1 fill:#bbdefb,stroke:#1565c0,stroke-width:2px
        style F2 fill:#90caf9,stroke:#1565c0,stroke-width:2px
        style F3 fill:#64b5f6,stroke:#1565c0,stroke-width:2px
        style F4 fill:#42a5f5,stroke:#1565c0,stroke-width:2px
        style F5 fill:#2196f3,stroke:#1565c0,stroke-width:2px
    end

    subgraph IncrBackupFlow["🟪 INCREMENTAL BACKUP DATABASE OPERATIONS"]
        style IncrBackupFlow fill:#f3e5f5,stroke:#7b1fa2,stroke-width:3px

        I1[("1️⃣ SELECT<br/>base_backup_uuid<br/>from full_backup_tracker")]
        I2{Base UUID<br/>Found?}
        I3[("2️⃣ INSERT<br/>incremental_backup_tracker<br/>with base_backup_uuid")]
        I4[("3️⃣ UPDATE<br/>incremental_backup_response")]
        I5[("4️⃣ UPDATE<br/>batch_execution_table<br/>status=COMPLETED")]
        I6[("❌ THROW ERROR<br/>Base UUID not found")]
        I7[("UPDATE<br/>batch_execution_table<br/>status=FAILED")]

        I1 -->|Query by category_code<br/>and backup_month| I2
        I2 -->|YES| I3
        I2 -->|NO| I6
        I3 -->|Store base_backup_uuid<br/>status=IN_PROGRESS| I4
        I4 -->|Store YBA response JSON| I5
        I6 --> I7

        style I1 fill:#e1bee7,stroke:#6a1b9a,stroke-width:2px
        style I2 fill:#ce93d8,stroke:#6a1b9a,stroke-width:2px
        style I3 fill:#ba68c8,stroke:#6a1b9a,stroke-width:2px
        style I4 fill:#ab47bc,stroke:#6a1b9a,stroke-width:2px
        style I5 fill:#9c27b0,stroke:#6a1b9a,stroke-width:2px
        style I6 fill:#f44336,stroke:#c62828,stroke-width:2px
        style I7 fill:#e53935,stroke:#c62828,stroke-width:2px
    end

    subgraph Tables["💾 DATABASE TABLES"]
        style Tables fill:#fff3e0,stroke:#f57c00,stroke-width:3px

        T1[(BATCH_EXECUTION_TABLE<br/>Batch Framework)]
        T2[(FULL_BACKUP_TRACKER<br/>Full Backup State)]
        T3[(INCREMENTAL_BACKUP_TRACKER<br/>Incremental State)]

        style T1 fill:#ffe0b2,stroke:#e65100,stroke-width:2px
        style T2 fill:#ffcc80,stroke:#e65100,stroke-width:2px
        style T3 fill:#ffb74d,stroke:#e65100,stroke-width:2px
    end

    REQ --> CHECK
    CHECK -->|"FULL"| FullBackupFlow
    CHECK -->|"INCRE"| IncrBackupFlow

    F1 -.->|INSERT| T2
    F2 -.->|UPDATE| T2
    F3 -.->|UPDATE| T1
    F5 -.->|UPDATE| T2

    I1 -.->|SELECT| T2
    I3 -.->|INSERT| T3
    I4 -.->|UPDATE| T3
    I5 -.->|UPDATE| T1
    I7 -.->|UPDATE| T1

    T2 -.->|Provides base_backup_uuid| T3
```

**Database Operations:**

**Full Backup (Blue):**
1. INSERT into full_backup_tracker (status=IN_PROGRESS)
2. UPDATE full_backup_response (YBA API response)
3. UPDATE batch_execution_table (status=COMPLETED)
4. POLL YBA API (async thread)
5. UPDATE base_backup_uuid and status=SUCCESS

**Incremental Backup (Purple):**
1. SELECT base_backup_uuid from full_backup_tracker
2. Check if base UUID found
3. INSERT into incremental_backup_tracker (with base_backup_uuid)
4. UPDATE incremental_backup_response (YBA API response)
5. UPDATE batch_execution_table (status=COMPLETED)
6-7. Error path: THROW ERROR and UPDATE batch_execution_table (status=FAILED)

**Color Legend:**
- 🔵 Blue: Full backup operations
- 🟣 Purple: Incremental backup operations
- 🟠 Orange: Database tables
- 🔴 Red: Error handling

---

## Summary

This document contains **7 comprehensive diagrams** covering:

1. ✅ **Full Backup Sequence** - Complete flow with 6 phases
2. ✅ **Incremental Backup Sequence** - With error handling
3. ✅ **Architecture Flow** - 18-step numbered flow
4. ✅ **State Diagram** - Lifecycle state machine
5. ✅ **ER Diagram** - Traditional entity relationships
6. ✅ **Detailed Schema** - With constraints and indexes
7. ✅ **Operations Flow** - Database operations comparison

**Usage:**
- Copy any diagram code block into a Mermaid-compatible viewer
- Use in documentation, presentations, or design reviews
- Reference for development and troubleshooting

**Tools to View Diagrams:**
- Mermaid Live Editor: https://mermaid.live
- GitHub/GitLab (native support)
- VS Code with Mermaid extension
- Confluence with Mermaid plugin
- Draw.io with Mermaid import

---

**Document Version:** 1.0
**Last Updated:** 2026-02-06
**Author:** Backup Orchestrator Service Team

