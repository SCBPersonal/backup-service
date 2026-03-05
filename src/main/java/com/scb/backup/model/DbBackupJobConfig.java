package com.scb.backup.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DbBackupJobConfig {

    private String backupJobCategory;
    private String backupType;
    private String dbName;
    private String dbUrl;

}
