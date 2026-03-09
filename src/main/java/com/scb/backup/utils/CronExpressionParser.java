package com.scb.backup.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * CronExpressionParser - Utility for parsing and analyzing cron expressions.
 *
 * This utility supports three types of cron expressions:
 * 1. MONTHLY: "0 0 2 1 * *" - 1st of every month at 2 AM
 * 2. WEEKLY: "0 0 2 * * MON" - Every Monday at 2 AM
 * 3. CUSTOM: "0 0 3 15 6 *" - June 15 at 3 AM (specific date)
 *
 * <p><b>Cron Expression Format (6 fields):</b></p>
 * <pre>
 * ┌───────────── second (0-59)
 * │ ┌───────────── minute (0-59)
 * │ │ ┌───────────── hour (0-23)
 * │ │ │ ┌───────────── day of month (1-31)
 * │ │ │ │ ┌───────────── month (1-12 or JAN-DEC)
 * │ │ │ │ │ ┌───────────── day of week (0-7 or MON-SUN)
 * │ │ │ │ │ │
 * * * * * * *
 * </pre>
 *
 * @author SCB ePricing Team
 * @version 3.0
 * @since 2026-03-09
 */
@Slf4j
public class CronExpressionParser {

    /**
     * Normalizes a cron expression to be compatible with Spring's CronExpression parser.
     * Converts day-of-week to '?' when day-of-month is specified, and vice versa.
     *
     * @param cronExpression Original cron expression
     * @return Normalized cron expression
     */
    private static String normalizeCronExpression(String cronExpression) {
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length != 6) {
            return cronExpression;
        }

        String dayOfMonth = parts[3];
        String dayOfWeek = parts[5];

        // If day-of-month is specified (not * or ?), set day-of-week to ?
        if (!dayOfMonth.equals("*") && !dayOfMonth.equals("?")) {
            parts[5] = "?";
        }
        // If day-of-week is specified (not * or ?), set day-of-month to ?
        else if (!dayOfWeek.equals("*") && !dayOfWeek.equals("?")) {
            parts[3] = "?";
        }
        // If both are *, set day-of-week to ?
        else if (dayOfMonth.equals("*") && dayOfWeek.equals("*")) {
            parts[5] = "?";
        }

        return String.join(" ", parts);
    }

    /**
     * Validates a cron expression.
     *
     * @param cronExpression Cron expression to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        try {
            String normalizedCron = normalizeCronExpression(cronExpression);
            CronExpression.parse(normalizedCron);
            return true;
        } catch (IllegalArgumentException e) {
            log.error("Invalid cron expression: {}", cronExpression, e);
            return false;
        }
    }

    /**
     * Validates and throws exception if invalid.
     *
     * @param cronExpression Cron expression to validate
     * @throws IllegalArgumentException if invalid
     */
    public static void validate(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty");
        }

        try {
            String normalizedCron = normalizeCronExpression(cronExpression);
            CronExpression.parse(normalizedCron);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression + ". Error: " + e.getMessage());
        }
    }

    /**
     * Gets the next execution time for a cron expression.
     *
     * @param cronExpression Cron expression
     * @return Next execution time
     */
    public static LocalDateTime getNextExecutionTime(String cronExpression) {
        validate(cronExpression);

        String normalizedCron = normalizeCronExpression(cronExpression);
        CronExpression cron = CronExpression.parse(normalizedCron);
        LocalDateTime nextExecution = cron.next(LocalDateTime.now());

        if (nextExecution == null) {
            throw new IllegalArgumentException("No future execution time for cron expression: " + cronExpression);
        }

        return nextExecution;
    }

    /**
     * Calculates backup period identifier from cron expression.
     * Returns format: "YYYY-MM-DD-HHmm" based on next execution time.
     *
     * @param cronExpression Cron expression
     * @return Backup period identifier
     */
    public static String calculateBackupPeriod(String cronExpression) {
        LocalDateTime nextExecution = getNextExecutionTime(cronExpression);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");
        return nextExecution.format(formatter);
    }

    /**
     * Calculates backup interval (human-readable) from cron expression.
     *
     * @param cronExpression Cron expression
     * @return Human-readable backup interval
     */
    public static String calculateBackupInterval(String cronExpression) {
        LocalDateTime nextExecution = getNextExecutionTime(cronExpression);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return nextExecution.format(formatter) + " (Cron: " + cronExpression + ")";
    }

    /**
     * Determines the frequency type from cron expression.
     *
     * @param cronExpression Cron expression
     * @return "MONTHLY", "WEEKLY", or "CUSTOM"
     */
    public static String determineFrequencyType(String cronExpression) {
        validate(cronExpression);
        
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length != 6) {
            return "CUSTOM";
        }

        String dayOfMonth = parts[3];
        String month = parts[4];
        String dayOfWeek = parts[5];

        // MONTHLY: specific day of month, any month, no day of week
        // Example: "0 0 2 1 * *" (1st of every month)
        if (!dayOfMonth.equals("*") && !dayOfMonth.equals("?") && 
            month.equals("*") && 
            (dayOfWeek.equals("*") || dayOfWeek.equals("?"))) {
            return "MONTHLY";
        }

        // WEEKLY: any day of month, any month, specific day of week
        // Example: "0 0 2 * * MON" (every Monday)
        if ((dayOfMonth.equals("*") || dayOfMonth.equals("?")) && 
            month.equals("*") && 
            !dayOfWeek.equals("*") && !dayOfWeek.equals("?")) {
            return "WEEKLY";
        }

        // CUSTOM: specific date (day + month)
        // Example: "0 0 3 15 6 *" (June 15)
        return "CUSTOM";
    }

    /**
     * Gets a human-readable description of the cron expression.
     *
     * @param cronExpression Cron expression
     * @return Human-readable description
     */
    public static String getDescription(String cronExpression) {
        String frequencyType = determineFrequencyType(cronExpression);
        LocalDateTime nextExecution = getNextExecutionTime(cronExpression);
        
        return String.format("%s backup - Next execution: %s", 
            frequencyType, 
            nextExecution.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}

