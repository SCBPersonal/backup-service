package com.scb.backup.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for PeriodCalculator class.
 * Tests period calculation for monthly, weekly, and custom interval backups.
 */
class PeriodCalculatorTest {

    @Test
    void calculatePeriod_Monthly_ShouldReturnYearMonth() {
        // Given
        String backupFrequency = "MONTHLY";
        String format = "yyyy-MM";
        String epochDate = null;

        // When
        String result = PeriodCalculator.calculatePeriod(backupFrequency, format, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}"));
    }

    @Test
    void calculatePeriod_Weekly_ShouldReturnISOWeek() {
        // Given
        String backupFrequency = "WEEKLY";
        String format = "yyyy-'W'ww";
        String epochDate = null;

        // When
        String result = PeriodCalculator.calculatePeriod(backupFrequency, format, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-W\\d{2}"));
    }

    @Test
    void calculatePeriod_Custom10Days_ShouldReturnIntervalStartDate() {
        // Given
        String backupFrequency = "10_DAYS";
        String format = "yyyy-MM-dd";
        String epochDate = "2026-01-01";

        // When
        String result = PeriodCalculator.calculatePeriod(backupFrequency, format, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void calculatePeriod_Custom15Days_ShouldReturnIntervalStartDate() {
        // Given
        String backupFrequency = "15_DAYS";
        String format = "yyyy-MM-dd";
        String epochDate = "2026-01-01";

        // When
        String result = PeriodCalculator.calculatePeriod(backupFrequency, format, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void calculatePeriod_Custom20Days_ShouldReturnIntervalStartDate() {
        // Given
        String backupFrequency = "20_DAYS";
        String format = "yyyy-MM-dd";
        String epochDate = "2026-01-01";

        // When
        String result = PeriodCalculator.calculatePeriod(backupFrequency, format, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void calculatePeriod_Custom30Days_ShouldReturnIntervalStartDate() {
        // Given
        String backupFrequency = "30_DAYS";
        String format = "yyyy-MM-dd";
        String epochDate = "2026-01-01";

        // When
        String result = PeriodCalculator.calculatePeriod(backupFrequency, format, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void calculatePeriod_InvalidFormat_ShouldThrowException() {
        // Given
        String backupFrequency = "INVALID_DAYS";
        String format = "yyyy-MM-dd";
        String epochDate = "2026-01-01";

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                PeriodCalculator.calculatePeriod(backupFrequency, format, epochDate));
    }

    @Test
    void calculatePeriod_MonthlyWithDifferentFormat_ShouldWork() {
        // Given
        String backupFrequency = "MONTHLY";
        String format = "yyyy/MM";
        String epochDate = null;

        // When
        String result = PeriodCalculator.calculatePeriod(backupFrequency, format, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}/\\d{2}"));
    }

    @Test
    void calculatePeriod_WeeklyWithDifferentFormat_ShouldWork() {
        // Given
        String backupFrequency = "WEEKLY";
        String format = "yyyy-ww";
        String epochDate = null;

        // When
        String result = PeriodCalculator.calculatePeriod(backupFrequency, format, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}"));
    }
}


