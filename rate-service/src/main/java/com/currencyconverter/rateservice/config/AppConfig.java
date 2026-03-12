package com.currencyconverter.rateservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder, ExchangeRateApiConfig apiConfig) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, apiConfig.getConnectTimeout())
                .responseTimeout(Duration.ofMillis(apiConfig.getReadTimeout()))
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(apiConfig.getReadTimeout(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(apiConfig.getReadTimeout(), TimeUnit.MILLISECONDS)));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(apiConfig.getUrl())
                .build();
    }
}
