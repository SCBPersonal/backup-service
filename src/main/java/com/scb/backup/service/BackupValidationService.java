package com.scb.backup.service;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class BackupValidationService {

    public void validateBatchParams(Map<String, Object> batchParams) {
        if (batchParams == null || batchParams.isEmpty()) {
            throw new IllegalArgumentException("Batch parameters cannot be null or empty");
        }

        validateRequiredParam(batchParams, com.hdfc.backup.utils.AppConstants.BATCH_ID, "Batch ID");
        validateRequiredParam(batchParams, com.hdfc.backup.utils.AppConstants.CATEGORY_CODE, "Category code");
    }

    public void validateBackupConfig(com.hdfc.backup.model.YbaDynamicConfig config, String categoryCode) {
        if (config == null) {
            throw new IllegalArgumentException("No backup configuration found for category: " + categoryCode);
        }

        if (StringUtils.isBlank(config.getApiToken())) {
            throw new IllegalArgumentException("API token is required for backup configuration");
        }

        if (StringUtils.isBlank(config.getUniverseUuid())) {
            throw new IllegalArgumentException("Universe UUID is required for backup configuration");
        }
    }

    private void validateRequiredParam(Map<String, Object> params, String key, String description) {
        Object value = params.get(key);
        if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
            throw new IllegalArgumentException(description + " is required");
        }
    }
}
