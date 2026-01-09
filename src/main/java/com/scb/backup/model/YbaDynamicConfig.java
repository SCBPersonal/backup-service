package com.scb.backup.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class YbaDynamicConfig {
    private String apiToken;
    private String universeUuid;
    private String customerUuid;
    private String storageConfigUuid;
    private String fullBackupUrl;
    private String incrementalBackupUrl;
    private String lastBackupUrl;
    private Long expiryMs;
    private String backupType;
    private String dbName;
    private  String backupCategoryType;
    private  String parentCategory;
}
