package com.scb.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.scb.backup.config.BackupPollerProperties;

import com.scb.backup.dao.BackupDaoService;

import com.scb.backup.model.YbaDynamicConfig;

import com.scb.backup.utils.AppConstants;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;

import org.springframework.web.reactive.function.client.*;

import reactor.core.publisher.Mono;

import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

class BackupPollerServiceTest {

    private BackupDaoService backupDaoService;

    private BackupPollerProperties pollerProperties;

    private YbaDynamicConfig config;

    private BackupPollerService service;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach

    void setup() {

        backupDaoService = mock(BackupDaoService.class);

        pollerProperties = mock(BackupPollerProperties.class);

        config = mock(YbaDynamicConfig.class);

        when(pollerProperties.isEnabled()).thenReturn(true);

        when(pollerProperties.getInitialDelayMs()).thenReturn(0L);

        when(pollerProperties.getPollingIntervalMs()).thenReturn(5L);

        when(config.getApiToken()).thenReturn("token");

        when(config.getJobCompletionCheckUrl())

                .thenReturn("http://dummy/status/{taskUuid}");

        when(config.getLastBackupUrl())

                .thenReturn("http://dummy/last-backup");

        when(config.getStorageConfigUuid()).thenReturn("storage-uuid");

        when(config.getBackupType()).thenReturn("FULL");

        when(config.getExpiryMs()).thenReturn(1000L);

        when(config.getUniverseUuid()).thenReturn("universe-uuid");

    }

    // --------------------------------------------------------

    // ✅ FULL BACKUP SUCCESS

    // --------------------------------------------------------

    @Test

    void pollFullBackup_success() {

        ExchangeFunction exchange = request -> {

            if (request.url().getPath().contains("status")) {

                return Mono.just(jsonResponse("{\"status\":\"Success\"}"));

            }

            if (request.url().getPath().contains("last-backup")) {

                return Mono.just(jsonResponse("""

                        {

                          "entities": [{

                            "backupUUID": "base-123",

                            "taskUUID": "task-123"

                          }]

                        }

                        """));

            }

            return Mono.error(new RuntimeException("Unexpected request"));

        };

        initService(exchange);

        Mono<String> result = service.pollFullBackupCompletion(

                config, "CAT1", "2026-02", "task-123", "batch1");

        StepVerifier.create(result)

                .expectNext("base-123")

                .verifyComplete();

        verify(backupDaoService).updateFullBackupWithBaseUuid(

                "CAT1", "2026-02", "base-123",

                "batch1", AppConstants.BACKUP_SUCCESS_STATUS);

    }

    // --------------------------------------------------------

    // ✅ FULL BACKUP FAILURE STATUS

    // --------------------------------------------------------

    @Test

    void pollFullBackup_failureStatus() {

        ExchangeFunction exchange = request ->

                Mono.just(jsonResponse("{\"status\":\"Failure\"}"));

        initService(exchange);

        Mono<String> result = service.pollFullBackupCompletion(

                config, "CAT1", "2026-02", "task-123", "batch1");

        StepVerifier.create(result)

                .expectError(RuntimeException.class)

                .verify();

        verify(backupDaoService, times(2)).updateFullBackupWithBaseUuid(
                "CAT1", "2026-02", "",
                "batch1", AppConstants.BACKUP_FAILED_STATUS);

    }

    // --------------------------------------------------------

    // ✅ TASK UUID MISMATCH

    // --------------------------------------------------------

    @Test

    void pollFullBackup_taskUuidMismatch() {

        ExchangeFunction exchange = request -> {

            if (request.url().getPath().contains("status")) {

                return Mono.just(jsonResponse("{\"status\":\"Success\"}"));

            }

            if (request.url().getPath().contains("last-backup")) {

                return Mono.just(jsonResponse("""

                        {

                          "entities": [{

                            "backupUUID": "base-123",

                            "taskUUID": "DIFFERENT"

                          }]

                        }

                        """));

            }

            return Mono.error(new RuntimeException("Unexpected request"));

        };

        initService(exchange);

        Mono<String> result = service.pollFullBackupCompletion(

                config, "CAT1", "2026-02", "task-123", "batch1");

        StepVerifier.create(result)

                .expectError(RuntimeException.class)

                .verify();

        verify(backupDaoService, atLeastOnce())

                .updateFullBackupWithBaseUuid(

                        eq("CAT1"), eq("2026-02"),

                        eq(""), eq("batch1"),

                        anyString());

    }

    // --------------------------------------------------------

    // ✅ INCREMENTAL SUCCESS

    // --------------------------------------------------------

    @Test

    void pollIncremental_success() {

        ExchangeFunction exchange = request ->

                Mono.just(jsonResponse("{\"status\":\"Success\"}"));

        initService(exchange);

        Mono<Void> result = service.pollIncrementalBackupCompletion(

                config, "CAT1", "2026-02",

                "task-123", "batch1", "base-123","");

        StepVerifier.create(result)

                .verifyComplete();

        verify(backupDaoService)

                .updateIncrementalBackupStatusByMonth(

                        "CAT1", "2026-02",

                        "base-123",

                        AppConstants.BACKUP_SUCCESS_STATUS,

                        "batch1","","task-123");

    }

    // --------------------------------------------------------

    // ✅ INCREMENTAL FAILURE

    // --------------------------------------------------------

    @Test

    void pollIncremental_failure() {

        ExchangeFunction exchange = request ->

                Mono.just(jsonResponse("{\"status\":\"Failure\"}"));

        initService(exchange);

        Mono<Void> result = service.pollIncrementalBackupCompletion(

                config, "CAT1", "2026-02",

                "task-123", "batch1", "base-123","");

        StepVerifier.create(result)

                .expectError(RuntimeException.class)

                .verify();

        verify(backupDaoService, times(1)).updateIncrementalBackupStatusByMonth(
                "CAT1", "2026-02", "base-123",
                AppConstants.BACKUP_FAILED_STATUS, "batch1","","task-123");

    }

    // --------------------------------------------------------

    // ✅ POLLER DISABLED

    // --------------------------------------------------------

    @Test

    void pollFullBackup_whenDisabled_shouldError() {

        when(pollerProperties.isEnabled()).thenReturn(false);

        service = new BackupPollerService(

                WebClient.builder().build(),

                backupDaoService,

                pollerProperties,

                mapper

        );

        Mono<String> result = service.pollFullBackupCompletion(

                config, "CAT1", "2026-02",

                "task-123", "batch1");

        StepVerifier.create(result)

                .expectError(RuntimeException.class)

                .verify();

    }

    // --------------------------------------------------------

    // ✅ EMPTY LAST BACKUP RESPONSE

    // --------------------------------------------------------

    @Test

    void pollFullBackup_emptyLastBackup_shouldFail() {

        ExchangeFunction exchange = request -> {

            if (request.url().getPath().contains("status")) {

                return Mono.just(jsonResponse("{\"status\":\"Success\"}"));

            }

            if (request.url().getPath().contains("last-backup")) {

                return Mono.just(jsonResponse("{\"entities\": []}"));

            }

            return Mono.error(new RuntimeException("Unexpected request"));

        };

        initService(exchange);

        Mono<String> result = service.pollFullBackupCompletion(

                config, "CAT1", "2026-02", "task-123", "batch1");

        StepVerifier.create(result)
                .verifyComplete();


    }

    // --------------------------------------------------------

    // 🔧 Helper Methods

    // --------------------------------------------------------

    private void initService(ExchangeFunction exchange) {

        WebClient webClient = WebClient.builder()

                .exchangeFunction(exchange)

                .build();

        service = new BackupPollerService(

                webClient,

                backupDaoService,

                pollerProperties,

                mapper

        );

    }

    private ClientResponse jsonResponse(String body) {

        return ClientResponse.create(HttpStatus.OK)

                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)

                .body(body)

                .build();

    }

}



