package com.scb.backup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.backup.model.YbaDynamicConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import com.scb.backup.config.YbaProperties;

import java.util.HashMap;
import java.util.Map;

@Service
public class YbaConfigService {

    private final YbaProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private Map<String, YbaDynamicConfig> configMap;

    public YbaConfigService(YbaProperties props) {
        this.props = props;
    }

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

    public YbaDynamicConfig resolve(String dbName) {
        return configMap.get(dbName);
    }
}
