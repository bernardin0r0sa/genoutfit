package com.genoutfit.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Apply to all endpoints
                .allowedOrigins(
                        "https://www.genoutfit.com",  // Production web domain
                        "https://genoutfit.com",  // Production web domain
                        "https://693a-2607-fea8-86e4-fa00-f464-e4d0-7f5f-c6dd.ngrok-free.app",  // Mobile/Development temporary domain
                        "https://localhost:8080"  // Local development
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // Allowed HTTP methods
                .allowCredentials(true)  // Allow sending cookies cross-origin
                .maxAge(3600);  // Cache CORS preflight response for 1 hour
    }
}