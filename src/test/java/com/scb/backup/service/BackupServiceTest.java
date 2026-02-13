package com.scb.backup.service;

import com.hdfcbank.epricing.batch.core.lib.dao.BatchExecutionDao;
import com.hdfcbank.epricing.batch.core.lib.model.BatchExecutionId;
import com.hdfcbank.epricing.batch.core.lib.model.BatchStartResponse;
import com.scb.backup.client.YbaClient;
import com.scb.backup.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackupService Comprehensive Tests")
class BackupServiceTest {

    @Mock
    private YbaClient ybaClient;

    @Mock
    private BatchExecutionDao batchExecutionDao;

    @Mock
    private BackupValidationService validationService;

    @InjectMocks
    private BackupService backupService;

    private Map<String, Object> batchParams;
    private String validPayload;
    private BatchStartResponse batchStartResponse;
    private BatchExecutionId batchExecutionId;

    @BeforeEach
    void setUp() {
        validPayload = "{\"subCategoryCode\":\"HWA_EPR_DB_BACKUP_FULL\"}";

        batchParams = new HashMap<>();
        batchParams.put(AppConstants.BATCH_ID, "BATCH_001");
        batchParams.put(AppConstants.CATEGORY_CODE, "HWA_EPR_DB_BACKUP");
        batchParams.put(AppConstants.PAYLOAD, validPayload);

        batchExecutionId = BatchExecutionId.builder()
                .batchExecutionId("BATCH_001")
                .batchExecutionDate("2026-02-13")
                .build();

        batchStartResponse = BatchStartResponse.builder()
                .id(batchExecutionId)
                .batchCategoryCode("HWA_EPR_DB_BACKUP")
                .executionStatus("IN_PROGRESS")
                .build();
    }

    // ==================== Process Method Tests ====================

    @Test
    @DisplayName("Should successfully process full backup")
    void should_ProcessFullBackup_When_ValidParametersProvided() {
        // Given
        String taskUuid = "task-uuid-123";
        doNothing().when(validationService).validateBatchParams(anyMap());
        when(batchExecutionDao.getBatchDetails(anyString(), anyString())).thenReturn(batchStartResponse);
        when(ybaClient.backupInitiate(anyString(), anyMap())).thenReturn(Mono.just(taskUuid));

        // When
        assertDoesNotThrow(() -> backupService.process(batchParams));

        // Then
        verify(validationService, times(1)).validateBatchParams(batchParams);
        verify(batchExecutionDao, times(1)).getBatchDetails("BATCH_001", "HWA_EPR_DB_BACKUP");
        verify(ybaClient, times(1)).backupInitiate(eq("HWA_EPR_DB_BACKUP_FULL"), anyMap());
    }

    @Test
    @DisplayName("Should handle validation failure in process method")
    void should_ThrowException_When_ValidationFails() {
        // Given
        doThrow(new IllegalArgumentException("Invalid batch parameters"))
                .when(validationService).validateBatchParams(anyMap());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                backupService.process(batchParams)
        );

        assertEquals("Invalid batch parameters", exception.getMessage());
        verify(validationService, times(1)).validateBatchParams(batchParams);
        verify(batchExecutionDao, never()).getBatchDetails(anyString(), anyString());
        verify(ybaClient, never()).backupInitiate(anyString(), anyMap());
    }

    @Test
    @DisplayName("Should handle exception during batch details retrieval")
    void should_ThrowException_When_BatchDetailsRetrievalFails() {
        // Given
        doNothing().when(validationService).validateBatchParams(anyMap());
        when(batchExecutionDao.getBatchDetails(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                backupService.process(batchParams)
        );

        assertEquals("Database connection failed", exception.getMessage());
        verify(validationService, times(1)).validateBatchParams(batchParams);
        verify(batchExecutionDao, times(1)).getBatchDetails("BATCH_001", "HWA_EPR_DB_BACKUP");
    }

    @Test
    @DisplayName("Should handle null payload")
    void should_ThrowException_When_PayloadIsNull() {
        // Given
        batchParams.put(AppConstants.PAYLOAD, null);
        doNothing().when(validationService).validateBatchParams(anyMap());

        // When & Then
        assertThrows(Exception.class, () ->
                backupService.process(batchParams)
        );

        verify(validationService, times(1)).validateBatchParams(batchParams);
    }

    @Test
    @DisplayName("Should handle empty payload")
    void should_ThrowException_When_PayloadIsEmpty() {
        // Given
        batchParams.put(AppConstants.PAYLOAD, "{}");
        doNothing().when(validationService).validateBatchParams(anyMap());
        when(batchExecutionDao.getBatchDetails(anyString(), anyString())).thenReturn(batchStartResponse);

        // When & Then
        assertThrows(Exception.class, () ->
                backupService.process(batchParams)
        );
    }

    // ==================== ProcessBackup Method Tests ====================

    @Test
    @DisplayName("Should successfully process backup with reactive flow")
    void should_ProcessBackupReactively_When_ValidParametersProvided() {
        // Given
        String batchId = "BATCH_001";
        String businessDate = "20260213";
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
        String taskUuid = "task-uuid-123";

        when(ybaClient.backupInitiate(anyString(), anyMap())).thenReturn(Mono.just(taskUuid));
        doNothing().when(batchExecutionDao).updateBatchStatus(anyString(), anyString(), anyMap(), anyString());

        // When
        Mono<Void> result = backupService.processBackup(batchId, businessDate, categoryCode);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(ybaClient, times(1)).backupInitiate(eq(categoryCode), anyMap());
        verify(batchExecutionDao, times(1)).updateBatchStatus(
                eq(batchId),
                eq(AppConstants.BATCH_COMPLETED_STATUS),
                anyMap(),
                eq(businessDate)
        );
    }

    @Test
    @DisplayName("Should handle backup success and update batch status")
    void should_UpdateBatchStatus_When_BackupSucceeds() {
        // Given
        String batchId = "BATCH_002";
        String businessDate = "20260214";
        String categoryCode = "HWA_EPR_DB_BACKUP_INCRE";
        String taskUuid = "task-uuid-456";

        when(ybaClient.backupInitiate(eq(categoryCode), anyMap())).thenReturn(Mono.just(taskUuid));
        doNothing().when(batchExecutionDao).updateBatchStatus(anyString(), anyString(), anyMap(), anyString());

        // When
        Mono<Void> result = backupService.processBackup(batchId, businessDate, categoryCode);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(batchExecutionDao, times(1)).updateBatchStatus(
                eq(batchId),
                eq(AppConstants.BATCH_COMPLETED_STATUS),
                anyMap(),
                eq(businessDate)
        );
    }

    @Test
    @DisplayName("Should handle backup failure in reactive flow")
    void should_HandleBackupFailure_When_YbaClientFails() {
        // Given
        String batchId = "BATCH_001";
        String businessDate = "20260213";
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
        String errorMessage = "YBA API error";

        when(ybaClient.backupInitiate(anyString(), anyMap()))
                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
        doNothing().when(batchExecutionDao).insertExceptionDetails(anyString(), anyString(), anyString(), anyMap(), anyString());
        doNothing().when(batchExecutionDao).updateBatchStatus(anyString(), anyString(), anyMap(), anyString());

        // When
        Mono<Void> result = backupService.processBackup(batchId, businessDate, categoryCode);

        // Then
        StepVerifier.create(result)
                .verifyComplete(); // Error is handled internally

        verify(ybaClient, times(1)).backupInitiate(eq(categoryCode), anyMap());
        verify(batchExecutionDao, times(1)).insertExceptionDetails(
                eq(batchId),
                eq(categoryCode),
                eq(errorMessage),
                anyMap(),
                eq(businessDate)
        );
        verify(batchExecutionDao, times(1)).updateBatchStatus(
                eq(batchId),
                eq(AppConstants.BATCH_FAILED_STATUS),
                anyMap(),
                eq(businessDate)
        );
    }

    @Test
    @DisplayName("Should handle network timeout error")
    void should_HandleNetworkTimeout_When_YbaClientTimesOut() {
        // Given
        String batchId = "BATCH_003";
        String businessDate = "20260215";
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";

        when(ybaClient.backupInitiate(anyString(), anyMap()))
                .thenReturn(Mono.error(new java.util.concurrent.TimeoutException("Request timeout")));
        doNothing().when(batchExecutionDao).insertExceptionDetails(anyString(), anyString(), anyString(), anyMap(), anyString());
        doNothing().when(batchExecutionDao).updateBatchStatus(anyString(), anyString(), anyMap(), anyString());

        // When
        Mono<Void> result = backupService.processBackup(batchId, businessDate, categoryCode);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(batchExecutionDao, times(1)).insertExceptionDetails(
                eq(batchId),
                eq(categoryCode),
                anyString(),
                anyMap(),
                eq(businessDate)
        );
    }

    // ==================== HandleProcessingError Method Tests ====================

    @Test
    @DisplayName("Should handle processing error and update database")
    void should_HandleProcessingError_When_ExceptionOccurs() {
        // Given
        String batchId = "BATCH_001";
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
        String businessDate = "20260213";
        RuntimeException exception = new RuntimeException("Processing failed");

        doNothing().when(batchExecutionDao).insertExceptionDetails(anyString(), anyString(), anyString(), anyMap(), anyString());
        doNothing().when(batchExecutionDao).updateBatchStatus(anyString(), anyString(), anyMap(), anyString());

        // When
        backupService.handleProcessingError(batchId, categoryCode, businessDate, exception);

        // Then
        verify(batchExecutionDao, times(1)).insertExceptionDetails(
                eq(batchId),
                eq(categoryCode),
                eq("Processing failed"),
                anyMap(),
                eq(businessDate)
        );
        verify(batchExecutionDao, times(1)).updateBatchStatus(
                eq(batchId),
                eq(AppConstants.BATCH_FAILED_STATUS),
                anyMap(),
                eq(businessDate)
        );
    }

    @Test
    @DisplayName("Should handle nested exception with cause")
    void should_HandleNestedException_When_ExceptionHasCause() {
        // Given
        String batchId = "BATCH_002";
        String categoryCode = "HWA_EPR_DB_BACKUP_INCRE";
        String businessDate = "20260214";
        RuntimeException cause = new RuntimeException("Root cause error");
        RuntimeException exception = new RuntimeException("Wrapper exception", cause);

        doNothing().when(batchExecutionDao).insertExceptionDetails(anyString(), anyString(), anyString(), anyMap(), anyString());
        doNothing().when(batchExecutionDao).updateBatchStatus(anyString(), anyString(), anyMap(), anyString());

        // When
        backupService.handleProcessingError(batchId, categoryCode, businessDate, exception);

        // Then
        verify(batchExecutionDao, times(1)).insertExceptionDetails(
                eq(batchId),
                eq(categoryCode),
                eq("Root cause error"),
                anyMap(),
                eq(businessDate)
        );
    }

    @Test
    @DisplayName("Should handle database error during error processing")
    void should_HandleDatabaseError_When_ErrorProcessingFails() {
        // Given
        String batchId = "BATCH_003";
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
        String businessDate = "20260215";
        RuntimeException originalException = new RuntimeException("Processing failed");

        doThrow(new RuntimeException("Database connection failed"))
                .when(batchExecutionDao).insertExceptionDetails(anyString(), anyString(), anyString(), anyMap(), anyString());

        // When & Then - Should not throw exception, just log error
        assertDoesNotThrow(() ->
                backupService.handleProcessingError(batchId, categoryCode, businessDate, originalException)
        );

        verify(batchExecutionDao, times(1)).insertExceptionDetails(
                eq(batchId),
                eq(categoryCode),
                eq("Processing failed"),
                anyMap(),
                eq(businessDate)
        );
    }

    // ==================== ExtractBusinessDate Method Tests ====================

    @Test
    @DisplayName("Should extract business date correctly")
    void should_ExtractBusinessDate_When_ValidBatchDetailsProvided() {
        // Given
        String batchId = "BATCH_001";
        String categoryCode = "HWA_EPR_DB_BACKUP";

        when(batchExecutionDao.getBatchDetails(batchId, categoryCode)).thenReturn(batchStartResponse);

        // When
        String actualDate = backupService.extractBusinessDate(batchId, categoryCode);

        // Then
        assertEquals("20260213", actualDate);
        verify(batchExecutionDao, times(1)).getBatchDetails(batchId, categoryCode);
    }

    @Test
    @DisplayName("Should handle null batch execution details")
    void should_ThrowException_When_BatchExecutionDetailsIsNull() {
        // Given
        String batchId = "BATCH_001";
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";

        when(batchExecutionDao.getBatchDetails(batchId, categoryCode)).thenReturn(null);

        // When & Then
        assertThrows(NullPointerException.class, () ->
                backupService.extractBusinessDate(batchId, categoryCode)
        );

        verify(batchExecutionDao, times(1)).getBatchDetails(batchId, categoryCode);
    }

    @Test
    @DisplayName("Should handle date with hyphens")
    void should_ExtractBusinessDate_When_DateHasHyphens() {
        // Given
        String batchId = "BATCH_002";
        String categoryCode = "HWA_EPR_DB_BACKUP_INCRE";

        BatchExecutionId id = BatchExecutionId.builder()
                .batchExecutionId(batchId)
                .batchExecutionDate("2026-02-14")
                .build();

        BatchStartResponse response = BatchStartResponse.builder()
                .id(id)
                .batchCategoryCode(categoryCode)
                .build();

        when(batchExecutionDao.getBatchDetails(batchId, categoryCode)).thenReturn(response);

        // When
        String actualDate = backupService.extractBusinessDate(batchId, categoryCode);

        // Then
        assertEquals("20260214", actualDate);
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("Should handle complete backup workflow end-to-end")
    void should_CompleteBackupWorkflow_When_AllStepsSucceed() {
        // Given
        String taskUuid = "task-uuid-789";
        doNothing().when(validationService).validateBatchParams(anyMap());
        when(batchExecutionDao.getBatchDetails(anyString(), anyString())).thenReturn(batchStartResponse);
        when(ybaClient.backupInitiate(anyString(), anyMap())).thenReturn(Mono.just(taskUuid));

        // When
        assertDoesNotThrow(() -> backupService.process(batchParams));

        // Then
        verify(validationService, times(1)).validateBatchParams(batchParams);
        verify(batchExecutionDao, times(1)).getBatchDetails("BATCH_001", "HWA_EPR_DB_BACKUP");
        verify(ybaClient, times(1)).backupInitiate(eq("HWA_EPR_DB_BACKUP_FULL"), anyMap());
    }

    @Test
    @DisplayName("Should handle multiple concurrent backup requests")
    void should_HandleConcurrentRequests_When_MultipleBackupsTriggered() {
        // Given
        String taskUuid1 = "task-uuid-001";
        String taskUuid2 = "task-uuid-002";

        when(ybaClient.backupInitiate(anyString(), anyMap()))
                .thenReturn(Mono.just(taskUuid1))
                .thenReturn(Mono.just(taskUuid2));
        doNothing().when(batchExecutionDao).updateBatchStatus(anyString(), anyString(), anyMap(), anyString());

        // When
        Mono<Void> result1 = backupService.processBackup("BATCH_001", "20260213", "HWA_EPR_DB_BACKUP_FULL");
        Mono<Void> result2 = backupService.processBackup("BATCH_002", "20260214", "HWA_EPR_DB_BACKUP_INCRE");

        // Then
        StepVerifier.create(result1).verifyComplete();
        StepVerifier.create(result2).verifyComplete();

        verify(ybaClient, times(2)).backupInitiate(anyString(), anyMap());
    }
}