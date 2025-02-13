package com.genoutfit.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.datasource.url", havingValue = "null", matchIfMissing = true)
    // Only create this bean if spring.datasource.url is not set
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName(System.getenv("DATABASE_DRIVER"))
                .url(System.getenv("DATABASE_URL"))
                .username(System.getenv("DATABASE_USERNAME"))
                .password(System.getenv("DATABASE_PASSWORD"))
                .build();
    }
}