package com.scb.backup.service;

import com.scb.backup.model.YbaDynamicConfig;
import com.scb.backup.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BackupValidationServiceTest {

    @InjectMocks
    private BackupValidationService validationService;

    private Map<String, Object> batchParams;

    @BeforeEach
    void setUp() {
        batchParams = new HashMap<>();
        batchParams.put(AppConstants.BATCH_ID, "BATCH_001");
        batchParams.put(AppConstants.CATEGORY_CODE, "HWA_EPR_DB_BACKUP");
    }

    // ========== PUBLIC METHOD TESTS ==========

    @Test
    @DisplayName("Should pass validation when all required batch parameters are present")
    void validateBatchParams_ShouldPass_When_ValidParams() {
        assertDoesNotThrow(() -> validationService.validateBatchParams(batchParams));
    }

    @Test
    @DisplayName("Should throw exception when batchParams is null")
    void validateBatchParams_ShouldFail_When_ParamsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateBatchParams(null));
        assertEquals("Batch parameters cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when batchParams is empty")
    void validateBatchParams_ShouldFail_When_ParamsEmpty() {
        Map<String, Object> emptyParams = new HashMap<>();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateBatchParams(emptyParams));
        assertEquals("Batch parameters cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when batch ID is missing")
    void validateBatchParams_ShouldFail_When_BatchIdMissing() {
        batchParams.remove(AppConstants.BATCH_ID);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateBatchParams(batchParams));
        assertEquals("Batch ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when batch ID is null")
    void validateBatchParams_ShouldFail_When_BatchIdNull() {
        batchParams.put(AppConstants.BATCH_ID, null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateBatchParams(batchParams));
        assertEquals("Batch ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when batch ID is blank")
    void validateBatchParams_ShouldFail_When_BatchIdBlank() {
        batchParams.put(AppConstants.BATCH_ID, "   ");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateBatchParams(batchParams));
        assertEquals("Batch ID is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when category code is missing")
    void validateBatchParams_ShouldFail_When_CategoryCodeMissing() {
        batchParams.remove(AppConstants.CATEGORY_CODE);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateBatchParams(batchParams));
        assertEquals("Category code is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should pass validation with non-string values")
    void validateBatchParams_ShouldPass_When_NonStringValues() {
        // ✅ INDIRECTLY tests private validateRequiredParam with non-string values
        batchParams.put(AppConstants.BATCH_ID, 123L); // Long value
        batchParams.put(AppConstants.CATEGORY_CODE, true); // Boolean value

        assertDoesNotThrow(() -> validationService.validateBatchParams(batchParams));
    }

    @Test
    @DisplayName("Should pass backup config validation when all fields are present")
    void validateBackupConfig_ShouldPass_When_ValidConfig() {
        YbaDynamicConfig config = new YbaDynamicConfig();
        config.setApiToken("valid-api-token");
        config.setUniverseUuid("universe-uuid-123");

        assertDoesNotThrow(() -> validationService.validateBackupConfig(config, "TEST_CATEGORY"));
    }

    @Test
    @DisplayName("Should throw exception when backup config is null")
    void validateBackupConfig_ShouldFail_When_ConfigNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateBackupConfig(null, "TEST_CATEGORY"));
        assertEquals("No backup configuration found for category: TEST_CATEGORY", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when API token is blank")
    void validateBackupConfig_ShouldFail_When_ApiTokenBlank() {
        YbaDynamicConfig config = new YbaDynamicConfig();
        config.setUniverseUuid("universe-uuid-123");
        config.setApiToken("");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateBackupConfig(config, "TEST_CATEGORY"));
        assertEquals("API token is required for backup configuration", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when universe UUID is null")
    void validateBackupConfig_ShouldFail_When_UniverseUuidNull() {
        YbaDynamicConfig config = new YbaDynamicConfig();
        config.setApiToken("valid-api-token");
        config.setUniverseUuid(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> validationService.validateBackupConfig(config, "TEST_CATEGORY"));
        assertEquals("Universe UUID is required for backup configuration", exception.getMessage());
    }
}
