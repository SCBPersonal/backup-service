package com.scb.backup.dao;

import com.scb.backup.exception.DbBackupException;
import com.scb.backup.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupDaoServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @InjectMocks
    private BackupDaoService backupDaoService;

    private Map<String, Object> testParam;
    private String testBackupPeriod = "2026-02";  // Renamed from testBackupMonth
    private String testTaskUuid = "task-uuid-123";
    private String testFullBackupResponse = "{\"status\":\"success\"}";
    private String testDbName = "testdb";
    private String testCategoryCode = "CAT001";
    private String testBaseBackupUuid = "base-uuid-456";
    private String testBatchId = "batch-001";
    private String testBusinessDateStr = "2026-02-15";
    private Date testBusinessDate;

    @BeforeEach
    void setUp() {
        testParam = new HashMap<>();
        testParam.put(AppConstants.CATEGORY_CODE, testCategoryCode);
        testBusinessDate = new Date(); // Current date for testing

        // Set query strings using reflection
        ReflectionTestUtils.setField(backupDaoService, "insertFullBackup",
                "INSERT INTO full_backup_tracker (...) VALUES (...)");
        ReflectionTestUtils.setField(backupDaoService, "updateFullBackupWithBaseUuidQuery",
                "UPDATE full_backup_tracker SET ...");
        ReflectionTestUtils.setField(backupDaoService, "getBaseBackupUuidFromFullTracker",
                "SELECT base_backup_uuid FROM full_backup_tracker WHERE ...");
        ReflectionTestUtils.setField(backupDaoService, "insertIncrementalBackup",
                "INSERT INTO incremental_backup_tracker (...) VALUES (...)");
        ReflectionTestUtils.setField(backupDaoService, "updateIncrementalBackupStatusQuery",
                "UPDATE incremental_backup_tracker SET ...");
    }

    // ==================== FULL BACKUP TEST CASES ====================

    @Test
    void insertFullBackupRecord_Success() {
        // When
        backupDaoService.insertFullBackupRecord(testParam, testBackupPeriod, testTaskUuid,
                testFullBackupResponse, testDbName);

        // Then
        verify(jdbcTemplate).update(eq("INSERT INTO full_backup_tracker (...) VALUES (...)"), any(Map.class));
    }

    @Test
    void insertFullBackupRecord_DatabaseFailure_ThrowsDbBackupException() {
        // Given
        doThrow(new RuntimeException("DB Error")).when(jdbcTemplate)
                .update(anyString(), any(Map.class));

        // When & Then
        DbBackupException exception = assertThrows(DbBackupException.class, () ->
                backupDaoService.insertFullBackupRecord(testParam, testBackupPeriod, testTaskUuid,
                        testFullBackupResponse, testDbName));

        assertTrue(exception.getMessage().contains(testCategoryCode));
        verify(jdbcTemplate).update(anyString(), any(Map.class));
    }

    @Test
    void updateFullBackupWithBaseUuid_Success() {
        // When
        backupDaoService.updateFullBackupWithBaseUuid(testCategoryCode, testBackupPeriod,
                testBaseBackupUuid, testBatchId, "SUCCESS");

        // Then
        verify(jdbcTemplate).update(eq("UPDATE full_backup_tracker SET ..."), any(Map.class));
    }

    @Test
    void updateFullBackupWithBaseUuid_DatabaseFailure_ThrowsDbBackupException() {
        // Given
        doThrow(new RuntimeException("DB Error")).when(jdbcTemplate)
                .update(anyString(), any(Map.class));

        // When & Then
        DbBackupException exception = assertThrows(DbBackupException.class, () ->
                backupDaoService.updateFullBackupWithBaseUuid(testCategoryCode, testBackupPeriod,
                        testBaseBackupUuid, testBatchId, "SUCCESS"));

        assertTrue(exception.getMessage().contains(testCategoryCode));
    }

    @Test
    void getBaseBackupUuidFromDb_Found() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(Map.class), eq(String.class)))
                .thenReturn(testBaseBackupUuid);

        // When
        String result = backupDaoService.getBaseBackupUuidFromDb(testBackupPeriod, testDbName);

        // Then
        assertEquals(testBaseBackupUuid, result);
        verify(jdbcTemplate).queryForObject(anyString(), any(Map.class), eq(String.class));
    }

    @Test
    void getBaseBackupUuidFromDb_NotFound_ReturnsEmptyString() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(Map.class), eq(String.class)))
                .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        // When
        String result = backupDaoService.getBaseBackupUuidFromDb(testBackupPeriod, testDbName);

        // Then
        assertEquals("", result);
    }

    @Test
    void getBaseBackupUuidFromDb_QueryFailure_ThrowsDbBackupException() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(Map.class), eq(String.class)))
                .thenThrow(new RuntimeException("Query Error"));

        // When & Then
        assertThrows(DbBackupException.class, () ->
                backupDaoService.getBaseBackupUuidFromDb(testBackupPeriod, testDbName));
    }

    // ==================== INCREMENTAL BACKUP TEST CASES ====================

    @Test
    void insertIncrementalBackupRecord_Success() {
        // When
        backupDaoService.insertIncrementalBackupRecord(testBatchId, testCategoryCode, testBusinessDate,
                testBackupPeriod);

        // Then
        verify(jdbcTemplate).update(eq("INSERT INTO incremental_backup_tracker (...) VALUES (...)"), any(Map.class));
    }

    @Test
    void insertIncrementalBackupRecord_DatabaseFailure_ThrowsDbBackupException() {
        // Given
        doThrow(new RuntimeException("DB Error")).when(jdbcTemplate)
                .update(anyString(), any(Map.class));

        // When & Then
        DbBackupException exception = assertThrows(DbBackupException.class, () ->
                backupDaoService.insertIncrementalBackupRecord(testBatchId, testCategoryCode, testBusinessDate,
                        testBackupPeriod));

        assertTrue(exception.getMessage().contains(testCategoryCode));
    }

    @Test
    void updateIncrementalBackupStatusByPeriod_Success() {
        // When
        backupDaoService.updateIncrementalBackupStatusByMonth(testCategoryCode, testBackupPeriod,
                testBaseBackupUuid, "SUCCESS", testBatchId,"","");

        // Then
        verify(jdbcTemplate).update(eq("UPDATE incremental_backup_tracker SET ..."), any(Map.class));
    }

    @Test
    void updateIncrementalBackupStatusByPeriod_DatabaseFailure_ThrowsDbBackupException() {
        // Given
        doThrow(new RuntimeException("DB Error")).when(jdbcTemplate)
                .update(anyString(), any(Map.class));

        // When & Then
        DbBackupException exception = assertThrows(DbBackupException.class, () ->
                backupDaoService.updateIncrementalBackupStatusByMonth(testCategoryCode, testBackupPeriod,
                        testBaseBackupUuid, "SUCCESS", testBatchId,"",""));

        assertTrue(exception.getMessage().contains(testCategoryCode));
    }

    @Test
    void updateIncrementalBackupStatusByPeriod_NullParameters_DoesNotThrow() {
        // When & Then - should not throw NPE as parameters are properly handled
        assertDoesNotThrow(() ->
                backupDaoService.updateIncrementalBackupStatusByMonth(null, null, null, null, null,null,null));
    }

    @Test
    void testWeeklyBackupPeriod() {
        // Test with weekly period format
        String weeklyPeriod = "2026-W10";
        when(jdbcTemplate.queryForObject(anyString(), any(Map.class), eq(String.class)))
                .thenReturn(testBaseBackupUuid);

        String result = backupDaoService.getBaseBackupUuidFromDb(weeklyPeriod, testDbName);

        assertEquals(testBaseBackupUuid, result);
    }

    @Test
    void testCustomIntervalBackupPeriod() {
        // Test with custom interval period format
        String customPeriod = "2026-03-11";
        when(jdbcTemplate.queryForObject(anyString(), any(Map.class), eq(String.class)))
                .thenReturn(testBaseBackupUuid);

        String result = backupDaoService.getBaseBackupUuidFromDb(customPeriod, testDbName);

        assertEquals(testBaseBackupUuid, result);
    }
}
