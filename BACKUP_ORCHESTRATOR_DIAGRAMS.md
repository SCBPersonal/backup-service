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
        Poller->>+YBA: GET /api/v1/customers/{cUUID}/universes/{uUUID}/backups
        YBA-->>-Poller: [{baseBackupUUID, createTime, state}]
        
        Poller->>Poller: Extract latest baseBackupUUID
        
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


