# Test Changes Summary - Base UUID Validation

## Overview

This document describes the new test cases added to validate the base backup UUID storage and retrieval functionality with strict validation.

## New Test Files Created

### 1. YbaClientTest.java

**Location**: `src/test/java/com/scb/backup/client/YbaClientTest.java`

**Purpose**: Comprehensive unit tests for YbaClient with focus on base UUID validation

**Test Categories**:

#### A. Full Backup Tests
- ✅ `should_PerformFullBackup_And_StoreBaseUuid()`
  - Verifies full backup successfully stores base UUID in database
  - Validates UUID extraction from YBA response
  - Confirms `storeBaseBackupUuid()` is called with correct parameters

- ✅ `should_ExtractUuid_From_BackupUuidField()`
  - Tests UUID extraction from `backupUUID` field when `resourceUUID` not present
  - Validates fallback UUID extraction logic

#### B. Incremental Backup Tests - With Base UUID
- ✅ `should_PerformIncrementalBackup_When_BaseUuidExistsInDb()`
  - Verifies incremental backup succeeds when base UUID exists in DB
  - Confirms `getBaseBackupUuidFromDb()` is called
  - Validates incremental backup API call with correct base UUID

#### C. Incremental Backup Tests - Without Base UUID (Validation)
- ✅ `should_FailIncrementalBackup_When_BaseUuidNotFoundInDb()`
  - **KEY TEST**: Validates that incremental backup FAILS when base UUID not in DB
  - Verifies `IllegalStateException` is thrown
  - Confirms error message contains category code and month
  - Ensures no YBA API call is made

- ✅ `should_FailIncrementalBackup_When_BaseUuidIsEmpty()`
  - Tests validation when base UUID is empty string
  - Confirms same error handling as null UUID

#### D. Error Handling Tests
- ✅ `should_HandleException_When_ConfigNotFound()`
  - Tests behavior when configuration not found for category

- ✅ `should_HandleException_When_UnsupportedBackupType()`
  - Validates error handling for unsupported backup types

- ✅ `should_HandleException_When_StoringBaseUuidFails()`
  - Tests that backup completes even if storing UUID fails
  - Validates graceful degradation

**Total Tests**: 8

---

### 2. BackupDaoServiceTest.java

**Location**: `src/test/java/com/scb/backup/dao/BackupDaoServiceTest.java`

**Purpose**: Unit tests for BackupDaoService base UUID operations

**Test Categories**:

#### A. Store Base Backup UUID Tests
- ✅ `should_StoreBaseBackupUuid_When_ValidParametersProvided()`
  - Verifies successful storage of base UUID
  - Validates correct SQL parameters

- ✅ `should_UpdateBaseBackupUuid_When_AlreadyExists()`
  - Tests UPSERT behavior (update existing record)
  - Confirms idempotency

- ✅ `should_ThrowException_When_DatabaseUpdateFails()`
  - Validates `DbBackupException` thrown on database failure
  - Confirms error message contains category code

#### B. Get Base Backup UUID Tests
- ✅ `should_GetBaseBackupUuid_When_RecordExists()`
  - Verifies successful retrieval of base UUID
  - Validates correct SQL parameters

- ✅ `should_ReturnNull_When_RecordNotFound()`
  - **KEY TEST**: Confirms null returned when no record found
  - Tests `EmptyResultDataAccessException` handling

- ✅ `should_ThrowException_When_DatabaseQueryFails()`
  - Validates `DbBackupException` thrown on database failure
  - Confirms error message contains category code

**Total Tests**: 6

---

## Test Execution

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=YbaClientTest
mvn test -Dtest=BackupDaoServiceTest

# Run specific test method
mvn test -Dtest=YbaClientTest#should_FailIncrementalBackup_When_BaseUuidNotFoundInDb
```

### Expected Results

All tests should pass with the new validation logic:

```
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
```

---

## Key Validation Tests

### Critical Test: Incremental Backup Without Base UUID

**Test**: `should_FailIncrementalBackup_When_BaseUuidNotFoundInDb()`

**Scenario**:
1. Attempt incremental backup
2. Base UUID not found in database for current month
3. Expected: `IllegalStateException` thrown

**Assertions**:
```java
StepVerifier.create(result)
    .expectErrorMatches(throwable ->
        throwable instanceof IllegalStateException &&
        throwable.getMessage().contains("Base backup UUID not found") &&
        throwable.getMessage().contains(categoryCode) &&
        throwable.getMessage().contains(currentMonth) &&
        throwable.getMessage().contains("Please perform a full backup first")
    )
    .verify();
```

**Verifications**:
- `getBaseBackupUuidFromDb()` called once
- `webClient.post()` never called (no API call made)

---

## Test Coverage

### YbaClient Coverage
- ✅ Full backup flow with UUID storage
- ✅ Incremental backup with base UUID (success path)
- ✅ Incremental backup without base UUID (validation path)
- ✅ UUID extraction from multiple response fields
- ✅ Error handling for configuration issues
- ✅ Error handling for database failures

### BackupDaoService Coverage
- ✅ Store base UUID (insert)
- ✅ Store base UUID (update/upsert)
- ✅ Retrieve base UUID (found)
- ✅ Retrieve base UUID (not found)
- ✅ Error handling for database operations

---

## Integration with CI/CD

These tests should be integrated into the CI/CD pipeline:

```yaml
# Example GitHub Actions / Jenkins
- name: Run Unit Tests
  run: mvn test
  
- name: Generate Test Report
  run: mvn surefire-report:report
  
- name: Check Test Coverage
  run: mvn jacoco:report
```

---

## Test Data

### Mock Data Used

**Category Codes**:
- `HWA_EPR_DB_BACKUP_FULL` - Full backup
- `HWA_EPR_DB_BACKUP_INCRE` - Incremental backup

**Base UUIDs**:
- `backup-uuid-123`
- `base-backup-uuid-123`
- `new-backup-uuid-456`

**Months**:
- `2026-02` (current month format: YYYY-MM)

---

## Next Steps

1. ✅ Run tests locally to verify all pass
2. ✅ Integrate tests into CI/CD pipeline
3. ✅ Add test coverage reporting
4. ⏳ Consider adding integration tests with Testcontainers
5. ⏳ Add performance tests for database operations

---

## Notes

- All tests use Mockito for mocking dependencies
- Tests use `StepVerifier` for reactive stream testing
- Tests follow AAA pattern (Arrange, Act, Assert)
- Test names follow `should_ExpectedBehavior_When_Condition` convention

