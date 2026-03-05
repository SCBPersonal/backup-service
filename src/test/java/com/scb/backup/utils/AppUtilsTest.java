package com.scb.backup.utils;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for AppUtils class.
 * Tests all utility methods for date formatting and batch parameter creation.
 */
class AppUtilsTest {

    @Test
    void getBusinessDate_ShouldRemoveHyphens() {
        // Given
        String dateWithHyphens = "2026-03-15";

        // When
        String result = AppUtils.getBusinessDate(dateWithHyphens);

        // Then
        assertEquals("20260315", result);
    }

    @Test
    void getBusinessDate_WithEmptyString_ShouldReturnEmptyString() {
        // Given
        String emptyDate = "";

        // When
        String result = AppUtils.getBusinessDate(emptyDate);

        // Then
        assertEquals("", result);
    }

    @Test
    void getBusinessDate_WithNoHyphens_ShouldReturnSameString() {
        // Given
        String dateWithoutHyphens = "20260315";

        // When
        String result = AppUtils.getBusinessDate(dateWithoutHyphens);

        // Then
        assertEquals("20260315", result);
    }

    @Test
    void toDate_WithValidDate_ShouldParseSuccessfully() {
        // Given
        String validDate = "20260315";

        // When
        Date result = AppUtils.toDate(validDate);

        // Then
        assertNotNull(result);
    }

    @Test
    void toDate_WithInvalidDate_ShouldReturnNull() {
        // Given
        String invalidDate = "invalid-date";

        // When
        Date result = AppUtils.toDate(invalidDate);

        // Then
        assertNull(result);
    }

    @Test
    void toDate_WithNullDate_ShouldThrowNullPointerException() {
        // Given
        String nullDate = null;

        // When & Then - Expect NPE as SimpleDateFormat.parse doesn't handle null
        assertThrows(NullPointerException.class, () -> {
            AppUtils.toDate(nullDate);
        });
    }

    @Test
    void toDate_WithEmptyString_ShouldReturnNull() {
        // Given
        String emptyDate = "";

        // When
        Date result = AppUtils.toDate(emptyDate);

        // Then
        assertNull(result);
    }

    @Test
    void createBatchParams_ShouldCreateMapWithAllParameters() {
        // Given
        String batchId = "batch-123";
        String businessDate = "20260315";
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";

        // When
        Map<String, Object> result = AppUtils.createBatchParams(batchId, businessDate, categoryCode);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(batchId, result.get(AppConstants.BATCH_ID));
        assertNotNull(result.get(AppConstants.BUSINESS_DATE));
        assertEquals(categoryCode, result.get(AppConstants.CATEGORY_CODE));
    }

    @Test
    void createBatchParams_WithInvalidDate_ShouldHaveNullBusinessDate() {
        // Given
        String batchId = "batch-123";
        String invalidDate = "invalid";
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";

        // When
        Map<String, Object> result = AppUtils.createBatchParams(batchId, invalidDate, categoryCode);

        // Then
        assertNotNull(result);
        assertNull(result.get(AppConstants.BUSINESS_DATE));
    }

    @Test
    void createBatchParams_WithValidValues_ShouldCreateCompleteMap() {
        // Given
        String batchId = "batch-456";
        String businessDate = "20260320";
        String categoryCode = "HWA_EPR_DB_BACKUP_INCRE";

        // When
        Map<String, Object> result = AppUtils.createBatchParams(batchId, businessDate, categoryCode);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(batchId, result.get(AppConstants.BATCH_ID));
        assertNotNull(result.get(AppConstants.BUSINESS_DATE));
        assertEquals(categoryCode, result.get(AppConstants.CATEGORY_CODE));
    }
}

