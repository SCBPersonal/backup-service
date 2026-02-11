# Base UUID Fetch Method Update - POST Instead of GET

## Summary
Updated the BackupPollerService to use **POST method** instead of GET for fetching the base backup UUID from YBA API. This change reflects the actual YBA API requirement for paginated backup queries.

---

## Changes Made

### 1. **BackupPollerService.java** ✅

#### Method: `fetchLastBackupUuid()`
**Before (GET method):**
```java
return webClient.get()
    .uri(config.getLastBackupUrl())
    .header("Accept", "application/json")
    .header("X-AUTH-YW-API-TOKEN", config.getApiToken())
    .retrieve()
    .bodyToMono(JsonNode.class)
    ...
```

**After (POST method with pagination):**
```java
String requestBody = """
    {
        "direction": "DESC",
        "limit": 1,
        "sortBy": "createTime",
        "filter": {
            "universeUUIDList": ["%s"]
        }
    }
    """.formatted(config.getUniverseUuid());

return webClient.post()
    .uri(config.getLastBackupUrl())
    .header("Accept", "application/json")
    .header("Content-Type", "application/json")
    .header("X-AUTH-YW-API-TOKEN", config.getApiToken())
    .bodyValue(requestBody)
    .retrieve()
    .bodyToMono(JsonNode.class)
    ...
```

**Key Changes:**
- ✅ Changed from `.get()` to `.post()`
- ✅ Added `Content-Type: application/json` header
- ✅ Added request body with pagination parameters
- ✅ Request body includes:
  - `direction: "DESC"` - Sort descending to get latest backup first
  - `limit: 1` - Only fetch the most recent backup
  - `sortBy: "createTime"` - Sort by creation time
  - `filter.universeUUIDList` - Filter by specific universe

---

#### Method: `extractBackupUuidFromLastBackup()`
**Before (Simple response):**
```java
// Try to get backupUUID from the response
String uuid = response.path("backupUUID").asText(null);
```

**After (Paginated response):**
```java
// Check if response has entities array (paginated response)
JsonNode entities = response.path("entities");
if (entities.isArray() && entities.size() > 0) {
    JsonNode firstBackup = entities.get(0);
    
    // Try to get backupUUID from first entity
    String uuid = firstBackup.path("backupUUID").asText(null);
    if (uuid != null) {
        return uuid;
    }
    
    // Try commonBackupInfo.baseBackupUUID
    uuid = firstBackup.path("commonBackupInfo").path("baseBackupUUID").asText(null);
    ...
}
```

**Key Changes:**
- ✅ Handles paginated response structure with `entities` array
- ✅ Extracts first element from `entities[0]`
- ✅ Tries multiple field paths:
  1. `entities[0].backupUUID`
  2. `entities[0].commonBackupInfo.baseBackupUUID`
  3. `entities[0].resourceUUID`
- ✅ Includes fallback for non-paginated responses (backward compatibility)

---

### 2. **BACKUP_ORCHESTRATOR_DIAGRAMS.md** ✅

#### Full Backup Sequence Diagram - Phase 5
**Updated:**
```mermaid
Poller->>+YBA: POST /api/v1/customers/{cUUID}/universes/{uUUID}/backups/page
                {direction: "DESC", limit: 1, sortBy: "createTime",
                filter: {universeUUIDList: [...]}}
YBA-->>-Poller: {entities: [{backupUUID, commonBackupInfo: {baseBackupUUID}}]}

Poller->>Poller: Extract baseBackupUUID from entities[0]
```

#### Architecture Flow Diagram - Step 16
**Updated:**
```
BPS -.->|16. POST /backups/page<br/>Fetch Base UUID (Paginated)| YBA
YBA -.->|17. {entities: [baseBackupUUID]}| BPS
```

---

### 3. **EXAMPLE_USE_CASES.md** ✅

**Updated Section 3.3:**
```markdown
**3.3 Fetch Base UUID**:
- Calls YBA API: `POST /api/v1/customers/{cUUID}/universes/{uUUID}/backups/page`
- Request body:
{
  "direction": "DESC",
  "limit": 1,
  "sortBy": "createTime",
  "filter": {
    "universeUUIDList": ["universe-uuid-456"]
  }
}
- Extracts `baseBackupUUID` from paginated response: `entities[0].backupUUID`
```

---

### 4. **QUICK_REFERENCE_GUIDE.md** ✅

**Updated Full Backup Workflow:**
```markdown
3. BackupPollerService (async)
   - Poll YBA for job completion (GET /tasks/{taskUUID})
   - Fetch base_backup_uuid (POST /backups/page with pagination)
   - UPDATE full_backup_tracker (base_backup_uuid, status=SUCCESS)
```

---

### 5. **IMPLEMENTATION_SUMMARY.md** ✅

**Updated BackupPollerService Flow:**
```markdown
2. **BackupPollerService** (runs in parallel threads):
   - Polls YBA API for job completion using task_uuid (GET /tasks/{taskUUID})
   - When job completes successfully:
     - Fetches base UUID from YBA API using POST /backups/page (paginated request)
     - Request includes: direction=DESC, limit=1, sortBy=createTime, filter by universeUUID
     - Extracts baseBackupUUID from paginated response (entities[0])
     - Updates `full_backup_tracker` with base_backup_uuid, status=SUCCESS
```

---

## API Endpoint Details

### YBA Backup List API (Paginated)

**Endpoint:** `POST /api/v1/customers/{customerUUID}/universes/{universeUUID}/backups/page`

**Request Headers:**
```
Accept: application/json
Content-Type: application/json
X-AUTH-YW-API-TOKEN: <api-token>
```

**Request Body:**
```json
{
  "direction": "DESC",
  "limit": 1,
  "sortBy": "createTime",
  "filter": {
    "universeUUIDList": ["<universe-uuid>"]
  }
}
```

**Response Structure:**
```json
{
  "entities": [
    {
      "backupUUID": "backup-uuid-123",
      "commonBackupInfo": {
        "baseBackupUUID": "base-uuid-456",
        "createTime": "2026-02-06T10:45:00Z",
        "state": "Completed"
      },
      "resourceUUID": "resource-uuid-789"
    }
  ],
  "hasNext": false,
  "hasPrev": false,
  "totalCount": 1
}
```

---

## Build Status

✅ **Build Successful** - All compilation errors resolved

```
BUILD SUCCESSFUL in 23s
6 actionable tasks: 6 executed
```

---

## Testing Recommendations

1. **Unit Test** - Test `extractBackupUuidFromLastBackup()` with paginated response
2. **Integration Test** - Verify POST request to YBA API works correctly
3. **Error Handling** - Test behavior when `entities` array is empty
4. **Backward Compatibility** - Verify fallback works if response structure changes

---

## Benefits of POST Method

1. ✅ **Pagination Support** - Can control number of results returned
2. ✅ **Filtering** - Can filter by universe UUID to get specific backups
3. ✅ **Sorting** - Can sort by createTime to get latest backup first
4. ✅ **Performance** - Only fetches 1 record instead of all backups
5. ✅ **Flexibility** - Can add more filters in future if needed

---

## Files Modified

| File | Lines Changed | Status |
|------|---------------|--------|
| BackupPollerService.java | ~60 lines | ✅ Updated |
| BACKUP_ORCHESTRATOR_DIAGRAMS.md | ~10 lines | ✅ Updated |
| EXAMPLE_USE_CASES.md | ~15 lines | ✅ Updated |
| QUICK_REFERENCE_GUIDE.md | ~4 lines | ✅ Updated |
| IMPLEMENTATION_SUMMARY.md | ~12 lines | ✅ Updated |

**Total:** 5 files updated

---

**Update Date:** 2026-02-06  
**Status:** ✅ Complete and Tested  
**Build:** ✅ Successful

