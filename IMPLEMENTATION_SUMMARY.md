# Backup Orchestrator Service - Implementation Summary

## Overview
This document summarizes the implementation of the new backup tracking system using three tables: batch execution table (handled by batch core library), full_backup_tracker, and incremental_backup_tracker.

## Architecture

### Three-Table Design

1. **Batch Execution Table** (handled by batch core library)
   - Managed by the batch framework
   - Tracks overall batch job status
   - No changes required

2. **full_backup_tracker Table**
   - Tracks full backup operations
   - Stores full backup response immediately after API call
   - Base UUID populated by BackupPollerService after job completion
   - One record per category per month (UNIQUE constraint)

3. **incremental_backup_tracker Table**
   - Tracks incremental backup operations
   - References base UUID from full_backup_tracker
   - Multiple records allowed per category per month

## Implementation Flow

### Full Backup Flow

1. **YbaClient.fullBackup()**:
   - Calls YBA API for full backup
   - Extracts task UUID from response
   - Inserts record into `full_backup_tracker` with:
     - batch_id, category_code, business_date, backup_month
     - backup_status = 'IN_PROGRESS'
     - task_uuid (for polling)
   - Updates `full_backup_tracker` with full backup response (JSON)

2. **BackupPollerService** (runs in parallel threads):
   - Polls YBA API for job completion using task_uuid
   - When job completes successfully:
     - Fetches base UUID from YBA API (latest backup)
     - Updates `full_backup_tracker` with:
       - base_backup_uuid
       - backup_status = 'SUCCESS'
       - end_time
   - On failure:
     - Updates backup_status = 'FAILED'
     - Sets error_message

### Incremental Backup Flow

1. **YbaClient.performIncrementalBackup()**:
   - Fetches base UUID from `full_backup_tracker` for current month
   - If base UUID not found, throws IllegalStateException
   - Calls YBA API for incremental backup with base UUID
   - Extracts task UUID from response
   - Inserts record into `incremental_backup_tracker` with:
     - batch_id, category_code, business_date, backup_month
     - base_backup_uuid (from full_backup_tracker)
     - backup_status = 'IN_PROGRESS'
     - task_uuid
   - Updates `incremental_backup_tracker` with incremental backup response (JSON)

2. **BackupPollerService** (future enhancement):
   - Can poll incremental backup jobs if needed
   - Updates incremental_backup_tracker status

## Key Changes Made

### 1. BackupDaoService.java
**Added Methods**:
- `insertFullBackupRecord()` - Insert full backup record
- `updateFullBackupResponse()` - Update with API response
- `updateFullBackupStatus()` - Update status and error message
- `updateFullBackupWithBaseUuid()` - Update with base UUID (called by poller)
- `getBaseBackupUuidFromDb()` - Retrieve base UUID for incremental backup
- `insertIncrementalBackupRecord()` - Insert incremental backup record
- `updateIncrementalBackupStatus()` - Update incremental backup status

**Removed**:
- All legacy methods (insertBackupDetails, updateBackupStatus, storeBaseBackupUuid)

### 2. application.yml
**Added Queries**:
- `insert-full-backup` - Insert into full_backup_tracker
- `update-full-backup-response` - Update with API response
- `update-full-backup-status` - Update status/error
- `update-full-backup-with-base-uuid` - Update with base UUID
- `get-base-backup-uuid-from-full-tracker` - Get base UUID
- `insert-incremental-backup` - Insert into incremental_backup_tracker
- `update-incremental-backup-status` - Update incremental status

**Removed**:
- All legacy queries (db-schedule-backup-insert, update-schedule-backup, insert-base-backup-uuid)

### 3. YbaClient.java
**Modified Methods**:
- `backupInitiate()` - Removed legacy insertBackupDetails call
- `fullBackup()` - Now inserts into full_backup_tracker and updates with response
- `performIncrementalBackup()` - Fetches base UUID from full_backup_tracker, inserts into incremental_backup_tracker

**Added Methods**:
- `extractTaskUuidFromResponse()` - Extract task UUID for polling

### 4. BackupService.java
**Modified Methods**:
- `handleBackupSuccess()` - Removed legacy updateBackupStatus call
- `handleBackupFailure()` - Removed legacy updateBackupStatus call
- Now only updates batch execution status in batch framework

### 5. Database Schema (V3 Migration)
**Tables Created**:
- `full_backup_tracker` - With UNIQUE(category_code, backup_month)
- `incremental_backup_tracker` - Multiple records allowed

## Benefits

1. **Clear Separation of Concerns**:
   - Batch framework handles batch execution
   - full_backup_tracker handles full backup lifecycle
   - incremental_backup_tracker handles incremental backup lifecycle

2. **Async Polling Support**:
   - Task UUID stored for polling
   - BackupPollerService can monitor job completion in parallel

3. **Monthly Base UUID Management**:
   - One base UUID per category per month
   - Automatic retrieval for incremental backups

4. **Better Tracking**:
   - Full backup response stored immediately
   - Base UUID populated after job completion
   - Clear audit trail with timestamps

## Testing Recommendations

1. Test full backup flow end-to-end
2. Verify BackupPollerService updates base UUID correctly
3. Test incremental backup with valid base UUID
4. Test incremental backup failure when base UUID missing
5. Verify UNIQUE constraint on full_backup_tracker
6. Test concurrent backups for different categories

## Next Steps

1. Ensure BackupPollerService is properly configured and running
2. Add monitoring for backup job completion
3. Add alerts for failed backups
4. Consider adding retry logic for failed backups

