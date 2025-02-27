package com.genoutfit.api.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.genoutfit.api.model.Outfit;
import com.genoutfit.api.repository.OutfitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple service to refresh R2 image URLs before they expire
 */
@Service
@Slf4j
public class UrlRefresher {

    @Autowired
    private R2StorageService r2StorageService;

    @Autowired
    private OutfitRepository outfitRepository;

    @Value("${R2_BUCKET_NAME}")
    private String bucketName;

    // Pattern to extract the key from the URL
    private static final Pattern KEY_PATTERN = Pattern.compile(".*\\.com/([^?]+)");

    /**
     * Scheduled task that runs weekly to refresh all R2 URLs
     * Runs every Sunday at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * 0")
    public void refreshAllR2Urls() {
        log.info("Starting weekly R2 URL refresh");
        int updatedUrls = 0;

        try {
            // Get all outfits
            List<Outfit> allOutfits = outfitRepository.findAll();

            for (Outfit outfit : allOutfits) {
                boolean outfitUpdated = false;
                List<String> updatedImageUrls = new ArrayList<>();

                // Process each URL in the outfit
                for (String imageUrl : outfit.getImageUrls()) {
                    if (imageUrl != null && imageUrl.contains(".r2.cloudflarestorage.com")) {
                        // Extract the key from the URL
                        String key = extractKeyFromUrl(imageUrl);
                        if (key != null) {
                            // Generate a new URL with fresh expiration
                            String newUrl = refreshUrl(key);
                            updatedImageUrls.add(newUrl);
                            updatedUrls++;
                            outfitUpdated = true;
                        } else {
                            // Keep the original URL if we can't extract the key
                            updatedImageUrls.add(imageUrl);
                        }
                    } else {
                        // Not an R2 URL, keep as is
                        updatedImageUrls.add(imageUrl);
                    }
                }

                // Update and save the outfit if needed
                if (outfitUpdated) {
                    outfit.setImageUrls(updatedImageUrls);
                    outfitRepository.save(outfit);
                }
            }

            log.info("Successfully refreshed {} R2 URLs", updatedUrls);
        } catch (Exception e) {
            log.error("Error refreshing R2 URLs: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract the object key from an R2 URL
     */
    private String extractKeyFromUrl(String url) {
        Matcher matcher = KEY_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Generate a new presigned URL for the given key
     */
    private String refreshUrl(String key) {
        try {
            AmazonS3 r2Client = r2StorageService.getR2Client();

            // Create a URL with 7-day expiration
            Date expiration = new Date();
            expiration.setTime(expiration.getTime() + 7L * 24L * 60L * 60L * 1000L); // 7 days

            GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(bucketName, key)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);

            URL presignedUrl = r2Client.generatePresignedUrl(urlRequest);
            return presignedUrl.toString();
        } catch (Exception e) {
            log.error("Error generating new URL for key {}: {}", key, e.getMessage());
            throw new RuntimeException("Failed to refresh URL", e);
        }
    }

    /**
     * Manually trigger a URL refresh (can be called from a controller)
     */
    public int manualRefresh() {
        log.info("Manual R2 URL refresh triggered");
        int beforeCount = 0;
        refreshAllR2Urls();
        return beforeCount;
    }
}