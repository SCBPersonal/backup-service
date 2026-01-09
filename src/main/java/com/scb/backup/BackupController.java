package com.scb.backup;

import com.scb.backup.exception.DbBackupException;
import com.scb.epricing.batch.core.lib.model.BatchStartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import com.scb.backup.service.BackupService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BackupController {

    @Autowired
    private BackupService backupService;

    @PostMapping("/backupProcess")
    public Mono<BatchStartResponse> backupProcess(@RequestBody String json){
        log.info("backup request received for db backup for job Type-{}",json);
        return Mono.fromCallable(()->{
            try {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                var response = backupService.execute(json);
                stopWatch.stop();
                log.info("Completion time - {} sec");
                return response;
            } catch (Exception e) {
                log.error("Error occurred during file Transfer : ", e);
                throw new DbBackupException("Error occurred during backup process: ",e);

            }
        }) .doOnError(throwable -> log.error("Backup process failed - RequestID: {}", throwable));
    }

}
