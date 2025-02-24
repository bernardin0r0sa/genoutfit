package com.genoutfit.api.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.genoutfit.api.R2Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class R2StorageService {
    @Value("${R2_BUCKET_NAME}")
    private String bucketName;

    @Autowired
    private R2Config r2Config;

    private AmazonS3 getR2Client() {
        try {
            return r2Config.r2Client();
        } catch (Exception e) {
            log.error("Failed to initialize R2 client: {}", e.getMessage());
            throw new RuntimeException("R2 client initialization failed", e);
        }
    }

    /**
     * Downloads an image from a URL and uploads it to R2 storage
     *
     * @param imageUrl The URL of the image to download
     * @param userId The user ID for the file path
     * @param occasion The occasion for the file path
     * @return The permanent URL of the stored image
     * @throws Exception If download or upload fails
     */
    public String uploadFile(String imageUrl, String userId, String occasion) throws Exception {
        try {
            AmazonS3 r2Client = getR2Client();

            // Generate a unique filename
            String fileName = UUID.randomUUID().toString() + ".jpg";

            // Create the storage path: userId/occasion/filename
            String key = String.format("%s/%s/%s", userId, occasion, fileName);

            log.info("Downloading image from URL: {}", imageUrl);

            // Download the image
            URL url = new URL(imageUrl);
            byte[] imageBytes;
            try (InputStream inputStream = url.openStream()) {
                imageBytes = inputStream.readAllBytes();
            }

            // Prepare metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg");
            metadata.setContentLength(imageBytes.length);

            // Upload to R2
            try (InputStream uploadStream = new ByteArrayInputStream(imageBytes)) {
                r2Client.putObject(bucketName, key, uploadStream, metadata);
                log.info("Successfully uploaded image to R2: {}", key);
            }

            // Generate a presigned URL with a long but reasonable expiration (1 year)
            // For an MVP with 100 users, this is a practical balance between security and simplicity
            Date expiration = new Date();
            expiration.setTime(expiration.getTime() + 365L * 24L * 60L * 60L * 1000L); // 1 year

            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, key)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);

            URL presignedUrl = r2Client.generatePresignedUrl(generatePresignedUrlRequest);
            return presignedUrl.toString();

        } catch (Exception e) {
            log.error("Failed to process image from URL {}: {}", imageUrl, e.getMessage());
            throw new Exception("Failed to process and upload image", e);
        }
    }

    public void deleteFile(String key) throws Exception {
        try {
            AmazonS3 r2Client = getR2Client();
            r2Client.deleteObject(bucketName, key);
            log.info("Successfully deleted file from R2: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file from R2: {}", e.getMessage());
            throw new Exception("Failed to delete file", e);
        }
    }
}