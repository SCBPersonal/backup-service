package com.scb.backup.service;

import com.scb.backup.client.YbaClient;
import com.scb.backup.dao.BackupDaoService;
import com.scb.backup.utils.AppConstants;
import com.scb.backup.utils.AppUtils;
import com.scb.epricing.batch.core.lib.dao.BatchExecutionDao;
import com.scb.epricing.batch.core.lib.service.GenericBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class BackupService extends GenericBatchService {

    private final YbaClient ybaClient;
    private final BackupDaoService backupDaoService;
    private final BatchExecutionDao batchExecutionDao;
    private final BackupValidationService validationService;

    public BackupService(YbaClient ybaClient, BackupDaoService backupDaoService,
                         BatchExecutionDao batchExecutionDao, BackupValidationService validationService) {
        this.ybaClient = ybaClient;
        this.backupDaoService = backupDaoService;
        this.batchExecutionDao = batchExecutionDao;
        this.validationService = validationService;
    }

    @Override
    public void process(Map<String, Object> batchParams) {
        try {
            validationService.validateBatchParams(batchParams);

            String categoryCode = (String) batchParams.get(AppConstants.CATEGORY_CODE);
            String batchId = (String) batchParams.get(AppConstants.BATCH_ID);
            String businessDate = extractBusinessDate(batchId, categoryCode);

            processBackup(batchId, businessDate, categoryCode)
                    .doOnError(e -> handleProcessingError(batchId, categoryCode, businessDate, e))
                    .subscribe();

        } catch (Exception e) {
            log.error("Failed to process backup request", e);
            throw e;
        }
    }

    public Mono<Void> processBackup(String batchId, String businessDate, String categoryCode) {
        Map<String, Object> batchParams = AppUtils.createBatchParams(batchId, businessDate, categoryCode);

        return ybaClient.backupInitiate(categoryCode, batchParams)
                .flatMap(ydbRes -> handleBackupSuccess(batchId, businessDate, ydbRes))
                .onErrorResume(e -> handleBackupFailure(batchId, businessDate, e))
                .then();
    }

    void handleProcessingError(String batchId, String categoryCode, String businessDate, Throwable e) {
        Map<String, Object> extensionField = new HashMap<>();
        String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
        extensionField.put(AppConstants.ERROR_MESSAGE, errorMessage);

        try {
            batchExecutionDao.insertExceptionDetails(batchId, categoryCode, e.getMessage(), extensionField, businessDate);
            batchExecutionDao.updateBatchStatus(batchId, AppConstants.BATCH_FAILED_STATUS, extensionField, businessDate);
            log.error("Technical Error occurred while processing backup for category: {}", categoryCode, e);
        } catch (Exception dbException) {
            log.error("Failed to update database with error details for batch: {}", batchId, dbException);
        }
    }

    private Mono<Void> handleBackupSuccess(String batchId, String businessDate, String ydbRes) {
        return Mono.fromRunnable(() -> {
            backupDaoService.updateBackupStatus(batchId, AppConstants.BACKUP_SUCCESS_STATUS,
                    AppUtils.toDate(businessDate), ydbRes);
            batchExecutionDao.updateBatchStatus(batchId, AppConstants.BATCH_COMPLETED_STATUS,
                    new HashMap<>(), businessDate);
            log.info("Backup completed successfully for batch: {}", batchId);
        });
    }

    private Mono<Void> handleBackupFailure(String batchId, String businessDate, Throwable e) {
        return Mono.fromRunnable(() -> {
            log.error("Backup failed for batch: {}", batchId, e);
            backupDaoService.updateBackupStatus(batchId, AppConstants.BACKUP_FAILED_STATUS,
                    AppUtils.toDate(businessDate), e.getMessage());
            batchExecutionDao.updateBatchStatus(batchId, AppConstants.BATCH_FAILED_STATUS,
                    new HashMap<>(), businessDate);
        });
    }

    String extractBusinessDate(String batchId, String categoryCode) {
        String date = batchExecutionDao.getBatchDetails(batchId, categoryCode)
                .getId().getBatchExecutionDate();
        return AppUtils.getBusinessDate(date);
    }
}
