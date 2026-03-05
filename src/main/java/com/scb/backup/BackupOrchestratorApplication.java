package com.scb.backup;

import com.hdfcbank.epricing.batch.core.lib.util.SSLContextInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * BackupOrchestratorApplication - Main Spring Boot application class.
 *
 * This is the entry point for the YugabyteDB Backup Orchestrator Service.
 * The application provides REST APIs for managing database backups through
 * the YugabyteDB Anywhere (YBA) platform.
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Full and incremental backup support</li>
 *   <li>Base UUID tracking for monthly incremental backups</li>
 *   <li>Background polling for backup job completion</li>
 *   <li>Integration with batch processing framework</li>
 *   <li>Reactive programming with WebFlux</li>
 *   <li>Database persistence for backup status tracking</li>
 * </ul>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 */
@SpringBootApplication
@EnableAsync
public class BackupOrchestratorApplication {
    /**
     * Main method to start the Spring Boot application.
     *
     * @param args Command line arguments
     */

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(BackupOrchestratorApplication.class);

        builder.initializers(new SSLContextInitializer());

        builder.run(args);
    }
}
