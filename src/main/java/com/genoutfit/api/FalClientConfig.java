package com.genoutfit.api;

import ai.fal.client.FalClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FalClientConfig {

    @Value("${fal.api.key}")
    private String falApiKey;

    @Bean
    public FalClient falClient() {
        return FalClient.withEnvCredentials();
    }
}