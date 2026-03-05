package com.scb.backup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PeriodCalculationProperties - YAML-based configuration for backup period calculation.
 *
 * This class reads period calculation rules from application.yml and provides
 * simple date format patterns for different frequency types.
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-03-05
 */
@Data
@Component
@ConfigurationProperties(prefix = "backup.period-calculation")
public class PeriodCalculationProperties {

    /**
     * Map of frequency type to period configuration.
     * Key: Frequency type (MONTHLY, WEEKLY, CUSTOM)
     * Value: PeriodConfig containing format and description
     */
    private Map<String, PeriodConfig> configs;

    /**
     * Configuration for a specific period calculation.
     */
    @Data
    public static class PeriodConfig {
        /**
         * Date format pattern for the period.
         * Examples:
         * - MONTHLY: "yyyy-MM"
         * - WEEKLY: "YYYY-'W'ww"
         * - CUSTOM: "yyyy-MM-dd"
         */
        private String format;

        /**
         * Description of the period format.
         */
        private String description;

        /**
         * Epoch date for CUSTOM interval calculation.
         * Format: yyyy-MM-dd
         */
        private String epochDate;
    }

    /**
     * Gets the period configuration for a specific frequency type.
     *
     * @param frequencyType Frequency type (MONTHLY, WEEKLY, CUSTOM)
     * @return PeriodConfig for the frequency type
     */
    public PeriodConfig getConfig(String frequencyType) {
        return configs.get(frequencyType.toUpperCase());
    }
}

