package com.scb.backup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.scb.backup.config.BackupPollerProperties;
import com.scb.backup.dao.BackupDaoService;
import com.scb.backup.model.YbaDynamicConfig;
import com.scb.backup.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

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
 * 3. Poll YBA task status API every N seconds
 * 4. When status = "Success":
 *    - Fetch last backup to get base UUID
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

    /**
     * Starts polling for backup job completion in a background thread.
     *
     * This method is called asynchronously after a full backup is triggered.
     * It polls the YBA task status API until the job completes or max attempts reached.
     *
     * @param config YBA dynamic configuration
     * @param categoryCode Backup category code
     * @param backupMonth Month in YYYY-MM format
     * @param taskUuid YBA task UUID to poll
     * @param customerUuid YBA customer UUID
     */
    @Async
    public void startPolling(YbaDynamicConfig config, String categoryCode, String backupMonth,
                            String taskUuid, String customerUuid) {
        if (!pollerProperties.isEnabled()) {
            log.info("Backup poller is disabled. Skipping polling for task: {}", taskUuid);
            return;
        }

        log.info("Starting backup poller for category: {}, task: {}", categoryCode, taskUuid);

        try {
            // Initial delay before first poll
            Thread.sleep(pollerProperties.getInitialDelayMs());

            int attempts = 0;
            boolean jobCompleted = false;

            while (attempts < pollerProperties.getMaxPollAttempts() && !jobCompleted) {
                attempts++;
                log.debug("Polling attempt {}/{} for task: {}", attempts, pollerProperties.getMaxPollAttempts(), taskUuid);

                try {
                    // Check job status
                    String jobStatus = checkJobStatus(config, taskUuid, customerUuid);

                    if ("Success".equalsIgnoreCase(jobStatus)) {
                        log.info("Backup job completed successfully for task: {}", taskUuid);
                        handleJobSuccess(config, categoryCode, backupMonth);
                        jobCompleted = true;
                    } else if ("Failure".equalsIgnoreCase(jobStatus) || "Aborted".equalsIgnoreCase(jobStatus)) {
                        log.error("Backup job failed for task: {}, status: {}", taskUuid, jobStatus);
                        handleJobFailure(categoryCode, backupMonth, "Job failed with status: " + jobStatus);
                        jobCompleted = true;
                    } else {
                        log.debug("Backup job still in progress for task: {}, status: {}", taskUuid, jobStatus);
                    }

                    if (!jobCompleted) {
                        Thread.sleep(pollerProperties.getPollingIntervalMs());
                    }

                } catch (Exception e) {
                    log.error("Error polling job status for task: {}, attempt: {}", taskUuid, attempts, e);
                    if (attempts >= pollerProperties.getMaxPollAttempts()) {
                        handleJobFailure(categoryCode, backupMonth, "Polling failed after " + attempts + " attempts: " + e.getMessage());
                    }
                }
            }

            if (!jobCompleted) {
                log.error("Max polling attempts reached for task: {}. Marking as failed.", taskUuid);
                handleJobFailure(categoryCode, backupMonth, "Max polling attempts (" + pollerProperties.getMaxPollAttempts() + ") reached");
            }

        } catch (InterruptedException e) {
            log.error("Polling interrupted for task: {}", taskUuid, e);
            Thread.currentThread().interrupt();
            handleJobFailure(categoryCode, backupMonth, "Polling interrupted: " + e.getMessage());
        }
    }

    /**
     * Checks the job status from YBA API.
     *
     * @param config YBA configuration
     * @param taskUuid Task UUID to check
     * @param customerUuid Customer UUID
     * @return Job status string (e.g., "Success", "Failure", "Running")
     */
    private String checkJobStatus(YbaDynamicConfig config, String taskUuid, String customerUuid) {
        String url = pollerProperties.getJobCompletionCheckUrl()
                .replace("{customerUuid}", customerUuid)
                .replace("{taskUuid}", taskUuid);

        return webClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .header("X-AUTH-YW-API-TOKEN", config.getApiToken())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.path("status").asText("Unknown"))
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    /**
     * Handles successful job completion.
     * Fetches the last backup to get base UUID and updates the database.
     */
    private void handleJobSuccess(YbaDynamicConfig config, String categoryCode, String backupMonth) {
        try {
            // Fetch last backup to get base UUID
            String baseUuid = fetchLastBackupUuid(config);

            if (baseUuid != null && !baseUuid.isEmpty()) {
                backupDaoService.updateFullBackupWithBaseUuid(categoryCode, backupMonth, baseUuid);
                log.info("Successfully updated full backup with base UUID: {} for category: {}", baseUuid, categoryCode);
            } else {
                log.error("Failed to fetch base UUID from last backup for category: {}", categoryCode);
                backupDaoService.updateFullBackupStatus(categoryCode, backupMonth, AppConstants.BACKUP_FAILED_STATUS,
                        "Failed to fetch base UUID from last backup");
            }
        } catch (Exception e) {
            log.error("Error handling job success for category: {}", categoryCode, e);
            backupDaoService.updateFullBackupStatus(categoryCode, backupMonth, AppConstants.BACKUP_FAILED_STATUS,
                    "Error fetching base UUID: " + e.getMessage());
        }
    }

    /**
     * Handles job failure.
     * Updates the database with failure status and error message.
     */
    private void handleJobFailure(String categoryCode, String backupMonth, String errorMessage) {
        try {
            backupDaoService.updateFullBackupStatus(categoryCode, backupMonth, AppConstants.BACKUP_FAILED_STATUS, errorMessage);
            log.info("Updated full backup status to FAILED for category: {}", categoryCode);
        } catch (Exception e) {
            log.error("Error updating full backup failure status for category: {}", categoryCode, e);
        }
    }

    /**
     * Fetches the base backup UUID from YBA's last backup API.
     *
     * @param config YBA configuration
     * @return Base backup UUID or null if not found
     */
    private String fetchLastBackupUuid(YbaDynamicConfig config) {
        try {
            return webClient.get()
                    .uri(config.getLastBackupUrl())
                    .header("Accept", "application/json")
                    .header("X-AUTH-YW-API-TOKEN", config.getApiToken())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(this::extractBackupUuidFromLastBackup)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            log.error("Error fetching last backup UUID", e);
            return null;
        }
    }

    /**
     * Extracts the backup UUID from the last backup API response.
     *
     * @param response JsonNode containing the last backup API response
     * @return String containing the extracted backup UUID, or null if not found
     */
    private String extractBackupUuidFromLastBackup(JsonNode response) {
        if (response == null) {
            return null;
        }

        // Try to get backupUUID from the response
        String uuid = response.path("backupUUID").asText(null);
        if (uuid != null) {
            return uuid;
        }

        // Try commonBackupInfo.baseBackupUUID
        uuid = response.path("commonBackupInfo").path("baseBackupUUID").asText(null);
        if (uuid != null) {
            return uuid;
        }

        // Try resourceUUID as fallback
        uuid = response.path("resourceUUID").asText(null);
        if (uuid != null) {
            return uuid;
        }

        // Try taskUUID as last resort
        uuid = response.path("taskUUID").asText(null);
        return uuid;
    }
}

