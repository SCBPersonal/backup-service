package com.scb.backup.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.Locale;

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

    // ========== NEW TESTS FOR calculatePeriodRange() ==========

    @Test
    void calculatePeriodRange_Monthly_ShouldReturnFullMonthRange() {
        // Given
        String backupFrequency = "MONTHLY";
        String epochDate = "2026-01-01";

        // When
        String result = PeriodCalculator.calculatePeriodRange(backupFrequency, epochDate);

        // Then
        assertNotNull(result);
        // Should be in format "YYYY-MM-DD to YYYY-MM-DD"
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} to \\d{4}-\\d{2}-\\d{2}"));

        // Verify it's the current month's range
        LocalDate now = LocalDate.now();
        YearMonth yearMonth = YearMonth.from(now);
        String expectedStart = yearMonth.atDay(1).toString();
        String expectedEnd = yearMonth.atEndOfMonth().toString();
        String expected = expectedStart + " to " + expectedEnd;
        assertEquals(expected, result);
    }

    @Test
    void calculatePeriodRange_Weekly_ShouldReturnWeekRange() {
        // Given
        String backupFrequency = "WEEKLY";
        String epochDate = "2026-01-01";

        // When
        String result = PeriodCalculator.calculatePeriodRange(backupFrequency, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} to \\d{4}-\\d{2}-\\d{2}"));

        // Verify it's a 7-day range (Monday to Sunday)
        LocalDate now = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        LocalDate monday = now.with(weekFields.dayOfWeek(), 1);
        LocalDate sunday = monday.plusDays(6);
        String expected = monday.toString() + " to " + sunday.toString();
        assertEquals(expected, result);
    }

    @Test
    void calculatePeriodRange_10Days_ShouldReturn10DayRange() {
        // Given
        String backupFrequency = "10_DAYS";
        String epochDate = "2026-01-01";

        // When
        String result = PeriodCalculator.calculatePeriodRange(backupFrequency, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} to \\d{4}-\\d{2}-\\d{2}"));

        // Extract start and end dates
        String[] parts = result.split(" to ");
        LocalDate startDate = LocalDate.parse(parts[0]);
        LocalDate endDate = LocalDate.parse(parts[1]);

        // Verify it's a 10-day range (0-9 days, so 10 days total)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        assertEquals(9, daysBetween); // 10 days = 0 to 9
    }

    @Test
    void calculatePeriodRange_15Days_ShouldReturn15DayRange() {
        // Given
        String backupFrequency = "15_DAYS";
        String epochDate = "2026-01-01";

        // When
        String result = PeriodCalculator.calculatePeriodRange(backupFrequency, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} to \\d{4}-\\d{2}-\\d{2}"));

        // Extract start and end dates
        String[] parts = result.split(" to ");
        LocalDate startDate = LocalDate.parse(parts[0]);
        LocalDate endDate = LocalDate.parse(parts[1]);

        // Verify it's a 15-day range
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        assertEquals(14, daysBetween); // 15 days = 0 to 14
    }

    @Test
    void calculatePeriodRange_20Days_ShouldReturn20DayRange() {
        // Given
        String backupFrequency = "20_DAYS";
        String epochDate = "2026-01-01";

        // When
        String result = PeriodCalculator.calculatePeriodRange(backupFrequency, epochDate);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} to \\d{4}-\\d{2}-\\d{2}"));

        // Extract start and end dates
        String[] parts = result.split(" to ");
        LocalDate startDate = LocalDate.parse(parts[0]);
        LocalDate endDate = LocalDate.parse(parts[1]);

        // Verify it's a 20-day range
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        assertEquals(19, daysBetween); // 20 days = 0 to 19
    }

    @Test
    void calculatePeriodRange_InvalidFrequency_ShouldThrowException() {
        // Given
        String backupFrequency = "INVALID";
        String epochDate = "2026-01-01";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            PeriodCalculator.calculatePeriodRange(backupFrequency, epochDate);
        });
    }
}


