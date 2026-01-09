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
import java.util.Map;


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
    public YbaClient(WebClient webClient, YbaProperties props, YbaConfigService configService) {
        this.webClient = webClient;
        this.props = props;
        this.configService = configService;
    }


//    public Mono<String> backupInitiate(String categoryCode, Map<String, Object> batchParams){
//
//            YbaDynamicConfig config = configService.resolve(categoryCode);
//            String backupType = config.getBackupCategoryType().toString();
//            backupDaoService.insertBackupDetails(batchParams, AppConstants.BACKUP_INPROGRESS_STATUS, backupType);
//            if (backupType.equalsIgnoreCase(AppConstants.FULL_BACKUP)) {
//                //posting curl payload
//                return fullBackup(config).map(JsonNode::toString);
//            } else if (backupType.equalsIgnoreCase(AppConstants.INCREMENTAL_BACKUP)) {
//                return fetchLastBackup(config)
//                        .flatMap(page -> {
//                            if (page.has("entities") &&
//                                    page.get("entities").isArray() &&
//                                    page.get("entities").size() > 0) {
//
//                                String uuid = page.path("entities")
//                                        .path(0)
//                                        .path("commonBackupInfo")
//                                        .path("baseBackupUUID")
//                                        .asText(null);
//
//                                if (uuid == null) {
//                                    return Mono.error(new RuntimeException("Base backup UUID missing"));
//                                }
//
//                                return incrementalBackup(config, uuid);
//                            }
//                            return Mono.error(new RuntimeException("No previous backups found"));
//                        }).map(JsonNode::toString);
//            }
//            return null;
//    }

    public Mono<String> backupInitiate(String categoryCode, Map<String, Object> batchParams) {
        return Mono.fromCallable(() -> configService.resolve(categoryCode))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No configuration found for category: " + categoryCode)))
                .flatMap(config -> {
                    String backupType = config.getBackupCategoryType();
                    backupDaoService.insertBackupDetails(batchParams, AppConstants.BACKUP_INPROGRESS_STATUS, backupType);

                    if (AppConstants.FULL_BACKUP.equalsIgnoreCase(backupType)) {
                        return fullBackup(config);
                    } else if (AppConstants.INCREMENTAL_BACKUP.equalsIgnoreCase(backupType)) {
                        return performIncrementalBackup(config);
                    } else {
                        return Mono.error(new IllegalArgumentException("Unsupported backup type: " + backupType));
                    }
                })
                .map(JsonNode::toString)
                .doOnError(e -> log.error("Backup initiation failed for category: {}", categoryCode, e));
    }

    private Mono<JsonNode> performIncrementalBackup(YbaDynamicConfig config) {
        return fetchLastBackup(config)
                .flatMap(this::extractBaseBackupUuid)
                .flatMap(uuid -> incrementalBackup(config, uuid));
    }

    private Mono<String> extractBaseBackupUuid(JsonNode page) {
        if (!page.has("entities") || !page.get("entities").isArray() || page.get("entities").size() == 0) {
            return Mono.error(new RuntimeException("No previous backups found"));
        }

        String uuid = page.path("entities").path(0).path("commonBackupInfo").path("baseBackupUUID").asText(null);
        if (uuid == null) {
            return Mono.error(new RuntimeException("Base backup UUID missing"));
        }
        return Mono.just(uuid);
    }
    private Mono<JsonNode> fullBackup(YbaDynamicConfig config)  {

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



        return webClient.post()
                .uri(config.getFullBackupUrl())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-AUTH-YW-API-TOKEN", config.getApiToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> fetchLastBackup(YbaDynamicConfig config) {

        ObjectNode body = mapper.createObjectNode();
        body.put("storageConfigUUID", config.getStorageConfigUuid());
        body.put("sse", false);
        body.put("backupType", config.getBackupType());
        body.put("backupCategory", "YB_CONTROLLER");
        body.put("direction","DESC");
        body.put("sortBy", "createTime");
        body.put("timeBeforeDelete", config.getExpiryMs());
        body.put("expiryTimeUnit", "MILLISECONDS");

        ObjectNode filter = body.putObject("filter");
        ArrayNode uniList = filter.putArray("universeUUIDList");
        uniList.add(config.getUniverseUuid());
        body.put("limit",1);

        return webClient.post()
                .uri(config.getLastBackupUrl())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-AUTH-YW-API-TOKEN", config.getApiToken())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }



    public Mono<JsonNode> incrementalBackup(YbaDynamicConfig config, String baseBackupUuid) {
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
