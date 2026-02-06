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

**Configuration Properties**:
```yaml
backup:
  polling:
    interval-seconds: 30      # Poll every 30 seconds
    max-duration-hours: 2     # Stop polling after 2 hours
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
```yaml
backup:
  polling:
    interval-seconds: 30      # How often to poll YBA API
    max-duration-hours: 2     # Maximum time to poll before giving up
```

### Thread Pool Configuration
Spring Boot's default async executor is used. For production, consider configuring a custom thread pool:

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("backup-poller-");
        executor.initialize();
        return executor;
        }
}
```

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

1. `src/main/java/com/scb/backup/service/BackupPollerService.java` (NEW)
2. `src/main/java/com/scb/backup/BackupOrchestratorApplication.java`
3. `src/main/java/com/scb/backup/client/YbaClient.java`
4. `src/main/resources/application.yml` (configuration properties)

## Build Status

✅ Build successful with no compilation errors

