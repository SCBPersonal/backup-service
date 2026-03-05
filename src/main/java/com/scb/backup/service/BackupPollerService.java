package com.scb.backup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scb.backup.config.BackupPollerProperties;
import com.scb.backup.dao.BackupDaoService;
import com.scb.backup.model.YbaDynamicConfig;
import com.scb.backup.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * BackupPollerService - Service for polling YBA backup job completion status.
 *
 * This service runs in parallel threads to monitor full backup job completion.
 * When a full backup is triggered, this poller checks the job status periodically
 * until it completes, then fetches the base UUID and updates the database.
 *
 * <p><b>Workflow:</b></p>
 * <pre>
 * 1. Full backup triggered → Record inserted with task UUID
 * 2. Poller starts in background thread
 * 3. Poll YBA task status API (GET) every N seconds
 * 4. When status = "Success":
 *    - Fetch last backup using POST API with pagination
 *    - Extract base UUID from paginated response
 *    - Update full_backup_tracker with base UUID
 * 5. When status = "Failure":
 *    - Update full_backup_tracker with error
 * </pre>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupPollerService {

    private final WebClient webClient;
    private final BackupDaoService backupDaoService;
    private final BackupPollerProperties pollerProperties;
    private final ObjectMapper mapper;

    /**
     * Starts polling for FULL backup job completion reactively using Mono.
     *
     * This method polls the YBA task status API until the job completes (no attempt limit).
     * It runs in the same reactive thread chain and returns a Mono that completes when polling finishes.
     * Polling will continue indefinitely until the backup job reaches a terminal state.
     *
     * @param config YBA dynamic configuration
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @param taskUuid YBA task UUID to poll
     * @return Mono<String> containing the base UUID on success, or error on failure
     */
    public Mono<String> pollFullBackupCompletion(YbaDynamicConfig config, String categoryCode, String backupMonth,
                                                 String taskUuid, String batchId) {
        if (!pollerProperties.isEnabled()) {
            log.info("Backup poller is disabled. Skipping polling for task: {}", taskUuid);
            return Mono.error(new RuntimeException("Backup poller is disabled"));
        }

        log.info("Starting reactive backup poller for FULL backup - category: {}, task: {}", categoryCode, taskUuid);

        return Mono.delay(Duration.ofMillis(pollerProperties.getInitialDelayMs()))
                .then(pollJobStatusWithRetry(config, taskUuid))
                .flatMap(jobStatus -> {
                    if (AppConstants.BACKUP_POLLING_STATUS.equalsIgnoreCase(jobStatus)) {
                        log.info("Full backup job completed successfully for task: {}", taskUuid);
                        return handleFullBackupSuccess(config, categoryCode, backupMonth, taskUuid,batchId);
                    } else {
                        log.error("Full backup job failed for task: {}, status: {}", taskUuid, jobStatus);
                        return handleFullBackupFailure(categoryCode, backupMonth, "Job failed with status: " + jobStatus,batchId);
                    }
                })
                .doOnError(error -> {
                    log.error("Error during full backup polling for task: {}", taskUuid, error);
                    backupDaoService.updateFullBackupWithBaseUuid(categoryCode, backupMonth,
                             "",batchId,AppConstants.BACKUP_FAILED_STATUS);
                });
    }

    /**
     * Starts polling for INCREMENTAL backup job completion reactively using Mono.
     *
     * This method polls the YBA task status API until the job completes (no attempt limit).
     * Polling will continue indefinitely until the backup job reaches a terminal state.
     *
     * @param config YBA dynamic configuration
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @param taskUuid YBA task UUID to poll

     * @param baseUuid Base UUID of the full backup
     * @return Mono<Void> that completes when polling finishes
     */
    public Mono<Void> pollIncrementalBackupCompletion(YbaDynamicConfig config, String categoryCode, String backupMonth,
                                                       String taskUuid,String batchId, String baseUuid,String response) {
        if (!pollerProperties.isEnabled()) {
            log.info("Backup poller is disabled. Skipping polling for task: {}", taskUuid);
            return Mono.error(new RuntimeException("Backup poller is disabled"));
        }

        log.info("Starting reactive backup poller for INCREMENTAL backup - category: {}, task: {}", categoryCode, taskUuid);

        return Mono.delay(Duration.ofMillis(pollerProperties.getInitialDelayMs()))
                .then(pollJobStatusWithRetry(config, taskUuid))
                .flatMap(jobStatus -> {
                    if (AppConstants.BACKUP_POLLING_STATUS.equalsIgnoreCase(jobStatus)) {
                        log.info("Incremental backup job completed successfully for task: {}", taskUuid);
                        return handleIncrementalBackupSuccess(categoryCode, backupMonth, baseUuid,batchId,response,taskUuid);
                    } else {
                        log.error("Incremental backup job failed for task: {}, status: {}", taskUuid, jobStatus);
                        return handleIncrementalBackupFailure(categoryCode, backupMonth, baseUuid ,batchId,response,taskUuid);
                    }
                })
                .doOnError(error -> {
                    log.error("Error during incremental backup polling for task: {}", taskUuid, error);
                    backupDaoService.updateIncrementalBackupStatusByMonth(categoryCode, backupMonth, baseUuid,
                            AppConstants.BACKUP_FAILED_STATUS,batchId,"", taskUuid);
                });
    }

    /**
     * Polls job status with retry logic until job completes (infinite retry).
     *
     * This method will continue polling indefinitely until the backup job completes.
     * There is no maximum attempt limit - it will keep polling until success, failure, or abort.
     *
     * @param config YBA configuration
     * @param taskUuid Task UUID to check
     * @return Mono of final job status string (e.g., "Success", "Failure", "Aborted")
     */
    Mono<String> pollJobStatusWithRetry(YbaDynamicConfig config, String taskUuid) {
        return checkJobStatus(config, taskUuid)
                .flatMap(status -> {
                    if (AppConstants.BACKUP_POLLING_STATUS.equalsIgnoreCase(status) ||
                        "Failure".equalsIgnoreCase(status) ||
                        "Aborted".equalsIgnoreCase(status)) {
                        // Job completed
                        return Mono.just(status);
                    } else {
                        // Job still running, throw error to trigger retry
                        return Mono.error(new RuntimeException("Backup still in progress: " + status));
                    }
                })
                .retryWhen(Retry.fixedDelay(Long.MAX_VALUE,
                        Duration.ofMillis(pollerProperties.getPollingIntervalMs()))
                        .filter(throwable -> throwable.getMessage().contains("Backup still in progress"))
                        .doBeforeRetry(retrySignal ->
                            log.info("Polling attempt {} for task: {} - Backup still in progress, retrying...",
                                retrySignal.totalRetries() + 1,
                                taskUuid))
                );
    }

    /**
     * Checks the job status from YBA API reactively.
     *
     * @param config YBA configuration (contains job-completion-check-url from database config)
     * @param taskUuid Task UUID to check
     * @return Mono of job status string (e.g., "Success", "Failure", "Running")
     */
    private Mono<String> checkJobStatus(YbaDynamicConfig config, String taskUuid) {
        // Get job completion check URL from database-specific config
        String url = config.getJobCompletionCheckUrl()
                .replace("{taskUuid}", taskUuid);

        return webClient.get()
                .uri(url)
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON)
                .header(AppConstants.X_AUTH_YW_API_TOKEN, config.getApiToken())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.path(AppConstants.STATUS).asText(AppConstants.UNKNOWN_STATUS))
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(e -> {
                    log.error("Error checking job status for task: {}", taskUuid, e);
                    return Mono.just(AppConstants.UNKNOWN_STATUS);
                });
    }

    /**
     * Handles successful FULL backup completion.
     * Fetches the last backup to get base UUID and task UUID, validates they match,
     * then updates the full_backup_tracker table.
     *
     * @param config YBA dynamic configuration
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @param expectedTaskUuid Task UUID from the full backup response (the one we're polling)
     * @return Mono<String> containing the base UUID
     */
    private Mono<String> handleFullBackupSuccess(YbaDynamicConfig config, String categoryCode, String backupMonth, String expectedTaskUuid,String batchId) {
        return fetchLastBackupDetailsReactive(config)
                .flatMap(backupDetails -> {
                    String baseUuid = backupDetails.get(AppConstants.BASEUUID);
                    String fetchedTaskUuid = backupDetails.get(AppConstants.TASKUUID);

                    // Validate that task UUIDs match
                    if (fetchedTaskUuid == null || !fetchedTaskUuid.equals(expectedTaskUuid)) {
                        String errorMsg = String.format(
                                "Task UUID mismatch! Expected: %s, but last backup has: %s. This indicates a different backup was created.",
                                expectedTaskUuid, fetchedTaskUuid);
                        log.error(errorMsg);
                        backupDaoService.updateFullBackupWithBaseUuid(categoryCode, backupMonth,"",batchId,AppConstants.BACKUP_FAILED_STATUS);
                        return Mono.error(new RuntimeException(errorMsg));
                    }

                    log.info("Task UUID validation successful. Expected: {}, Fetched: {}", expectedTaskUuid, fetchedTaskUuid);

                    if (baseUuid != null && !baseUuid.isEmpty()) {
                        // Update full_backup_tracker with base UUID and SUCCESS status
                        backupDaoService.updateFullBackupWithBaseUuid(categoryCode, backupMonth, baseUuid,batchId,AppConstants.BACKUP_SUCCESS_STATUS);
                        log.info("Successfully updated full backup with base UUID: {} for category: {}", baseUuid, categoryCode);
                        return Mono.just(baseUuid);
                    } else {
                        log.error("Failed to fetch base UUID from last backup for category: {}", categoryCode);
                        backupDaoService.updateFullBackupWithBaseUuid(categoryCode, backupMonth,"",
                                batchId,AppConstants.BATCH_FAILED_STATUS);
                        return Mono.error(new RuntimeException("Failed to fetch base UUID from last backup"));
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error handling full backup success for category: {}", categoryCode, error);
                    backupDaoService.updateFullBackupWithBaseUuid(categoryCode, backupMonth, "",
                            batchId,AppConstants.BATCH_FAILED_STATUS);
                    return Mono.error(error);
                });
    }

    /**
     * Handles FULL backup failure.
     * Updates the full_backup_tracker table with failure status.
     *
     * @return Mono<String> that errors with the failure message
     */
    private Mono<String> handleFullBackupFailure(String categoryCode, String backupMonth, String errorMessage,String batchId) {
        backupDaoService.updateFullBackupWithBaseUuid(categoryCode, backupMonth,"",batchId,AppConstants.BACKUP_FAILED_STATUS);
        log.info("Updated full backup status to FAILED for category: {}", categoryCode);
        return Mono.error(new RuntimeException(errorMessage));
    }

    /**
     * Handles successful INCREMENTAL backup completion.
     * Updates the incremental_backup_tracker table with SUCCESS status.
     *
     * @return Mono<Void> that completes when update is done
     */
    private Mono<Void> handleIncrementalBackupSuccess(String categoryCode, String backupMonth, String baseUuid, String batchId, String response, String taskUuid) {
        return Mono.fromRunnable(() -> {
            backupDaoService.updateIncrementalBackupStatusByMonth(categoryCode, backupMonth, baseUuid,
                    AppConstants.BACKUP_SUCCESS_STATUS,batchId,response,taskUuid);
            log.info("Successfully updated incremental backup status to SUCCESS for category: {}, baseUuid: {}",
                    categoryCode, baseUuid);
        });
    }

    /**
     * Handles INCREMENTAL backup failure.
     * Updates the incremental_backup_tracker table with failure status.
     *
     * @return Mono<Void> that errors with the failure message
     */
    private Mono<Void> handleIncrementalBackupFailure(String categoryCode, String backupMonth, String baseUuid, String errorMessage, String batchId, String taskUuid) {
        backupDaoService.updateIncrementalBackupStatusByMonth(categoryCode, backupMonth, baseUuid,
                AppConstants.BACKUP_FAILED_STATUS,batchId,"",taskUuid);
        log.info("Updated incremental backup status to FAILED for category: {}", categoryCode);
        return Mono.error(new RuntimeException(errorMessage));
    }

    /**
     * Fetches the base backup UUID and task UUID from YBA's last backup API using POST method - REACTIVE VERSION.
     *
     * YBA requires POST method with pagination parameters to fetch the latest backup.
     * We request the most recent backup by sorting descending by createTime.
     *
     * @param config YBA configuration
     * @return Mono<Map<String, String>> containing both "baseUuid" and "taskUuid", or empty if not found
     */
    Mono<Map<String, String>> fetchLastBackupDetailsReactive(YbaDynamicConfig config) {
        // Create request body for pagination - get latest backup
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put(AppConstants.STORAGE_CONFIG_UUID, config.getStorageConfigUuid());
        requestBody.put(AppConstants.PAYLOAD_SSE, false);
        requestBody.put(AppConstants.BACKUPTYPE, config.getBackupType());
        requestBody.putPOJO(AppConstants.BACKUPCATEGORY, AppConstants.YB_CONTROLLER);
        requestBody.put(AppConstants.DIRECTION,AppConstants.DESC);
        requestBody.put(AppConstants.SORT_BY, AppConstants.CREATE_TIME);
        requestBody.put(AppConstants.TIME_BEFORE_DELETE, config.getExpiryMs());
        requestBody.put(AppConstants.EXPIRY_TIME_UNIT, AppConstants.PAYLOAD_MILLISECONDS);

        ObjectNode filter = requestBody.putObject(AppConstants.FILTER);
        ArrayNode uniList = filter.putArray(AppConstants.UNIVERSE_UUID_LIST);
        uniList.add(config.getUniverseUuid());
        requestBody.put(AppConstants.LIMIT,1);

        return webClient.post()
                .uri(config.getLastBackupUrl())
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON)
                .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON)
                .header(AppConstants.X_AUTH_YW_API_TOKEN, config.getApiToken())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(response -> {
                    java.util.Map<String, String> details = extractBackupDetailsFromLastBackup(response);
                    return Mono.justOrEmpty(details);
                })
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(error -> {
                    log.error("Error fetching last backup details", error);
                    return Mono.empty();
                });
    }

    /**
     * Extracts the backup UUID and task UUID from the paginated last backup API response.
     *
     * The POST API returns a paginated response
     * @param response JsonNode containing the paginated last backup API response
     * @return Map containing "baseUuid" and "taskUuid", or null if not found
     */
    Map<String, String> extractBackupDetailsFromLastBackup(JsonNode response) {
        if (response == null) {
            return null;
        }

        Map<String,String> details = new HashMap<>();
        String baseUuid = null;
        String taskUuid = null;

        // Check if response has entities array (paginated response)
        JsonNode entities = response.path(AppConstants.ENTITIES);
        if (entities.isArray() && entities.size() > 0) {
            JsonNode firstBackup = entities.get(0);
            // get basebackupUUID from first entity
            baseUuid = firstBackup.path("backupUUID").asText(null);
            if (baseUuid == null) {
                // get from commonBackupInfo.baseBackupUUID
                baseUuid = firstBackup.path(AppConstants.COMMON_BACKUP_INFO).path(AppConstants.BASE_BACKUP_UUID).asText(null);
            }
            // Try to get taskUUID from first entity
            taskUuid = firstBackup.path(AppConstants.TASKUUID).asText(null);
            if (taskUuid == null) {
                // Try commonBackupInfo.taskUUID
                taskUuid = firstBackup.path(AppConstants.COMMON_BACKUP_INFO).path(AppConstants.TASKUUID).asText(null);
            }
        }

        if (baseUuid != null || taskUuid != null) {
            details.put(AppConstants.BASEUUID, baseUuid);
            details.put(AppConstants.TASKUUID, taskUuid);
            log.info("Extracted backup details - Base UUID: {}, Task UUID: {}", baseUuid, taskUuid);
            return details;
        }

        log.warn("Failed to extract backup details from response");
        return null;
    }
}

