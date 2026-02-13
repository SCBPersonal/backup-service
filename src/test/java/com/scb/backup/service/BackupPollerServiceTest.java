package com.scb.backup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scb.backup.config.BackupPollerProperties;
import com.scb.backup.dao.BackupDaoService;
import com.scb.backup.model.YbaDynamicConfig;
import com.scb.backup.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackupPollerService Comprehensive Tests")
class BackupPollerServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private BackupDaoService backupDaoService;

    @Mock
    private BackupPollerProperties pollerProperties;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private BackupPollerService backupPollerService;

    private YbaDynamicConfig config;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();

        config = YbaDynamicConfig.builder()
                .jobCompletionCheckUrl("http://yba-api/task-status/{taskUuid}")
                .lastBackupUrl("http://yba-api/last-backup")
                .apiToken("test-api-token")
                .universeUuid("universe-uuid-123")
                .dbName("test_db")
                .build();

        when(pollerProperties.isEnabled()).thenReturn(true);
        when(pollerProperties.getInitialDelayMs()).thenReturn(100L);
        when(pollerProperties.getPollingIntervalMs()).thenReturn(100L);
    }

    @Test
    @DisplayName("Should successfully poll full backup completion after few attempts")
    void should_PollFullBackupCompletion_When_JobSucceeds() throws Exception {
        // Given
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
        String backupMonth = "2026-02";
        String taskUuid = "task-uuid-123";
        String batchId = "BATCH_001";
        String baseUuid = "base-uuid-456";

        // Simulate polling: First 2 attempts return "Running", 3rd attempt returns "Success"
        JsonNode runningStatusResponse = mapper.readTree("{\"status\":\"Running\"}");
        JsonNode successStatusResponse = mapper.readTree("{\"status\":\"Success\"}");
        JsonNode lastBackupResponse = mapper.readTree(
                "{\"entities\":[{\"taskUUID\":\"" + taskUuid + "\",\"commonBackupInfo\":{\"baseBackupUUID\":\"" + baseUuid + "\"},\"backupUUID\":\"backup-uuid-789\"}]}"
        );

        // Use AtomicInteger to track poll attempts and return different responses
        AtomicInteger pollCount = new AtomicInteger(0);

        // Mock task status check - simulate 2 polls with "Running", then "Success"
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Use thenAnswer to return different responses based on call count
        when(responseSpec.bodyToMono(JsonNode.class)).thenAnswer((Answer<Mono<JsonNode>>) invocation -> {
            int count = pollCount.incrementAndGet();
            if (count <= 2) {
                return Mono.just(runningStatusResponse);  // First 2 calls: Running
            } else if (count == 3) {
                return Mono.just(successStatusResponse);  // 3rd call: Success
            } else {
                return Mono.just(lastBackupResponse);     // 4th call: last backup response
            }
        });

        // Mock last backup fetch
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);

        doNothing().when(backupDaoService).updateFullBackupWithBaseUuid(anyString(), anyString(), anyString(), anyString(), anyString());

        // When
        Mono<String> result = backupPollerService.pollFullBackupCompletion(config, categoryCode, backupMonth, taskUuid, batchId);

        // Then
        StepVerifier.create(result)
                .expectNext(baseUuid)
                .verifyComplete();

        // Verify that polling happened 3 times (2 Running + 1 Success) + 1 for last backup
        verify(webClient, atLeast(3)).get();
        verify(backupDaoService).updateFullBackupWithBaseUuid(eq(categoryCode), eq(backupMonth), eq(baseUuid), eq(batchId), eq(AppConstants.BACKUP_SUCCESS_STATUS));
    }

    @Test
    @DisplayName("Should handle full backup failure after few polling attempts")
    void should_HandleFullBackupFailure_When_JobFails() throws Exception {
        // Given
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
        String backupMonth = "2026-02";
        String taskUuid = "task-uuid-123";
        String batchId = "BATCH_001";

        // Simulate polling: First 2 attempts return "Running", 3rd attempt returns "Failure"
        JsonNode runningStatusResponse = mapper.readTree("{\"status\":\"Running\"}");
        JsonNode failureStatusResponse = mapper.readTree("{\"status\":\"Failure\"}");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class))
                .thenReturn(
                        Mono.just(runningStatusResponse),  // 1st poll: Running
                        Mono.just(runningStatusResponse),  // 2nd poll: Running
                        Mono.just(failureStatusResponse)   // 3rd poll: Failure
                );

        doNothing().when(backupDaoService).updateFullBackupWithBaseUuid(anyString(), anyString(), anyString(), anyString(), anyString());

        // When
        Mono<String> result = backupPollerService.pollFullBackupCompletion(config, categoryCode, backupMonth, taskUuid, batchId);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Job failed with status: Failure"))
                .verify();

        // Verify that polling happened multiple times (at least 3 times: 2 Running + 1 Failure)
        verify(webClient, atLeast(3)).get();
        verify(backupDaoService).updateFullBackupWithBaseUuid(eq(categoryCode), eq(backupMonth), eq(""), eq(batchId), eq(AppConstants.BACKUP_FAILED_STATUS));
    }

    @Test
    @DisplayName("Should poll multiple times before getting success status")
    void should_PollMultipleTimes_When_BackupTakesTimeToComplete() throws Exception {
        // Given
        String categoryCode = "HWA_EPR_DB_BACKUP_FULL";
        String backupMonth = "2026-02";
        String taskUuid = "task-uuid-123";
        String batchId = "BATCH_001";
        String baseUuid = "base-uuid-456";

        // Simulate realistic polling: 5 attempts with "Running", then "Success"
        JsonNode runningStatusResponse = mapper.readTree("{\"status\":\"Running\"}");
        JsonNode successStatusResponse = mapper.readTree("{\"status\":\"Success\"}");
        JsonNode lastBackupResponse = mapper.readTree(
                "{\"entities\":[{\"taskUUID\":\"" + taskUuid + "\",\"commonBackupInfo\":{\"baseBackupUUID\":\"" + baseUuid + "\"},\"backupUUID\":\"backup-uuid-789\"}]}"
        );

        // Mock task status check - simulate 5 polls with "Running", then "Success"
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class))
                .thenReturn(
                        Mono.just(runningStatusResponse),  // 1st poll: Running
                        Mono.just(runningStatusResponse),  // 2nd poll: Running
                        Mono.just(runningStatusResponse),  // 3rd poll: Running
                        Mono.just(runningStatusResponse),  // 4th poll: Running
                        Mono.just(runningStatusResponse),  // 5th poll: Running
                        Mono.just(successStatusResponse)   // 6th poll: Success
                );

        // Mock last backup fetch
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class))
                .thenReturn(Mono.just(lastBackupResponse));

        doNothing().when(backupDaoService).updateFullBackupWithBaseUuid(anyString(), anyString(), anyString(), anyString(), anyString());

        // When
        Mono<String> result = backupPollerService.pollFullBackupCompletion(config, categoryCode, backupMonth, taskUuid, batchId);

        // Then
        StepVerifier.create(result)
                .expectNext(baseUuid)
                .verifyComplete();

        // Verify that polling happened 6 times (5 Running + 1 Success)
        verify(webClient, atLeast(6)).get();
        verify(backupDaoService).updateFullBackupWithBaseUuid(eq(categoryCode), eq(backupMonth), eq(baseUuid), eq(batchId), eq(AppConstants.BACKUP_SUCCESS_STATUS));
    }
}
