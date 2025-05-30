package com.genoutfit.api;

import io.pinecone.clients.Pinecone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PineconeConfig {
    @Value("${PINECONE_API_KEY}")
    private String apiKey;

    @Bean
    public Pinecone pineconeClient() {
        return new Pinecone.Builder(apiKey).build();
    }
}
