package com.scb.backup.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scb.backup.dao.BackupDaoService;
import com.scb.backup.model.YbaDynamicConfig;
import com.scb.backup.service.YbaConfigService;
import com.scb.backup.utils.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.scb.backup.config.YbaProperties;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
public class YbaClient {

    @Autowired
    private  WebClient webClient;

    @Autowired
    BackupDaoService backupDaoService;

    @Autowired
    private YbaProperties props;

    private final ObjectMapper mapper = new ObjectMapper();
    private final YbaConfigService configService;

    /**
     * Constructor for YbaClient.
     *
     * @param webClient WebClient instance for making HTTP requests
     * @param props YBA properties configuration
     * @param configService Service for resolving YBA dynamic configurations
     */
    public YbaClient(WebClient webClient, YbaProperties props, YbaConfigService configService) {
        this.webClient = webClient;
        this.props = props;
        this.configService = configService;
    }

    /**
     * Initiates a backup operation based on the category code.
     *
     * This method determines the backup type (FULL or INCREMENTAL) from the configuration
     * and delegates to the appropriate backup method. For full backups, it stores the
     * base backup UUID in the database. For incremental backups, it validates that a
     * base backup UUID exists for the current month before proceeding.
     *
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
                    backupDaoService.insertBackupDetails(batchParams, AppConstants.BACKUP_INPROGRESS_STATUS, backupType);

                    if (AppConstants.FULL_BACKUP.equalsIgnoreCase(backupType)) {
                        return fullBackup(config, categoryCode);
                    } else if (AppConstants.INCREMENTAL_BACKUP.equalsIgnoreCase(backupType)) {
                        return performIncrementalBackup(config, categoryCode);
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
     * This method retrieves the base backup UUID from the database for the current month.
     * If the base UUID is not found, it throws an IllegalStateException, requiring the user
     * to perform a full backup first for the current month.
     *
     * @param config YBA dynamic configuration for the backup
     * @param categoryCode The backup category code
     * @return Mono<JsonNode> containing the YBA API response
     * @throws IllegalStateException if base backup UUID not found for current month
     */
    private Mono<JsonNode> performIncrementalBackup(YbaDynamicConfig config, String categoryCode) {
        String currentMonth = getCurrentMonth();

        return Mono.fromCallable(() -> backupDaoService.getBaseBackupUuidFromDb(categoryCode, currentMonth))
                .flatMap(baseUuid -> {
                    if (baseUuid != null && !baseUuid.isEmpty()) {
                        log.info("Using base backup UUID from database: {} for category: {}, month: {}",
                                baseUuid, categoryCode, currentMonth);
                        return incrementalBackup(config, baseUuid);
                    } else {
                        log.error("No base backup UUID found in database for category: {}, month: {}. " +
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
     *
     * This format is used as the key for storing and retrieving base backup UUIDs
     * on a monthly basis.
     *
     * @return String representing current month in YYYY-MM format (e.g., "2026-02")
     */
    private String getCurrentMonth() {
        return YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    /**
     * Performs a full backup operation.
     *
     * This method initiates a full backup via the YBA API and stores the resulting
     * base backup UUID in the database for the current month. The stored UUID will
     * be used for subsequent incremental backups in the same month.
     *
     * @param config YBA dynamic configuration for the backup
     * @param categoryCode The backup category code
     * @return Mono<JsonNode> containing the YBA API response with backup details
     */
    private Mono<JsonNode> fullBackup(YbaDynamicConfig config, String categoryCode)  {

        ObjectNode body = mapper.createObjectNode();
        body.put("storageConfigUUID", config.getStorageConfigUuid());
        body.put("sse", false);
        body.put("backupType", config.getBackupType());
        body.put("backupCategory", "YB_CONTROLLER");
        body.put("universeUUID", config.getUniverseUuid());
        body.put("timeBeforeDelete", config.getExpiryMs());
        body.put("expiryTimeUnit", "MILLISECONDS");

        ArrayNode keyspaces = body.putArray("keyspaceTableList");
        ObjectNode tableNode = mapper.createObjectNode();
        tableNode.put("keyspace", config.getDbName());
        keyspaces.add(tableNode);

        String currentMonth = getCurrentMonth();

        return webClient.post()
                .uri(config.getFullBackupUrl())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-AUTH-YW-API-TOKEN", config.getApiToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnSuccess(response -> {
                    // Extract and store base backup UUID
                    String backupUuid = extractBackupUuidFromResponse(response);
                    if (backupUuid != null && !backupUuid.isEmpty()) {
                        try {
                            backupDaoService.storeBaseBackupUuid(categoryCode, backupUuid, currentMonth);
                            log.info("Stored base backup UUID: {} for category: {}, month: {}",
                                    backupUuid, categoryCode, currentMonth);
                        } catch (Exception e) {
                            log.error("Failed to store base backup UUID for category: {}", categoryCode, e);
                        }
                    } else {
                        log.warn("No backup UUID found in response for category: {}", categoryCode);
                    }
                });
    }

    /**
     * Extracts the backup UUID from YBA API response.
     *
     * The YBA API response may contain the backup UUID in different fields depending
     * on the API version and operation type. This method tries multiple field names
     * in order of preference: resourceUUID, backupUUID, taskUUID.
     *
     * @param response JsonNode containing the YBA API response
     * @return String containing the extracted UUID, or null if not found
     */
    private String extractBackupUuidFromResponse(JsonNode response) {
        if (response == null) {
            return null;
        }

        // Try to get resourceUUID first (common in YBA responses)
        String uuid = response.path("resourceUUID").asText(null);
        if (uuid != null) {
            return uuid;
        }

        // Try backupUUID
        uuid = response.path("backupUUID").asText(null);
        if (uuid != null) {
            return uuid;
        }

        // Try taskUUID as fallback
        uuid = response.path("taskUUID").asText(null);
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
        body.put("storageConfigUUID", config.getStorageConfigUuid());
        body.put("sse", false);
        body.put("backupType", config.getBackupType());
        body.put("backupCategory", "YB_CONTROLLER");
        body.put("universeUUID", config.getUniverseUuid());
        body.put("baseBackupUUID", baseBackupUuid);
       // body.put("expiryTimeUnit", "MILLISECONDS");

        ArrayNode arr = body.putArray("keyspaceTableList");
        ObjectNode db = mapper.createObjectNode();
        db.put("keyspace", config.getDbName());
        arr.add(db);

        return webClient.post()
                .uri(config.getIncrementalBackupUrl())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-AUTH-YW-API-TOKEN", config.getApiToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }
}
