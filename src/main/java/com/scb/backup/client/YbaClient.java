package com.scb.backup.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scb.backup.dao.BackupDaoService;
import com.scb.backup.model.YbaDynamicConfig;
import com.scb.backup.service.BackupPollerService;
import com.scb.backup.service.YbaConfigService;
import com.scb.backup.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * YbaClient - HTTP client for YugabyteDB Anywhere (YBA) API integration.
 *
 * This client handles backup operations including full and incremental backups.
 * It manages base backup UUID storage and retrieval for monthly incremental backups.
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YbaClient {


    private final WebClient webClient;
    private final BackupDaoService backupDaoService;
    private final BackupPollerService backupPollerService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final YbaConfigService configService;


    /**
     * Initiates a backup operation based on the category code.
     * This method determines the backup type (FULL or INCREMENTAL) from the configuration
     * and delegates to the appropriate backup method. For full backups, it stores the
     * base backup UUID in the database. For incremental backups, it validates that a
     * base backup UUID exists for the current month before proceeding.
     * @param categoryCode The backup category code (e.g., "HWA_EPR_DB_BACKUP_FULL")
     * @param batchParams Batch parameters including batch ID and business date
     * @return Mono<String> containing the YBA API response as JSON string
     * @throws IllegalArgumentException if no configuration found for category or unsupported backup type
     * @throws IllegalStateException if incremental backup attempted without base UUID for current month
     */
    public Mono<String> backupInitiate(String categoryCode, Map<String, Object> batchParams) {
        return Mono.fromCallable(() -> configService.resolve(categoryCode))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No configuration found for category: " + categoryCode)))
                .flatMap(config -> {
                    String backupType = config.getBackupCategoryType();

                    if (AppConstants.FULL_BACKUP.equalsIgnoreCase(backupType)) {
                        return fullBackup(config, categoryCode, batchParams);
                    } else if (AppConstants.INCREMENTAL_BACKUP.equalsIgnoreCase(backupType)) {
                        return performIncrementalBackup(config, categoryCode, batchParams);
                    } else {
                        return Mono.error(new IllegalArgumentException("Unsupported backup type: " + backupType));
                    }
                })
                .map(JsonNode::toString)
                .doOnError(e -> log.error("Backup initiation failed for category: {}", categoryCode, e));
    }

    /**
     * Performs an incremental backup operation.
     *
     * This method retrieves the base backup UUID from the full_backup_tracker table for the current month.
     * If the base UUID is not found, it throws an IllegalStateException, requiring the user
     * to perform a full backup first for the current month.
     *
     * Flow:
     * 1. Fetch base UUID from full_backup_tracker for current month
     * 2. Insert record into incremental_backup_tracker with IN_PROGRESS status
     * 3. Call YBA API for incremental backup
     * 4. Update incremental_backup_tracker with response
     *
     * @param config YBA dynamic configuration for the backup
     * @param categoryCode The backup category code
     * @param batchParams Batch parameters including batch ID and business date
     * @return Mono<JsonNode> containing the YBA API response
     * @throws IllegalStateException if base backup UUID not found for current month
     */
    private Mono<JsonNode> performIncrementalBackup(YbaDynamicConfig config, String categoryCode, Map<String, Object> batchParams) {
        String currentMonth = getCurrentMonth();
        String batchId = (String) batchParams.get(AppConstants.BATCH_ID);
        Date businessDate = (Date) batchParams.get(AppConstants.BUSINESS_DATE);
        return Mono.fromCallable(() -> backupDaoService.getBaseBackupUuidFromDb(currentMonth,config.getDbName()))
                .flatMap(baseUuid ->  {
                    if (baseUuid != null && !baseUuid.isEmpty()) {
                        log.info("Using base backup UUID from full_backup_tracker: {} for category: {}, month: {}",
                                baseUuid, categoryCode, currentMonth);

                        // Call incremental backup API
                        return incrementalBackup(config, baseUuid)
                                .flatMap(response -> {
                                    // Extract task UUID  response
                                    String taskUuid = extractTaskUuidFromResponse(response);
                                    // Insert into incremental_backup_tracker with task UUID and response in one operation
                                    backupDaoService.insertIncrementalBackupRecord(
                                            batchId, categoryCode, businessDate, currentMonth, baseUuid, taskUuid, response.toString());
                                    log.info("Inserted incremental backup record with response for batch: {}, category: {}, task: {}",
                                            batchId, categoryCode, taskUuid);

                                    // Start reactive polling for incremental backup completion
                                    log.info("Starting reactive polling for incremental backup task: {}", taskUuid);
                                    return backupPollerService.pollIncrementalBackupCompletion(config, categoryCode, currentMonth,
                                            taskUuid,batchId, baseUuid)
                                            .thenReturn(response);
                                });
                    } else {
                        log.error("No base backup UUID found in full_backup_tracker for category: {}, month: {}. " +
                                "A full backup must be performed first for the current month before incremental backup can proceed.",
                                categoryCode, currentMonth);
                        return Mono.error(new IllegalStateException(
                                String.format("Base backup UUID not found for category '%s' and month '%s'. " +
                                        "Please perform a full backup first for the current month.",
                                        categoryCode, currentMonth)));
                    }
                });
    }

    /**
     * Gets the current month in YYYY-MM format.
     * This format is used as the key for storing and retrieving base backup UUIDs
     * on a monthly basis.
     * @return String representing current month in YYYY-MM format (e.g., "2026-02")
     */
    private String getCurrentMonth() {
        return YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    /**
     * Performs a full backup operation.
     *
     * This method initiates a full backup via the YBA API and manages the full_backup_tracker table.
     *
     * Flow:
     * 1. Insert record into full_backup_tracker with IN_PROGRESS status and task_uuid
     * 2. Call YBA API for full backup
     * 3. Update full_backup_tracker with full backup response
     * 4. BackupPollerService will poll for job completion and update with base UUID
     *
     * @param config YBA dynamic configuration for the backup
     * @param categoryCode The backup category code
     * @param batchParams Batch parameters including batch ID and business date
     * @return Mono<JsonNode> containing the YBA API response with backup details
     */
    private Mono<JsonNode> fullBackup(YbaDynamicConfig config, String categoryCode, Map<String, Object> batchParams)  {
        
        String currentMonth = getCurrentMonth();
        String batchId = (String) batchParams.get(AppConstants.BATCH_ID);
        Date businessDate = (Date) batchParams.get(AppConstants.BUSINESS_DATE);

        ObjectNode body = mapper.createObjectNode();
        body.put(AppConstants.STORAGE_CONFIG_UUID, config.getStorageConfigUuid());
        body.put(AppConstants.PAYLOAD_SSE, false);
        body.put(AppConstants.BACKUPTYPE, config.getBackupType());
        body.put(AppConstants.BACKUPCATEGORY, AppConstants.YB_CONTROLLER);
        body.put(AppConstants.UNIVERSE_UUID, config.getUniverseUuid());
        body.put(AppConstants.TIME_BEFORE_DELETE, config.getExpiryMs());
        body.put(AppConstants.EXPIRY_TIME_UNIT, AppConstants.PAYLOAD_MILLISECONDS);

        ArrayNode keyspaces = body.putArray(AppConstants.KEYSPACE_TABLE_LIST);
        ObjectNode tableNode = mapper.createObjectNode();
        tableNode.put(AppConstants.KEYSPACE, config.getDbName());
        keyspaces.add(tableNode);

        return webClient.post()
                .uri(config.getFullBackupUrl())
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON)
                .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON)
                .header(AppConstants.X_AUTH_YW_API_TOKEN, config.getApiToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(response -> {
                    // Extract task UUID from response
                    String taskUuid = extractTaskUuidFromResponse(response);

                    // Insert into full_backup_tracker with task UUID and response in one operation
                    backupDaoService.insertFullBackupRecord(
                            batchParams ,currentMonth, taskUuid, response.toString(),config.getDbName());
                    log.info("Inserted full backup record with response for batch: {}, category: {}, task: {}",
                            batchId, categoryCode, taskUuid);

                    // Start reactive polling for job completion - stays in same Mono chain
                    log.info("Starting reactive polling for full backup task: {}", taskUuid);
                    return backupPollerService.pollFullBackupCompletion(config, categoryCode, currentMonth,
                            taskUuid,batchId)
                            .map(baseUuid -> {
                                log.info("Full backup polling completed successfully with base UUID: {}", baseUuid);
                                return response;
                            });
                });
    }

    /**
     * Extracts the task UUID from YBA API response.
     *
     * The task UUID is used for polling the job status. This method tries multiple field names
     * in order of preference: taskUUID, resourceUUID.
     *
     * @param response JsonNode containing the YBA API response
     * @return String containing the extracted task UUID, or null if not found
     */
    private String extractTaskUuidFromResponse(JsonNode response) {
        if (response == null) {
            return null;
        }

        // Try to get taskUUID first (most common for async operations)
        String uuid = response.path(AppConstants.TASKUUID).asText(null);
        if (uuid != null) {
            return uuid;
        }

        // Try resourceUUID as fallback
        uuid = response.path("resourceUUID").asText(null);
        return uuid;
    }

    /**
     * Performs an incremental backup operation using a base backup UUID.
     *
     * This method initiates an incremental backup via the YBA API using the provided
     * base backup UUID. The base UUID should be retrieved from the database and
     * represents the full backup from which this incremental backup will be based.
     *
     * @param config YBA dynamic configuration for the backup
     * @param baseBackupUuid The UUID of the base backup (from full backup)
     * @return Mono<JsonNode> containing the YBA API response with backup task details
     */
    private Mono<JsonNode> incrementalBackup(YbaDynamicConfig config, String baseBackupUuid) {
        ObjectNode body = mapper.createObjectNode();
        body.put(AppConstants.STORAGE_CONFIG_UUID, config.getStorageConfigUuid());
        body.put(AppConstants.PAYLOAD_SSE, false);
        body.put(AppConstants.BACKUPTYPE, config.getBackupType());
        body.put(AppConstants.BACKUPCATEGORY, AppConstants.YB_CONTROLLER);
        body.put(AppConstants.UNIVERSE_UUID, config.getUniverseUuid());
        body.put(AppConstants.BASE_BACKUP_UUID, baseBackupUuid);
       // body.put(AppConstants.EXPIRY_TIME_UNIT, AppConstants.PAYLOAD_MILLISECONDS);

        ArrayNode arr = body.putArray(AppConstants.KEYSPACE_TABLE_LIST);
        ObjectNode db = mapper.createObjectNode();
        db.put(AppConstants.KEYSPACE, config.getDbName());
        arr.add(db);

        return webClient.post()
                .uri(config.getIncrementalBackupUrl())
                .header(AppConstants.ACCEPT, AppConstants.APPLICATION_JSON)
                .header(AppConstants.CONTENT_TYPE, AppConstants.APPLICATION_JSON)
                .header(AppConstants.X_AUTH_YW_API_TOKEN, config.getApiToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }
}
