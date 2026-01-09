package com.scb.backup.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Autowired
    private YbaProperties ybaProperties;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ybaProperties.getConnectionTimeout())
                                .responseTimeout(Duration.ofMillis(ybaProperties.getReadTimeout()))
                                .doOnConnected(conn ->
                                        conn.addHandlerLast(new ReadTimeoutHandler(ybaProperties.getReadTimeout(), TimeUnit.MILLISECONDS))
                                                .addHandlerLast(new WriteTimeoutHandler(ybaProperties.getReadTimeout(), TimeUnit.MILLISECONDS)))
                ))
                .filter(ExchangeFilterFunction.ofRequestProcessor(this::logRequest))
                .filter(ExchangeFilterFunction.ofResponseProcessor(this::logResponse))
                .build();
    }

    private Mono<ClientRequest> logRequest(ClientRequest request) {
        log.debug("Request: {} {}", request.method(), request.url());
        return Mono.just(request);
    }

    private Mono<ClientResponse> logResponse(ClientResponse response) {
        log.debug("Response Status: {}", response.statusCode());
        return Mono.just(response);
    }
}
