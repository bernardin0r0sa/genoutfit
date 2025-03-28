package com.genoutfit.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for sitemap serving and related MVC configuration
 */
@Configuration
public class SitemapConfig implements WebMvcConfigurer {

    @Value("${sitemap.directory:src/main/resources/static}")
    private String sitemapDirectory;

    /**
     * Configure resource handlers to ensure sitemap.xml is accessible at the root URL
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path sitemapPath = Paths.get(sitemapDirectory).toAbsolutePath().normalize();

        registry.addResourceHandler("/sitemap.xml")
                .addResourceLocations("file:" + sitemapPath.toString() + "/");

        registry.addResourceHandler("/robots.txt")
                .addResourceLocations("file:" + sitemapPath.toString() + "/");
    }
}