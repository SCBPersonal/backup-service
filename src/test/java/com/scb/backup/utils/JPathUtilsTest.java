package com.scb.backup.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for JPathUtils class.
 * Tests JSON path extraction functionality.
 */
class JPathUtilsTest {

    @Test
    void get_WithValidJsonAndPath_ShouldReturnValue() {
        // Given
        String json = "{\"batchCategoryCode\": \"HWA_EPR_DB_BACKUP_FULL\"}";
        String expression = "$.batchCategoryCode";

        // When
        Object result = JPathUtils.get(json, expression);

        // Then
        assertNotNull(result);
        assertEquals("HWA_EPR_DB_BACKUP_FULL", result);
    }

    @Test
    void get_WithNestedPath_ShouldReturnValue() {
        // Given
        String json = "{\"batchCategoryParameters\": {\"subCategoryCode\": \"FULL\"}}";
        String expression = "$.batchCategoryParameters.subCategoryCode";

        // When
        Object result = JPathUtils.get(json, expression);

        // Then
        assertNotNull(result);
        assertEquals("FULL", result);
    }

    @Test
    void get_WithBackupFrequency_ShouldReturnValue() {
        // Given
        String json = "{\"backupFrequency\": \"MONTHLY\"}";
        String expression = "$.backupFrequency";

        // When
        Object result = JPathUtils.get(json, expression);

        // Then
        assertNotNull(result);
        assertEquals("MONTHLY", result);
    }

    @Test
    void get_WithBackupFrequency10Days_ShouldReturnValue() {
        // Given
        String json = "{\"backupFrequency\": \"10_DAYS\"}";
        String expression = "$.backupFrequency";

        // When
        Object result = JPathUtils.get(json, expression);

        // Then
        assertNotNull(result);
        assertEquals("10_DAYS", result);
    }

    @Test
    void get_WithInvalidPath_ShouldReturnNull() {
        // Given
        String json = "{\"batchCategoryCode\": \"HWA_EPR_DB_BACKUP_FULL\"}";
        String expression = "$.nonExistentField";

        // When
        Object result = JPathUtils.get(json, expression);

        // Then
        assertNull(result);
    }

    @Test
    void get_WithInvalidJson_ShouldReturnNull() {
        // Given
        String invalidJson = "not a valid json";
        String expression = "$.batchCategoryCode";

        // When
        Object result = JPathUtils.get(invalidJson, expression);

        // Then
        assertNull(result);
    }

    @Test
    void get_WithNullJson_ShouldReturnNull() {
        // Given
        String nullJson = null;
        String expression = "$.batchCategoryCode";

        // When
        Object result = JPathUtils.get(nullJson, expression);

        // Then
        assertNull(result);
    }

    @Test
    void get_WithEmptyJson_ShouldReturnNull() {
        // Given
        String emptyJson = "";
        String expression = "$.batchCategoryCode";

        // When
        Object result = JPathUtils.get(emptyJson, expression);

        // Then
        assertNull(result);
    }

    @Test
    void get_WithComplexNestedJson_ShouldReturnValue() {
        // Given
        String json = "{\"payload\": {\"backupType\": \"FULL\", \"backupFrequency\": \"WEEKLY\"}}";
        String expression = "$.payload.backupFrequency";

        // When
        Object result = JPathUtils.get(json, expression);

        // Then
        assertNotNull(result);
        assertEquals("WEEKLY", result);
    }

    @Test
    void get_WithArrayInJson_ShouldReturnArray() {
        // Given
        String json = "{\"items\": [\"item1\", \"item2\", \"item3\"]}";
        String expression = "$.items";

        // When
        Object result = JPathUtils.get(json, expression);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof java.util.List);
    }

    @Test
    void get_WithNumericValue_ShouldReturnNumber() {
        // Given
        String json = "{\"intervalDays\": 10}";
        String expression = "$.intervalDays";

        // When
        Object result = JPathUtils.get(json, expression);

        // Then
        assertNotNull(result);
        assertEquals(10, result);
    }

    @Test
    void get_WithBooleanValue_ShouldReturnBoolean() {
        // Given
        String json = "{\"enabled\": true}";
        String expression = "$.enabled";

        // When
        Object result = JPathUtils.get(json, expression);

        // Then
        assertNotNull(result);
        assertEquals(true, result);
    }
}

