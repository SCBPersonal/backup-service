package com.scb.backup.config;

import com.scb.backup.model.YbaDynamicConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "yba")
@Validated
public class YbaProperties {

    private Map<String, YbaDynamicConfig> databases;


    @Value("${yba.connection.timeout:30000}")
    private int connectionTimeout;

    @Value("${yba.read.timeout:60000}")
    private int readTimeout;

    @Value("${yba.retry.max-attempts:3}")
    private int maxRetryAttempts;
}
