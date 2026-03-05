package com.scb.backup.service;

import com.scb.backup.client.YbaClient;
import com.hdfcbank.epricing.batch.core.lib.dao.BatchExecutionDao;
import com.scb.backup.utils.AppConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class BackupServiceTest {
    @Mock
    private YbaClient ybaClient;
    @Mock
    private BatchExecutionDao batchExecutionDao;
    @Mock
    private BackupValidationService validationService;
    @InjectMocks
    private BackupService backupService;
    private final String batchId = "BATCH_20260216";
    private final String businessDate = "20260216";
    private final String categoryCode = "TEST_CATEGORY";
    private Map<String, Object> createBatchParams() {
        Map<String, Object> map = new HashMap<>();
        map.put(AppConstants.BATCH_ID, batchId);
        map.put(AppConstants.CATEGORY_CODE, categoryCode);
        map.put(AppConstants.PAYLOAD,
                "{\"" + AppConstants.SUB_CATEGORY_CODE + "\":\"" + categoryCode + "\"}");
        return map;
    }
    @Test
    void shouldProcessBackupSuccessfully() {
        when(ybaClient.backupInitiate(anyString(), anyMap()))
                .thenReturn(Mono.just("SUCCESS_RESPONSE"));
        Mono<Void> result =
                backupService.processBackup(batchId, businessDate, categoryCode);
        StepVerifier.create(result)
                .verifyComplete();
        verify(batchExecutionDao)
                .updateBatchStatus(eq(batchId),
                        eq(AppConstants.BATCH_COMPLETED_STATUS),
                        anyMap(),
                        eq(businessDate));
    }

    @Test
    void shouldHandleBackupFailure() {
        when(ybaClient.backupInitiate(anyString(), anyMap()))
                .thenReturn(Mono.error(new RuntimeException("API FAILED")));
        Mono<Void> result =
                backupService.processBackup(batchId, businessDate, categoryCode);
        StepVerifier.create(result)
                .verifyComplete(); // because onErrorResume handles it
        verify(batchExecutionDao)
                .updateBatchStatus(eq(batchId),
                        eq(AppConstants.BATCH_FAILED_STATUS),
                        anyMap(),
                        eq(businessDate));
        verify(batchExecutionDao)
                .insertExceptionDetails(eq(batchId),
                        eq(categoryCode),
                        eq("API FAILED"),
                        anyMap(),
                        eq(businessDate));
    }
    @Test
    void shouldCallProcessFromFramework() {
        Map<String, Object> params = createBatchParams();
        doNothing().when(validationService).validateBatchParams(params);
        // mock business date extraction
        var mockDetails = mock(com.hdfcbank.epricing.batch.core.lib.model.BatchStartResponse.class);
        var mockId = mock(com.hdfcbank.epricing.batch.core.lib.model.BatchExecutionId.class);
        when(batchExecutionDao.getBatchDetails(anyString(), anyString()))
                .thenReturn(mockDetails);
        when(mockDetails.getId()).thenReturn(mockId);
        when(mockId.getBatchExecutionDate()).thenReturn("2026-02-16");
        when(ybaClient.backupInitiate(nullable(String.class), anyMap()))
                .thenReturn(Mono.just("OK"));
        backupService.process(params);
        verify(validationService).validateBatchParams(params);
    }
    @Test
    void shouldHandleProcessingError() {
        RuntimeException ex = new RuntimeException("Test Error");
        backupService.handleProcessingError(
                batchId,
                categoryCode,
                businessDate,
                ex
        );
        verify(batchExecutionDao)
                .insertExceptionDetails(eq(batchId),
                        eq(categoryCode),
                        eq("Test Error"),
                        anyMap(),
                        eq(businessDate));
        verify(batchExecutionDao)
                .updateBatchStatus(eq(batchId),
                        eq(AppConstants.BATCH_FAILED_STATUS),
                        anyMap(),
                        eq(businessDate));
    }
    @Test
    void shouldExtractBusinessDate() {
        var mockDetails = mock(com.hdfcbank.epricing.batch.core.lib.model.BatchStartResponse.class);
        var mockId = mock(com.hdfcbank.epricing.batch.core.lib.model.BatchExecutionId.class);
        when(batchExecutionDao.getBatchDetails(batchId, categoryCode))
                .thenReturn(mockDetails);
        when(mockDetails.getId()).thenReturn(mockId);
        when(mockId.getBatchExecutionDate()).thenReturn("2026-02-16");
        String result =
                backupService.extractBusinessDate(batchId, categoryCode);
        verify(batchExecutionDao).getBatchDetails(batchId, categoryCode);
    }
}
