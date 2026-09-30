package com.finsight.finsight_api.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {

    /** Base URL of the Python ML microservice. */
    @Value("${python.ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    /** How long we wait for the TCP connection to be established. */
    @Value("${python.ml.service.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    /** How long we wait for the response once the request has been sent. */
    @Value("${python.ml.service.response-timeout-ms:30000}")
    private long responseTimeoutMs;

    /**
     * Inference plus SHAP can return a large payload, so the default 256KB
     * in-memory buffer of the codecs is not enough.
     */
    @Value("${python.ml.service.max-response-size-bytes:4194304}")
    private int maxResponseSizeBytes;

    /**
     * Shared secret that authenticates this API against the microservice. The
     * microservice must reject any request that does not carry it, otherwise
     * anyone who can reach it can run inference on it.
     *
     * Empty by default so local development works without the microservice, but
     * it MUST be set in any deployment where the service is not on localhost.
     */
    @Value("${python.ml.service.api-key:}")
    private String mlServiceApiKey;

    /** Header carrying the shared secret. Matches FastAPI's APIKeyHeader default. */
    @Value("${python.ml.service.api-key-header:X-API-Key}")
    private String mlServiceApiKeyHeader;

    @Bean
    public WebClient mlWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(responseTimeoutMs, TimeUnit.MILLISECONDS)));

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(mlServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(maxResponseSizeBytes));

        if (StringUtils.hasText(mlServiceApiKey)) {
            // The secret itself is never logged.
            builder.defaultHeader(mlServiceApiKeyHeader, mlServiceApiKey);
        } else {
            log.warn("python.ml.service.api-key is not set: calls to the ML microservice at {} "
                    + "will be unauthenticated. Set it before deploying.", mlServiceUrl);
        }

        return builder.build();
    }
}
