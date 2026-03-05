package com.scb.backup.config;

import com.hdfcbank.epricing.batch.core.lib.config.BatchConfig;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClientConfig - Configuration class for Spring WebClient.
 *
 * This configuration provides a customized WebClient bean for making HTTP requests
 * to the YugabyteDB Anywhere (YBA) REST API. The WebClient is configured with:
 * <ul>
 *   <li>Connection timeout settings</li>
 *   <li>Read/write timeout handlers</li>
 *   <li>Request/response logging filters</li>
 *   <li>Reactive Netty HTTP client connector</li>
 * </ul>
 *
 * <p>Timeout values are loaded from YbaProperties configuration.</p>
 *
 * @author SCB ePricing Team
 * @version 2.0
 * @since 2026-02-04
 * @see YbaProperties
 * @see WebClient
 */
@Slf4j
@Configuration
@EnableAsync
@Import({ BatchConfig.class})
@ComponentScan(basePackages = "com.hdfc.backup")
@RequiredArgsConstructor
public class WebClientConfig {

    @Autowired
    private YbaProperties ybaProperties;



    /**
     * Creates and configures a WebClient bean for YBA API integration.
     *
     * The WebClient is configured with:
     * <ul>
     *   <li>Connection timeout from ybaProperties.connectionTimeout</li>
     *   <li>Response timeout from ybaProperties.readTimeout</li>
     *   <li>Read timeout handler</li>
     *   <li>Write timeout handler</li>
     *   <li>Request logging filter (DEBUG level)</li>
     *   <li>Response logging filter (DEBUG level)</li>
     * </ul>
     *
     * @return Configured WebClient instance for making HTTP requests
     */
    @Bean
    public WebClient webClient() throws SSLException {

        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .secure(sslSpec -> sslSpec.sslContext(sslContext))
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
