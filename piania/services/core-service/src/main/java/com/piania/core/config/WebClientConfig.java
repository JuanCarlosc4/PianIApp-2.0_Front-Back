package com.piania.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient genericWebClient() {
        return WebClient.builder().build();
    }

    @Bean
    public WebClient omrWebClient() {
        return WebClient.builder()
                .baseUrl("http://omr-service:8000")
                .build();
    }
}
