# Async Polling Implementation Summary

## Overview
This document summarizes the implementation of asynchronous background polling for YugabyteDB backup job completion monitoring. The new workflow eliminates the need for immediate polling after backup initiation, allowing the API to return quickly while monitoring job status in the background.

## Architecture Changes

### 1. New Service: BackupPollerService
**File**: `src/main/java/com/scb/backup/service/BackupPollerService.java`

**Purpose**: Manages background polling of YBA task status until job completion.

**Key Features**:
- Asynchronous polling using `@Async` annotation
- Configurable polling interval (default: 30 seconds)
- Configurable max polling duration (default: 2 hours)
- Automatic retry with exponential backoff
- Updates database when job completes successfully
- Handles failures and timeouts gracefully

**Key Methods**:
- `startPolling()`: Initiates background polling for a full backup task
- `pollTaskStatus()`: Polls YBA API for task status
- `fetchAndStoreBaseUuid()`: Fetches base UUID from YBA and updates database
- `extractBackupUuidFromLastBackup()`: Extracts backup UUID from YBA response

**Configuration Properties** (via `BackupPollerProperties`):
```yaml
backup:
  poller:
    enabled: true                    # Enable/disable poller
    initial-delay-ms: 5000          # Initial delay before first poll (5 seconds)
    polling-interval-ms: 30000      # Poll every 30 seconds
    thread-pool-size: 5             # Number of concurrent polling threads
    # Note: job-completion-check-url is now configured per database in yba.databases section
    # Note: max-poll-attempts removed - now supports infinite retry until job completes
```

### 2. Application Configuration
**File**: `src/main/java/com/scb/backup/BackupOrchestratorApplication.java`

**Changes**:
- Added `@EnableAsync` annotation to enable asynchronous processing
- Updated JavaDoc to reflect background polling feature

### 3. YbaClient Updates
**File**: `src/main/java/com/scb/backup/client/YbaClient.java`

**Changes**:
- Injected `BackupPollerService` dependency
- Updated `fullBackup()` method to:
  - Accept `batchParams` parameter
  - Extract task UUID from YBA response
  - Store full backup record in database
  - Start background polling via `pollerService.startPolling()`
- Updated `performIncrementalBackup()` method to:
  - Accept `batchParams` parameter
  - Retrieve base UUID from `full_backup_tracker` table instead of old table
- Updated `incrementalBackup()` method to:
  - Accept additional parameters (batchParams, categoryCode, backupMonth)
  - Store incremental backup record in database
- Added `extractTaskUuid()` method to extract task UUID from YBA response
- Removed `fetchLastBackupAndStoreBaseUuid()` method (replaced by poller)

### 4. Database Schema
**Tables Used**:
- `full_backup_tracker`: Stores full backup details including task UUID and base UUID
- `incremental_backup_tracker`: Stores incremental backup details
- `base_backup_uuid_tracker`: Legacy table (still used for backward compatibility)

**Key Columns in full_backup_tracker**:
- `task_uuid`: YBA task UUID for polling
- `base_backup_uuid`: Populated after job completion
- `backup_status`: IN_PROGRESS, SUCCESS, FAILED
- `full_backup_response`: YBA API response JSON

## Workflow Comparison

### Old Workflow (Synchronous)
1. Client calls backup API
2. YbaClient triggers backup via YBA API
3. YbaClient immediately calls "last backup" API to get base UUID
4. Store base UUID in database
5. Return response to client
**Problem**: Step 3 often fails because backup job hasn't completed yet

### New Workflow (Asynchronous)
1. Client calls backup API
2. YbaClient triggers backup via YBA API
3. Extract task UUID from response
4. Store backup record with task UUID in database
5. **Start background poller** (non-blocking)
6. **Return response immediately to client**
7. Poller monitors task status every 30 seconds
8. When job completes, poller fetches base UUID and updates database

**Benefits**:
- API returns quickly (no waiting for job completion)
- Reliable base UUID retrieval (waits for actual completion)
- Better error handling and retry logic
- Scalable (multiple jobs can be polled concurrently)

## Configuration

### application.yml
The poller configuration is present in `application.yml`:

```yaml
yba:
  # Common YBA Configuration
  base-url: ${YBA_BASE_URL:https://yba-api.example.com/api/v1}
  customer-id: ${YBA_CUSTOMER_ID:cust123}

  databases:
    uam-db:
      full-backup-url: ${UAM_FULL_BACKUP_URL:${yba.base-url}/customers/${yba.customer-id}/backups}
      incremental-backup-url: ${UAM_INCREMENTAL_BACKUP_URL:${yba.base-url}/customers/${yba.customer-id}/backups/incremental}
      last-backup-url: ${UAM_LAST_BACKUP_URL:${yba.base-url}/customers/${yba.customer-id}/backups?limit=1&direction=DESC}
      job-completion-check-url: ${UAM_JOB_CHECK_URL:${yba.base-url}/customers/${yba.customer-id}/tasks/{taskUuid}}
      storage-config-uuid: ${UAM_STORAGE_CONFIG_UUID:}
      api-token: ${UAM_API_TOKEN:}
      universe-uuid: ${UAM_UNIVERSE_UUID:}
      backup-type: PGSQL_TABLE_TYPE
      backup-category-type: full_backup
      db-name: ${UAM_DB_NAME:hbl_gcp_uat_epr_db}
      expiry-ms: 172800000

# Backup Poller Configuration
# Note: job-completion-check-url is configured per database in yba.databases section
backup:
  poller:
    enabled: ${BACKUP_POLLER_ENABLED:true}
    initial-delay-ms: ${BACKUP_POLLER_INITIAL_DELAY_MS:5000}
    polling-interval-ms: ${BACKUP_POLLER_INTERVAL_MS:30000}
    thread-pool-size: ${BACKUP_POLLER_THREAD_POOL_SIZE:5}
```

**Environment Variables**:
- `BACKUP_POLLER_ENABLED`: Enable/disable poller (default: true)
- `BACKUP_POLLER_INITIAL_DELAY_MS`: Initial delay in ms (default: 5000)
- `BACKUP_POLLER_INTERVAL_MS`: Polling interval in ms (default: 30000)
- `BACKUP_POLLER_THREAD_POOL_SIZE`: Thread pool size (default: 5)
- `YBA_BASE_URL`: YBA API base URL
- `YBA_CUSTOMER_ID`: YBA customer ID
- `UAM_JOB_CHECK_URL`: Job status endpoint (per database, contains `{taskUuid}` placeholder)

**Important Changes**:
- ❌ **Removed**: `max-poll-attempts` - Now supports **infinite retry** until job completes
- ✅ **Added**: `job-completion-check-url` per database configuration
- ✅ **Added**: Common `base-url` and `customer-id` for URL templating

### Thread Pool Configuration
Spring Boot's default async executor is used with `@EnableAsync` annotation.
The thread pool size can be controlled via `backup.poller.thread-pool-size` property.

## Testing Recommendations

1. **Unit Tests**:
   - Test `BackupPollerService.pollTaskStatus()` with mocked WebClient
   - Test `extractBackupUuidFromLastBackup()` with various response formats
   - Test timeout and failure scenarios

2. **Integration Tests**:
   - Test full backup workflow end-to-end
   - Verify database records are created and updated correctly
   - Test concurrent backup jobs

3. **Manual Testing**:
   - Trigger a full backup and verify poller starts
   - Check database for task UUID and status updates
   - Verify base UUID is populated after job completion
   - Test incremental backup using the stored base UUID

## Monitoring and Logging

Key log messages to monitor:
- `"Starting background polling for full backup task..."` - Poller started
- `"Polling task status for task UUID: {}"` - Each poll attempt
- `"Task completed successfully"` - Job finished
- `"Base backup UUID stored successfully"` - UUID retrieved and saved
- `"Polling timed out after {} attempts"` - Timeout occurred
- `"Error polling task status"` - Polling failed

## Future Enhancements

1. **Persistent Poller Recovery**: Store polling state in database to resume after service restart
2. **Webhook Support**: Replace polling with YBA webhooks if available
3. **Metrics**: Add Prometheus metrics for polling success/failure rates
4. **Admin API**: Provide endpoints to view/manage active pollers
5. **Incremental Backup Polling**: Extend polling to incremental backups if needed

## Files Modified

### New Files Created:
1. `src/main/java/com/scb/backup/service/BackupPollerService.java` - Background polling service
2. `src/main/java/com/scb/backup/config/BackupPollerProperties.java` - Configuration properties class

### Existing Files Modified:
3. `src/main/java/com/scb/backup/BackupOrchestratorApplication.java` - Added @EnableAsync
4. `src/main/java/com/scb/backup/client/YbaClient.java` - Updated backup workflow

### Configuration Files:
5. `src/main/resources/application.yml` - Already contains poller configuration

## Build Status

✅ **Build successful with no compilation errors**

```bash
./gradlew clean build -x test
BUILD SUCCESSFUL in 23s
```

## Next Steps

1. **Test the Implementation**:
   - Trigger a full backup and verify poller starts
   - Check logs for polling activity
   - Verify database updates when job completes

2. **Monitor in Production**:
   - Set up alerts for polling failures
   - Monitor thread pool utilization
   - Track backup completion times

3. **Optional Enhancements**:
   - Add metrics/monitoring endpoints
   - Implement persistent poller recovery
   - Add admin API to view active pollers

