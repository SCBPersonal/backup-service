package com.scb.backup.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "yba")
@Data
public class YdbConfigDetails {

    private String ybaDb;
    private String apiToken;
    private String customerUuid;
    private String universeUuid;
    private String storageConfigUuid;
    private String expiryMs;
}
