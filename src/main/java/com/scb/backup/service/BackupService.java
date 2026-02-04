package com.scb.backup.service;

import com.scb.backup.client.YbaClient;
import com.scb.backup.dao.BackupDaoService;
import com.scb.backup.utils.AppConstants;
import com.scb.backup.utils.AppUtils;
import com.hdfcbank.epricing.batch.core.lib.dao.BatchExecutionDao;
import com.hdfcbank.epricing.batch.core.lib.service.GenericBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * BackupService - Core service for orchestrating YugabyteDB backup operations.
 *
 * This service extends the generic batch framework to provide specialized backup
 * functionality for YugabyteDB databases. It coordinates the entire backup workflow
 * including validation, execution, status tracking, and error handling.
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *   <li>Validate batch parameters before backup execution</li>
 *   <li>Orchestrate full and incremental backup operations via YBA API</li>
 *   <li>Track backup status in the database (IN_PROGRESS, SUCCESS, FAILED)</li>
 *   <li>Handle backup success and failure scenarios</li>
 *   <li>Update batch execution status in the framework</li>
 *   <li>Log exception details for troubleshooting</li>
 * </ul>
 *
 * <p><b>Workflow:</b></p>
 * <pre>
 * 1. Validate batch parameters
 * 2. Extract business date from batch ID
 * 3. Initiate backup via YbaClient
 * 4. Handle success: Update status to SUCCESS
 * 5. Handle failure: Update status to FAILED, log exception
 * </pre>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 * @see GenericBatchService
 * @see YbaClient
 * @see BackupDaoService
 */
@Slf4j
@Service
public class BackupService extends GenericBatchService {

    private final YbaClient ybaClient;
    private final BackupDaoService backupDaoService;
    private final BatchExecutionDao batchExecutionDao;
    private final BackupValidationService validationService;

    /**
     * Constructor for BackupService with dependency injection.
     *
     * @param ybaClient Client for YBA API integration
     * @param backupDaoService DAO for backup status persistence
     * @param batchExecutionDao DAO for batch framework integration
     * @param validationService Service for parameter validation
     */
    public BackupService(YbaClient ybaClient, BackupDaoService backupDaoService,
                         BatchExecutionDao batchExecutionDao, BackupValidationService validationService) {
        this.ybaClient = ybaClient;
        this.backupDaoService = backupDaoService;
        this.batchExecutionDao = batchExecutionDao;
        this.validationService = validationService;
    }

    /**
     * Processes a backup request from the batch framework.
     *
     * This method is the entry point from the generic batch framework. It validates
     * the batch parameters, extracts necessary information, and triggers the reactive
     * backup workflow.
     *
     * @param batchParams Map containing batch parameters (batchId, categoryCode, businessDate)
     * @throws IllegalArgumentException if batch parameters are invalid
     */
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

    /**
     * Executes the reactive backup workflow.
     *
     * This method orchestrates the backup operation using reactive programming with
     * Project Reactor. It initiates the backup via YBA API and handles both success
     * and failure scenarios asynchronously.
     *
     * @param batchId Unique identifier for the batch execution
     * @param businessDate Business date for the backup operation (YYYYMMDD format)
     * @param categoryCode Backup category code (e.g., HWA_EPR_DB_BACKUP_FULL)
     * @return Mono&lt;Void&gt; representing the asynchronous backup operation
     */
    public Mono<Void> processBackup(String batchId, String businessDate, String categoryCode) {
        Map<String, Object> batchParams = AppUtils.createBatchParams(batchId, businessDate, categoryCode);

        return ybaClient.backupInitiate(categoryCode, batchParams)
                .flatMap(ydbRes -> handleBackupSuccess(batchId, businessDate, ydbRes))
                .onErrorResume(e -> handleBackupFailure(batchId, businessDate, e))
                .then();
    }

    /**
     * Handles errors that occur during backup processing.
     *
     * This method logs exception details to the database and updates the batch
     * status to FAILED. It ensures that errors are properly tracked for
     * troubleshooting and monitoring purposes.
     *
     * @param batchId Unique identifier for the batch execution
     * @param categoryCode Backup category code
     * @param businessDate Business date for the backup operation
     * @param e Throwable representing the error that occurred
     */
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

    /**
     * Handles successful backup completion.
     *
     * Updates the backup status to SUCCESS in the database and updates the batch
     * execution status to COMPLETED in the batch framework.
     *
     * @param batchId Unique identifier for the batch execution
     * @param businessDate Business date for the backup operation
     * @param ydbRes Response from YBA API containing backup details
     * @return Mono&lt;Void&gt; representing the asynchronous status update operation
     */
    private Mono<Void> handleBackupSuccess(String batchId, String businessDate, String ydbRes) {
        return Mono.fromRunnable(() -> {
            backupDaoService.updateBackupStatus(batchId, AppConstants.BACKUP_SUCCESS_STATUS,
                    AppUtils.toDate(businessDate), ydbRes);
            batchExecutionDao.updateBatchStatus(batchId, AppConstants.BATCH_COMPLETED_STATUS,
                    new HashMap<>(), businessDate);
            log.info("Backup completed successfully for batch: {}", batchId);
        });
    }

    /**
     * Handles backup failure scenarios.
     *
     * Updates the backup status to FAILED in the database and updates the batch
     * execution status to FAILED in the batch framework. Logs the error for
     * troubleshooting.
     *
     * @param batchId Unique identifier for the batch execution
     * @param businessDate Business date for the backup operation
     * @param e Throwable representing the error that caused the failure
     * @return Mono&lt;Void&gt; representing the asynchronous status update operation
     */
    private Mono<Void> handleBackupFailure(String batchId, String businessDate, Throwable e) {
        return Mono.fromRunnable(() -> {
            log.error("Backup failed for batch: {}", batchId, e);
            backupDaoService.updateBackupStatus(batchId, AppConstants.BACKUP_FAILED_STATUS,
                    AppUtils.toDate(businessDate), e.getMessage());
            batchExecutionDao.updateBatchStatus(batchId, AppConstants.BATCH_FAILED_STATUS,
                    new HashMap<>(), businessDate);
        });
    }

    /**
     * Extracts the business date from batch execution details.
     *
     * Retrieves the batch execution record from the database and extracts the
     * business date in the required format.
     *
     * @param batchId Unique identifier for the batch execution
     * @param categoryCode Backup category code
     * @return String representing the business date in YYYYMMDD format
     */
    String extractBusinessDate(String batchId, String categoryCode) {
        String date = batchExecutionDao.getBatchDetails(batchId, categoryCode)
                .getId().getBatchExecutionDate();
        return AppUtils.getBusinessDate(date);
    }
}
