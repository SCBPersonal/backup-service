package com.scb.backup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.backup.model.YbaDynamicConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import com.scb.backup.config.YbaProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * YbaConfigService - Service for managing YugabyteDB Anywhere (YBA) configurations.
 *
 * This service loads and manages database-specific backup configurations from
 * application properties. It provides a centralized configuration repository
 * that can be queried by category code to retrieve YBA API connection details
 * and backup settings.
 *
 * <p><b>Configuration Structure:</b></p>
 * <pre>
 * yba:
 *   databases:
 *     HWA_EPR_DB_BACKUP_FULL:
 *       full-backup-url: "https://yba-api/backups"
 *       api-token: "token123"
 *       universe-uuid: "uuid-456"
 *       ...
 * </pre>
 *
 * <p>Configurations are loaded at application startup via {@code @PostConstruct}
 * and cached in memory for fast retrieval during backup operations.</p>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 * @see YbaProperties
 * @see YbaDynamicConfig
 */
@Service
public class YbaConfigService {

    private final YbaProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private Map<String, YbaDynamicConfig> configMap;

    /**
     * Constructor for YbaConfigService.
     *
     * @param props YBA properties loaded from application.yml
     */
    public YbaConfigService(YbaProperties props) {
        this.props = props;
    }

    /**
     * Initializes the configuration map from application properties.
     *
     * This method is called automatically after bean construction. It loads
     * all database configurations from YbaProperties and builds a map of
     * YbaDynamicConfig objects keyed by category code (uppercase).
     *
     * <p>The configuration map is used for fast lookup during backup operations.</p>
     */
    @PostConstruct
    public void init() {
        configMap = new HashMap<>();

        props.getDatabases().forEach((key, dbConfig) -> {
            YbaDynamicConfig dynamicConfig = YbaDynamicConfig.builder()
                    .fullBackupUrl(dbConfig.getFullBackupUrl())
                    .incrementalBackupUrl(dbConfig.getIncrementalBackupUrl())
                    .lastBackupUrl(dbConfig.getLastBackupUrl())
                    .storageConfigUuid(dbConfig.getStorageConfigUuid())
                    .apiToken(dbConfig.getApiToken())
                    .universeUuid(dbConfig.getUniverseUuid())
                    .backupType(dbConfig.getBackupType())
                    .backupCategoryType(dbConfig.getBackupCategoryType())
                    .dbName(dbConfig.getDbName())
                    .expiryMs(dbConfig.getExpiryMs())
                    .build();

            configMap.put(key.toUpperCase(), dynamicConfig);
        });
    }

    /**
     * Resolves YBA configuration by category code.
     *
     * Retrieves the backup configuration for the specified category code.
     * The category code is used as the key to lookup the configuration in
     * the internal map.
     *
     * @param dbName Category code (e.g., "HWA_EPR_DB_BACKUP_FULL")
     * @return YbaDynamicConfig containing YBA API details and backup settings, or null if not found
     */
    public YbaDynamicConfig resolve(String dbName) {
        return configMap.get(dbName);
    }
}
