package com.scb.backup;

import com.scb.backup.exception.DbBackupException;
import com.hdfcbank.epricing.batch.core.lib.model.BatchStartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import com.scb.backup.service.BackupService;

/**
 * BackupController - REST API controller for database backup operations.
 *
 * This controller provides HTTP endpoints for triggering YugabyteDB backup processes
 * through the batch processing framework. It handles backup requests asynchronously
 * using reactive programming with Project Reactor.
 *
 * <p>The controller integrates with the batch framework to orchestrate full and
 * incremental backups, track backup status, and handle errors gracefully.</p>
 *
 * <p><b>Endpoints:</b></p>
 * <ul>
 *   <li>POST /backupProcess - Initiates a backup operation</li>
 * </ul>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 * @see BackupService
 * @see BatchStartResponse
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class BackupController {

    @Autowired
    private BackupService backupService;

    /**
     * Initiates a database backup process.
     *
     * This endpoint receives a JSON payload containing batch parameters and triggers
     * the backup workflow. The operation is executed asynchronously and returns a
     * reactive Mono containing the batch execution response.
     *
     * <p><b>Request Body Example:</b></p>
     * <pre>
     * {
     *   "batchCategoryCode": "HWA_EPR_DB_BACKUP_FULL",
     *   "batchTransactionDate": "20260204"
     * }
     * </pre>
     *
     * <p><b>Response Example:</b></p>
     * <pre>
     * {
     *   "batchId": "BATCH_123",
     *   "executionStatus": "SUCCESS",
     *   "extensionFields": {}
     * }
     * </pre>
     *
     * @param json JSON string containing batch parameters (batchCategoryCode, batchTransactionDate)
     * @return Mono&lt;BatchStartResponse&gt; containing the batch execution result
     * @throws DbBackupException if backup process fails due to technical errors
     */
    @PostMapping("/backupProcess")
    public Mono<BatchStartResponse> backupProcess(@RequestBody String json){
        log.info("backup request received for db backup for job Type-{}",json);
        return Mono.fromCallable(()->{
            try {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                var response = backupService.execute(json);
                stopWatch.stop();
                log.info("Completion time - {} sec");
                return response;
            } catch (Exception e) {
                log.error("Error occurred during file Transfer : ", e);
                throw new DbBackupException("Error occurred during backup process: ",e);

            }
        }) .doOnError(throwable -> log.error("Backup process failed - RequestID: {}", throwable));
    }

}
