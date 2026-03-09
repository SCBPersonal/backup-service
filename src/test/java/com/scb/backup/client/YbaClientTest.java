package com.scb.backup.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.backup.model.YbaDynamicConfig;
import com.scb.backup.service.BackupPollerService;
import com.scb.backup.service.YbaConfigService;
import com.scb.backup.dao.BackupDaoService;
import com.scb.backup.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class YbaClientTest {

    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private BackupDaoService backupDaoService;
    @Mock
    private BackupPollerService backupPollerService;
    @Mock
    private YbaConfigService configService;

    @InjectMocks
    private YbaClient ybaClient;

    private final ObjectMapper mapper = new ObjectMapper();
    private YbaDynamicConfig config;

    @BeforeEach
    void setup() {
        config = new YbaDynamicConfig();
        config.setBackupCategoryType(AppConstants.FULL_BACKUP);
        config.setFullBackupUrl("http://test/full");
        config.setIncrementalBackupUrl("http://test/incremental");
        config.setApiToken("token");
        config.setDbName("testdb");
        config.setStorageConfigUuid("storage-uuid");
        config.setUniverseUuid("universe-uuid");
        config.setBackupType("YQL_TABLE_TYPE");

        lenient().when(webClient.post()).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.header(anyString(), anyString())).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.contentType(any())).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private Map<String, Object> createBatchParams() {
        Map<String, Object> map = new HashMap<>();
        map.put(AppConstants.BATCH_ID, "batch1");
        map.put(AppConstants.CATEGORY_CODE, "FULL_BACKUP");
        map.put(AppConstants.BUSINESS_DATE, new Date());
        map.put(AppConstants.CRON_EXPRESSION, "0 0 2 1 * *");  // Monthly: 1st of month at 2 AM
        return map;
    }


    @Test
    void shouldPerformFullBackupSuccessfully() throws Exception {

        JsonNode response = mapper.readTree("{\"taskUUID\":\"task-123\"}");

        when(configService.resolve(anyString())).thenReturn(config);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
        when(backupPollerService.pollFullBackupCompletion(
                any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just("base-uuid"));

        // Mock DAO method
        doNothing().when(backupDaoService).insertFullBackupRecord(
                any(), anyString(), anyString(), anyString(), anyString(), anyString());

        Mono<String> result = ybaClient.backupInitiate("FULL_BACKUP", createBatchParams());

        StepVerifier.create(result)
                .expectNextMatches(json -> json.contains("task-123"))
                .verifyComplete();

        verify(backupDaoService).insertFullBackupRecord(
                any(), anyString(), anyString(), eq("task-123"), anyString(), anyString());
    }


    @Test
    void shouldPerformIncrementalBackupSuccessfully() throws Exception {

        config.setBackupCategoryType(AppConstants.INCREMENTAL_BACKUP);

        JsonNode response = mapper.readTree("{\"taskUUID\":\"task-456\"}");

        when(configService.resolve(anyString())).thenReturn(config);
        when(backupDaoService.getBaseBackupUuidFromDb(anyString(), anyString()))
                .thenReturn("base-uuid");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
        when(backupPollerService.pollIncrementalBackupCompletion(
                any(), anyString(), anyString(), anyString(), anyString(), anyString(),anyString()))
                .thenReturn(Mono.empty());

        // Mock DAO method
        doNothing().when(backupDaoService).insertIncrementalBackupRecord(
                anyString(), anyString(), any(), anyString(), anyString());

        Mono<String> result = ybaClient.backupInitiate("INC_BACKUP", createBatchParams());

        StepVerifier.create(result)
                .expectNextMatches(json -> json.contains("task-456"))
                .verifyComplete();

        verify(backupDaoService).insertIncrementalBackupRecord(
                anyString(), anyString(), any(), anyString(), anyString());
    }

    @Test
    void shouldFailIfBaseUuidMissing() {

        config.setBackupCategoryType(AppConstants.INCREMENTAL_BACKUP);

        when(configService.resolve(anyString())).thenReturn(config);
        when(backupDaoService.getBaseBackupUuidFromDb(anyString(), anyString()))
                .thenReturn("");

        Mono<String> result = ybaClient.backupInitiate("INC_BACKUP", createBatchParams());

        StepVerifier.create(result)
                .expectError(IllegalStateException.class)
                .verify();
    }


    @Test
    void shouldFailForUnsupportedBackupType() {

        config.setBackupCategoryType("UNKNOWN");

        when(configService.resolve(anyString())).thenReturn(config);

        Mono<String> result = ybaClient.backupInitiate("UNKNOWN", createBatchParams());

        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldHandleWeeklyCronExpression() throws Exception {
        Map<String, Object> params = createBatchParams();
        params.put(AppConstants.CRON_EXPRESSION, "0 0 2 * * MON");  // Weekly: Every Monday at 2 AM

        JsonNode response = mapper.readTree("{\"taskUUID\":\"task-weekly\"}");
        when(configService.resolve(anyString())).thenReturn(config);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
        when(backupPollerService.pollFullBackupCompletion(
                any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just("base-uuid-weekly"));

        // Mock DAO method
        doNothing().when(backupDaoService).insertFullBackupRecord(
                any(), anyString(), anyString(), anyString(), anyString(), anyString());

        Mono<String> result = ybaClient.backupInitiate("FULL_BACKUP", params);

        StepVerifier.create(result)
                .expectNextMatches(json -> json.contains("task-weekly"))
                .verifyComplete();
    }

    @Test
    void shouldHandleCustomDateCronExpression() throws Exception {
        Map<String, Object> params = createBatchParams();
        params.put(AppConstants.CRON_EXPRESSION, "0 0 3 15 6 *");  // Custom: June 15 at 3 AM

        JsonNode response = mapper.readTree("{\"taskUUID\":\"task-custom\"}");
        when(configService.resolve(anyString())).thenReturn(config);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
        when(backupPollerService.pollFullBackupCompletion(
                any(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just("base-uuid-custom"));

        // Mock DAO method
        doNothing().when(backupDaoService).insertFullBackupRecord(
                any(), anyString(), anyString(), anyString(), anyString(), anyString());

        Mono<String> result = ybaClient.backupInitiate("FULL_BACKUP", params);

        StepVerifier.create(result)
                .expectNextMatches(json -> json.contains("task-custom"))
                .verifyComplete();
    }

    @Test
    void shouldHandleIncrementalBackupWithWeeklyCron() throws Exception {
        config.setBackupCategoryType(AppConstants.INCREMENTAL_BACKUP);

        Map<String, Object> params = createBatchParams();
        params.put(AppConstants.CRON_EXPRESSION, "0 0 2 * * MON");  // Weekly: Every Monday at 2 AM

        JsonNode response = mapper.readTree("{\"taskUUID\":\"task-inc-weekly\"}");
        when(configService.resolve(anyString())).thenReturn(config);
        when(backupDaoService.getBaseBackupUuidFromDb(anyString(), anyString()))
                .thenReturn("base-uuid-weekly");
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(response));
        when(backupPollerService.pollIncrementalBackupCompletion(
                any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        // Mock DAO method
        doNothing().when(backupDaoService).insertIncrementalBackupRecord(
                anyString(), anyString(), any(), anyString(), anyString());

        Mono<String> result = ybaClient.backupInitiate("INC_BACKUP", params);

        StepVerifier.create(result)
                .expectNextMatches(json -> json.contains("task-inc-weekly"))
                .verifyComplete();
    }

    @Test
    void shouldHandleMissingCronExpression() throws Exception {
        Map<String, Object> params = createBatchParams();
        params.remove(AppConstants.CRON_EXPRESSION);  // Remove cron expression

        // When cron expression is missing, it should throw an error
        when(configService.resolve(anyString())).thenReturn(config);

        Mono<String> result = ybaClient.backupInitiate("FULL_BACKUP", params);

        // Expect error when cron expression is missing
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("cronExpression is required"))
                .verify();
    }

    @Test
    void shouldHandleApiError() {
        when(configService.resolve(anyString())).thenReturn(config);
        when(responseSpec.bodyToMono(JsonNode.class))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        Mono<String> result = ybaClient.backupInitiate("FULL_BACKUP", createBatchParams());

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldHandleEmptyBaseUuid() {
        config.setBackupCategoryType(AppConstants.INCREMENTAL_BACKUP);

        when(configService.resolve(anyString())).thenReturn(config);

        // Mock the DAO to return empty string for base UUID
        when(backupDaoService.getBaseBackupUuidFromDb(anyString(), anyString()))
                .thenReturn("");

        Mono<String> result = ybaClient.backupInitiate("INC_BACKUP", createBatchParams());

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalStateException &&
                        throwable.getMessage().contains("Base backup UUID not found"))
                .verify();
    }

    @Test
    void shouldHandleInvalidCronExpression() {
        Map<String, Object> params = createBatchParams();
        params.put(AppConstants.CRON_EXPRESSION, "INVALID_CRON");  // Invalid cron expression

        when(configService.resolve(anyString())).thenReturn(config);

        Mono<String> result = ybaClient.backupInitiate("FULL_BACKUP", params);

        // Expect error when cron expression is invalid
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("Invalid cron expression"))
                .verify();
    }
}