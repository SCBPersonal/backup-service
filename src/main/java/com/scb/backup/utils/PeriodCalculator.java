package com.scb.backup.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * PeriodCalculator - Simple utility for calculating backup periods using date formats.
 *
 * This class calculates backup periods based on backup frequency and date format patterns
 * configured in application.yml.
 *
 * Supported frequency values:
 * - MONTHLY: Returns YYYY-MM format
 * - WEEKLY: Returns YYYY-Www format
 * - N_DAYS (e.g., 10_DAYS, 15_DAYS): Returns YYYY-MM-DD format for interval start date
 *
 * @author SCB ePricing Team
 * @version 4.0
 * @since 2026-03-05
 */
@Slf4j
public class PeriodCalculator {

    /**
     * Calculates the backup period based on backup frequency value.
     *
     * @param backupFrequency Backup frequency (MONTHLY, WEEKLY, or N_DAYS like 10_DAYS)
     * @param formatPattern Date format pattern from YAML
     * @param epochDate Epoch date string (only for N_DAYS format)
     * @return Calculated period string
     */
    public static String calculatePeriod(String backupFrequency, String formatPattern, String epochDate) {
        LocalDate currentDate = LocalDate.now();

        // Check if it's a custom interval (e.g., 10_DAYS, 15_DAYS)
        if (backupFrequency.endsWith("_DAYS")) {
            int intervalDays = extractIntervalDays(backupFrequency);
            return calculateCustomPeriod(currentDate, intervalDays, epochDate, formatPattern);
        } else {
            // For MONTHLY and WEEKLY, just format the current date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
            return currentDate.format(formatter);
        }
    }

    /**
     * Extracts interval days from backup frequency value.
     * Example: "10_DAYS" → 10, "15_DAYS" → 15
     *
     * @param backupFrequency Backup frequency value (e.g., "10_DAYS")
     * @return Number of days in the interval
     */
    private static int extractIntervalDays(String backupFrequency) {
        try {
            String daysStr = backupFrequency.replace("_DAYS", "");
            return Integer.parseInt(daysStr);
        } catch (NumberFormatException e) {
            log.error("Invalid backup frequency format: {}. Expected format: N_DAYS (e.g., 10_DAYS)", backupFrequency);
            throw new IllegalArgumentException("Invalid backup frequency format: " + backupFrequency +
                ". Expected format: N_DAYS (e.g., 10_DAYS, 15_DAYS)", e);
        }
    }

    /**
     * Calculates custom interval period.
     *
     * @param currentDate Current date
     * @param intervalDays Number of days in each interval
     * @param epochDateStr Epoch date string (yyyy-MM-dd)
     * @param formatPattern Date format pattern
     * @return Period string representing the interval start date
     */
    private static String calculateCustomPeriod(LocalDate currentDate, int intervalDays,
                                               String epochDateStr, String formatPattern) {
        LocalDate epochDate = LocalDate.parse(epochDateStr);
        long daysSinceEpoch = ChronoUnit.DAYS.between(epochDate, currentDate);
        long intervalNumber = daysSinceEpoch / intervalDays;
        long intervalStartDay = intervalNumber * intervalDays;
        LocalDate intervalStartDate = epochDate.plusDays(intervalStartDay);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
        return intervalStartDate.format(formatter);
    }
}

