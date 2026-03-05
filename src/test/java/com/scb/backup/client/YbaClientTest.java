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
        map.put(AppConstants.BUSINESS_DATE, new Date());
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

        Mono<String> result = ybaClient.backupInitiate("FULL_BACKUP", createBatchParams());

        StepVerifier.create(result)
                .expectNextMatches(json -> json.contains("task-123"))
                .verifyComplete();

        verify(backupDaoService).insertFullBackupRecord(any(), anyString(), eq("task-123"), anyString(), anyString());
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

        Mono<String> result = ybaClient.backupInitiate("INC_BACKUP", createBatchParams());

        StepVerifier.create(result)
                .expectNextMatches(json -> json.contains("task-456"))
                .verifyComplete();

        verify(backupDaoService).insertIncrementalBackupRecord(
                anyString(), anyString(), any(), anyString());
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
}