//
//package com.scb.backup.service;
//
//import com.scb.backup.client.YbaClient;
//import com.scb.backup.dao.BackupDaoService;
//import com.scb.backup.exception.DbBackupException;
//import com.scb.backup.utils.AppConstants;
//import com.scb.backup.utils.AppUtils;
//import com.scb.epricing.batch.core.lib.dao.BatchExecutionDao;
//import com.scb.epricing.batch.core.lib.model.BatchExecutionId;
//import com.scb.epricing.batch.core.lib.model.BatchStartResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockedStatic;
//import org.mockito.junit.jupiter.MockitoExtension;
//import reactor.core.publisher.Mono;
//
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("BackupService Comprehensive Tests")
//class BackupServiceTest {
//
//    @Mock
//    private YbaClient ybaClient;
//
//    @Mock
//    private BackupDaoService backupDaoService;
//
//    @Mock
//    private BatchExecutionDao batchExecutionDao;
//
//    @Mock
//    private BackupValidationService validationService;
//
//    @InjectMocks
//    private BackupService backupService;
//
//    private Map<String, Object> validBatchParams;
//    private BatchExecutionId batchExecutionId;
//    private BatchStartResponse successResponse;
//
//    @BeforeEach
//    void setUp() {
//        validBatchParams = new HashMap<>();
//        validBatchParams.put(AppConstants.BATCH_ID, "BATCH_001");
//        validBatchParams.put(AppConstants.CATEGORY_CODE, "HWA_EPR_DB_BACKUP_FULL");
//
//        batchExecutionId = new BatchExecutionId();
//        batchExecutionId.setBatchExecutionDate("20240101");
//
//
//        Map<String, Object> extensionFields = new HashMap<>();
//        extensionFields.put("requestId", "test-request-id");
//
//        successResponse = BatchStartResponse.builder()
//                .executionStatus("SUCCESS")
//                .extensionFields(extensionFields)
//                .build();
//    }
//
//    // Process Method Tests
//    @Test
//    @DisplayName("Should successfully process backup request")
//    void should_ProcessBackup_When_ValidParametersProvided() {
//        // Given
//        doNothing().when(validationService).validateBatchParams(validBatchParams);
//        when(batchExecutionDao.getBatchDetails(anyString(), anyString()))
//                .thenReturn(batchExecutionDetails);
//
//        try (MockedStatic<AppUtils> mockedAppUtils = mockStatic(AppUtils.class)) {
//            mockedAppUtils.when(() -> AppUtils.getBusinessDate(anyString()))
//                    .thenReturn("20240101");
//
//            // When
//            assertDoesNotThrow(() -> backupService.process(validBatchParams));
//
//            // Then
//            verify(validationService, times(1)).validateBatchParams(validBatchParams);
//            verify(batchExecutionDao, times(1)).getBatchDetails(anyString(), anyString());
//        }
//    }
//
//    @Test
//    @DisplayName("Should throw exception when validation fails")
//    void should_ThrowException_When_ValidationFails() {
//        // Given
//        doThrow(new IllegalArgumentException("Batch ID is required"))
//                .when(validationService).validateBatchParams(validBatchParams);
//
//        // When & Then
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//            backupService.process(validBatchParams);
//        });
//
//        assertEquals("Batch ID is required", exception.getMessage());
//        verify(validationService, times(1)).validateBatchParams(validBatchParams);
//        verify(batchExecutionDao, never()).getBatchDetails(anyString(), anyString());
//    }
//
//    @Test
//    @DisplayName("Should handle exception during batch details retrieval")
//    void should_HandleException_When_BatchDetailsRetrievalFails() {
//        // Given
//        doNothing().when(validationService).validateBatchParams(validBatchParams);
//        when(batchExecutionDao.getBatchDetails(anyString(), anyString()))
//                .thenThrow(new RuntimeException("Database connection failed"));
//
//        // When & Then
//        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
//            backupService.process(validBatchParams);
//        });
//
//        assertEquals("Database connection failed", exception.getMessage());
//        verify(validationService, times(1)).validateBatchParams(validBatchParams);
//        verify(batchExecutionDao, times(1)).getBatchDetails(anyString(), anyString());
//    }
//
//    // ProcessBackup Method Tests
//    @Test
//    @DisplayName("Should successfully process full backup")
//    void should_ProcessFullBackup_When_ValidParametersProvided() {
//        // Given
//        String batchId = "BATCH_001";
//        String businessDate = "20240101";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//        String ybaResponse = "backup-uuid-123";
//
//        when(ybaClient.backupInitiate(eq(categoryCode), any(Map.class)))
//                .thenReturn(Mono.just(ybaResponse));
//
//        doNothing().when(backupDaoService)
//                .updateBackupStatus(eq(batchId), eq(AppConstants.BACKUP_SUCCESS_STATUS), any(Date.class), eq(ybaResponse));
//
//        doNothing().when(batchExecutionDao)
//                .updateBatchStatus(eq(batchId), eq(AppConstants.BATCH_COMPLETED_STATUS), any(Map.class), eq(businessDate));
//
//        try (MockedStatic<AppUtils> mockedAppUtils = mockStatic(AppUtils.class)) {
//            mockedAppUtils.when(() -> AppUtils.createBatchParams(batchId, businessDate, categoryCode))
//                    .thenReturn(validBatchParams);
//            mockedAppUtils.when(() -> AppUtils.toDate(businessDate))
//                    .thenReturn(new Date());
//
//            // When & Then
//            StepVerifier.create(backupService.processBackup(batchId, businessDate, categoryCode))
//                    .verifyComplete();
//
//            verify(ybaClient, times(1)).backupInitiate(eq(categoryCode), any(Map.class));
//            verify(backupDaoService, times(1))
//                    .updateBackupStatus(eq(batchId), eq(AppConstants.BACKUP_SUCCESS_STATUS), any(Date.class), eq(ybaResponse));
//            verify(batchExecutionDao, times(1))
//                    .updateBatchStatus(eq(batchId), eq(AppConstants.BATCH_COMPLETED_STATUS), any(Map.class), eq(businessDate));
//        }
//    }
//
//    @Test
//    @DisplayName("Should handle backup failure and update status accordingly")
//    void should_HandleBackupFailure_When_YbaClientThrowsException() {
//        // Given
//        String batchId = "BATCH_001";
//        String businessDate = "20240101";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//        String errorMessage = "YBA service unavailable";
//
//        when(ybaClient.backupInitiate(eq(categoryCode), any(Map.class)))
//                .thenReturn(Mono.error(new RuntimeException(errorMessage)));
//
//        doNothing().when(backupDaoService)
//                .updateBackupStatus(eq(batchId), eq(AppConstants.BACKUP_FAILED_STATUS), any(Date.class), eq(errorMessage));
//
//        doNothing().when(batchExecutionDao)
//                .updateBatchStatus(eq(batchId), eq(AppConstants.BATCH_FAILED_STATUS), any(Map.class), eq(businessDate));
//
//        try (MockedStatic<AppUtils> mockedAppUtils = mockStatic(AppUtils.class)) {
//            mockedAppUtils.when(() -> AppUtils.createBatchParams(batchId, businessDate, categoryCode))
//                    .thenReturn(validBatchParams);
//            mockedAppUtils.when(() -> AppUtils.toDate(businessDate))
//                    .thenReturn(new Date());
//
//            // When & Then
//            StepVerifier.create(backupService.processBackup(batchId, businessDate, categoryCode))
//                    .verifyComplete();
//
//            verify(ybaClient, times(1)).backupInitiate(eq(categoryCode), any(Map.class));
//            verify(backupDaoService, times(1))
//                    .updateBackupStatus(eq(batchId), eq(AppConstants.BACKUP_FAILED_STATUS), any(Date.class), eq(errorMessage));
//            verify(batchExecutionDao, times(1))
//                    .updateBatchStatus(eq(batchId), eq(AppConstants.BATCH_FAILED_STATUS), any(Map.class), eq(businessDate));
//        }
//    }
//
//    @Test
//    @DisplayName("Should handle database update failure during success handling")
//    void should_HandleDatabaseFailure_When_SuccessUpdateFails() {
//        // Given
//        String batchId = "BATCH_001";
//        String businessDate = "20240101";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//        String ybaResponse = "backup-uuid-123";
//
//        when(ybaClient.backupInitiate(eq(categoryCode), any(Map.class)))
//                .thenReturn(Mono.just(ybaResponse));
//
//        doThrow(new RuntimeException("Database update failed"))
//                .when(backupDaoService)
//                .updateBackupStatus(eq(batchId), eq(AppConstants.BACKUP_SUCCESS_STATUS), any(Date.class), eq(ybaResponse));
//
//        try (MockedStatic<AppUtils> mockedAppUtils = mockStatic(AppUtils.class)) {
//            mockedAppUtils.when(() -> AppUtils.createBatchParams(batchId, businessDate, categoryCode))
//                    .thenReturn(validBatchParams);
//            mockedAppUtils.when(() -> AppUtils.toDate(businessDate))
//                    .thenReturn(new Date());
//
//            // When & Then
//            StepVerifier.create(backupService.processBackup(batchId, businessDate, categoryCode))
//                    .verifyComplete();
//
//            verify(ybaClient, times(1)).backupInitiate(eq(categoryCode), any(Map.class));
//            verify(backupDaoService, times(1))
//                    .updateBackupStatus(eq(batchId), eq(AppConstants.BACKUP_SUCCESS_STATUS), any(Date.class), eq(ybaResponse));
//        }
//    }
//
//    // GetBackupStatus Method Tests
//    @Test
//    @DisplayName("Should successfully get backup status")
//    void should_GetBackupStatus_When_ValidParametersProvided() {
//        // Given
//        String batchId = "BATCH_001";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//        String expectedStatus = "IN_PROGRESS";
//
//        when(backupDaoService.getBackupStatus(batchId, categoryCode))
//                .thenReturn(expectedStatus);
//
//        // When
//        Mono<String> result = backupService.getBackupStatus(batchId, categoryCode);
//
//        // Then
//        StepVerifier.create(result)
//                .expectNext(expectedStatus)
//                .verifyComplete();
//
//        verify(backupDaoService, times(1)).getBackupStatus(batchId, categoryCode);
//    }
//
//    @Test
//    @DisplayName("Should return empty when backup status not found")
//    void should_ReturnEmpty_When_BackupStatusNotFound() {
//        // Given
//        String batchId = "NONEXISTENT_BATCH";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//
//        when(backupDaoService.getBackupStatus(batchId, categoryCode))
//                .thenReturn(null);
//
//        // When
//        Mono<String> result = backupService.getBackupStatus(batchId, categoryCode);
//
//        // Then
//        StepVerifier.create(result)
//                .verifyComplete();
//
//        verify(backupDaoService, times(1)).getBackupStatus(batchId, categoryCode);
//    }
//
//    @Test
//    @DisplayName("Should handle exception during status retrieval")
//    void should_HandleException_When_StatusRetrievalFails() {
//        // Given
//        String batchId = "BATCH_001";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//
//        when(backupDaoService.getBackupStatus(batchId, categoryCode))
//                .thenThrow(new RuntimeException("Database connection failed"));
//
//        // When
//        Mono<String> result = backupService.getBackupStatus(batchId, categoryCode);
//
//        // Then
//        StepVerifier.create(result)
//                .expectError(RuntimeException.class)
//                .verify();
//
//        verify(backupDaoService, times(1)).getBackupStatus(batchId, categoryCode);
//    }
//
//    // Execute Method Tests (inherited from GenericBatchService)
//    @Test
//    @DisplayName("Should successfully execute batch with valid JSON")
//    void should_ExecuteBatch_When_ValidJsonProvided() throws Exception {
//        // Given
//        String validJson = "{\"batchId\":\"BATCH_001\",\"categoryCode\":\"HWA_EPR_DB_BACKUP_FULL\"}";
//
//        doNothing().when(validationService).validateBatchParams(any(Map.class));
//        when(batchExecutionDao.getBatchDetails(anyString(), anyString()))
//                .thenReturn(batchExecutionDetails);
//
//        try (MockedStatic<AppUtils> mockedAppUtils = mockStatic(AppUtils.class)) {
//            mockedAppUtils.when(() -> AppUtils.getBusinessDate(anyString()))
//                    .thenReturn("20240101");
//
//            // When
//            BatchStartResponse result = backupService.execute(validJson);
//
//            // Then
//            assertNotNull(result);
//            verify(validationService, times(1)).validateBatchParams(any(Map.class));
//        }
//    }
//
//    @Test
//    @DisplayName("Should throw exception when invalid JSON provided")
//    void should_ThrowException_When_InvalidJsonProvided() {
//        // Given
//        String invalidJson = "{invalid json}";
//
//        // When & Then
//        assertThrows(Exception.class, () -> {
//            backupService.execute(invalidJson);
//        });
//
//        verify(validationService, never()).validateBatchParams(any(Map.class));
//    }
//
//    // Business Date Extraction Tests
//    @Test
//    @DisplayName("Should extract business date correctly")
//    void should_ExtractBusinessDate_When_ValidBatchDetailsProvided() {
//        // Given
//        String batchId = "BATCH_001";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//        String expectedDate = "20240101";
//
//        when(batchExecutionDao.getBatchDetails(batchId, categoryCode))
//                .thenReturn(batchExecutionDetails);
//
//        try (MockedStatic<AppUtils> mockedAppUtils = mockStatic(AppUtils.class)) {
//            mockedAppUtils.when(() -> AppUtils.getBusinessDate("20240101"))
//                    .thenReturn(expectedDate);
//
//            // When
//            String actualDate = backupService.extractBusinessDate(batchId, categoryCode);
//
//            // Then
//            assertEquals(expectedDate, actualDate);
//            verify(batchExecutionDao, times(1)).getBatchDetails(batchId, categoryCode);
//        }
//    }
//
//    @Test
//    @DisplayName("Should handle null batch execution details")
//    void should_HandleNull_When_BatchExecutionDetailsIsNull() {
//        // Given
//        String batchId = "BATCH_001";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//
//        when(batchExecutionDao.getBatchDetails(batchId, categoryCode))
//                .thenReturn(null);
//
//        // When & Then
//        assertThrows(NullPointerException.class, () -> {
//            backupService.extractBusinessDate(batchId, categoryCode);
//        });
//
//        verify(batchExecutionDao, times(1)).getBatchDetails(batchId, categoryCode);
//    }
//
//    // Error Handling Tests
//    @Test
//    @DisplayName("Should handle processing error and update database")
//    void should_HandleProcessingError_When_ExceptionOccurs() {
//        // Given
//        String batchId = "BATCH_001";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//        String businessDate = "20240101";
//        RuntimeException exception = new RuntimeException("Processing failed");
//
//        doNothing().when(batchExecutionDao)
//                .insertExceptionDetails(eq(batchId), eq(categoryCode), anyString(), any(Map.class), eq(businessDate));
//        doNothing().when(batchExecutionDao)
//                .updateBatchStatus(eq(batchId), eq(AppConstants.BATCH_FAILED_STATUS), any(Map.class), eq(businessDate));
//
//        // When
//        backupService.handleProcessingError(batchId, categoryCode, businessDate, exception);
//
//        // Then
//        verify(batchExecutionDao, times(1))
//                .insertExceptionDetails(eq(batchId), eq(categoryCode), eq("Processing failed"), any(Map.class), eq(businessDate));
//        verify(batchExecutionDao, times(1))
//                .updateBatchStatus(eq(batchId), eq(AppConstants.BATCH_FAILED_STATUS), any(Map.class), eq(businessDate));
//    }
//
//    @Test
//    @DisplayName("Should handle nested exception with cause")
//    void should_HandleNestedException_When_ExceptionHasCause() {
//        // Given
//        String batchId = "BATCH_001";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//        String businessDate = "20240101";
//        RuntimeException cause = new RuntimeException("Root cause");
//        RuntimeException exception = new RuntimeException("Wrapper exception", cause);
//
//        doNothing().when(batchExecutionDao)
//                .insertExceptionDetails(eq(batchId), eq(categoryCode), eq("Root cause"), any(Map.class), eq(businessDate));
//        doNothing().when(batchExecutionDao)
//                .updateBatchStatus(eq(batchId), eq(AppConstants.BATCH_FAILED_STATUS), any(Map.class), eq(businessDate));
//
//        // When
//        backupService.handleProcessingError(batchId, categoryCode, businessDate, exception);
//
//        // Then
//        verify(batchExecutionDao, times(1))
//                .insertExceptionDetails(eq(batchId), eq(categoryCode), eq("Root cause"), any(Map.class), eq(businessDate));
//    }
//
//    @Test
//    @DisplayName("Should handle database error during error processing")
//    void should_HandleDatabaseError_When_ErrorProcessingFails() {
//        // Given
//        String batchId = "BATCH_001";
//        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
//        String businessDate = "20240101";
//        RuntimeException originalException = new RuntimeException("Processing failed");
//
//        doThrow(new RuntimeException("Database connection failed"))
//                .when(batchExecutionDao)
//                .insertExceptionDetails(eq(batchId), eq(categoryCode), anyString(), any(Map.class), eq(businessDate));
//
//        // When & Then - Should not throw exception, just log error
//        assertDoesNotThrow(() -> {
//            backupService.handleProcessingError(batchId, categoryCode, businessDate, originalException);
//        });
//
//        verify(batchExecutionDao, times(1))
//                .insertExceptionDetails(eq(batchId), eq(categoryCode), eq("Processing failed"), any(Map.class), eq(businessDate));
//    }
//}