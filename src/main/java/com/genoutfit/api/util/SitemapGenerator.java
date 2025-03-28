package com.genoutfit.api.util;

import com.genoutfit.api.model.ProgrammaticPage;
import com.genoutfit.api.repository.ProgrammaticPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to generate and periodically update the sitemap.xml file
 * for SEO optimization.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SitemapGenerator {

    private final ProgrammaticPageRepository programmaticPageRepository;

    @Value("${BASE_URL}")
    private String baseUrl;

    @Value("${sitemap.directory:src/main/resources/static}")
    private String sitemapDirectory;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Static routes that should be included in the sitemap
     */
    private final List<String> staticRoutes = List.of(
            "/",
            "/home",
            "/login",
            "/register",
            "/onboard",
            "/terms",
            "/privacy",
            "/plus-size-outfit-ideas",
            "/outfit-ideas-for-women",
            "/outfit-ideas"
    );

    /**
     * Generate the sitemap at application startup
     */
    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void generateInitialSitemap() {
        generateSitemap();
    }

    /**
     * Update the sitemap on the first day of each month at 2 AM
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void updateMonthlySitemap() {
        log.info("Running scheduled monthly sitemap update");
        generateSitemap();
    }

    /**
     * Manually trigger sitemap generation
     */
    public void generateSitemap() {
        try {
            log.info("Starting sitemap generation...");

            String today = LocalDateTime.now().format(DATE_FORMATTER);
            List<String> sitemapEntries = new ArrayList<>();

            // XML header
            sitemapEntries.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sitemapEntries.add("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

            // Add static routes
            for (String route : staticRoutes) {
                sitemapEntries.add(createUrlEntry(route, "0.8", today, "monthly"));
            }

            // Add dynamic programmatic pages
            addProgrammaticPages(sitemapEntries, today);

            // Close XML
            sitemapEntries.add("</urlset>");

            // Write to temporary file first
            Path tempFile = Paths.get(sitemapDirectory, "sitemap_temp.xml");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile.toFile()))) {
                for (String line : sitemapEntries) {
                    writer.write(line);
                    writer.newLine();
                }
            }

            // Move to final destination atomically
            Path finalFile = Paths.get(sitemapDirectory, "sitemap.xml");
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("Sitemap generation completed successfully. Generated {} URLs", sitemapEntries.size() - 2);
        } catch (Exception e) {
            log.error("Error generating sitemap: {}", e.getMessage(), e);
        }
    }

    /**
     * Add all programmatic pages to the sitemap
     */
    private void addProgrammaticPages(List<String> sitemapEntries, String today) {
        // Fetch all active programmatic pages
        List<ProgrammaticPage> allPages = programmaticPageRepository.findAll()
                .stream()
                .filter(ProgrammaticPage::isActive)
                .toList();

        log.info("Found {} active programmatic pages", allPages.size());

        for (ProgrammaticPage page : allPages) {
            // Calculate priority based on search volume
            // Higher search volume pages get higher priority (max 0.7)
            double priority = Math.min(0.3 + (page.getSearchVolume() / 10000.0), 0.7);
            String priorityStr = String.format("%.1f", priority);

            sitemapEntries.add(createUrlEntry("/" + page.getSlug(), priorityStr, today, "monthly"));
        }
    }

    /**
     * Create a properly formatted URL entry for the sitemap
     */
    private String createUrlEntry(String path, String priority, String lastmod, String changefreq) {
        StringBuilder entry = new StringBuilder();
        entry.append("  <url>");
        entry.append("\n    <loc>").append(baseUrl).append(path).append("</loc>");
        entry.append("\n    <lastmod>").append(lastmod).append("</lastmod>");
        entry.append("\n    <changefreq>").append(changefreq).append("</changefreq>");
        entry.append("\n    <priority>").append(priority).append("</priority>");
        entry.append("\n  </url>");
        return entry.toString();
    }
}