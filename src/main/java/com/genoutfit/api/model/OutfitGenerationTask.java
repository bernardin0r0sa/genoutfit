package com.genoutfit.api.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the status of an outfit generation task
 */
@Getter
public class OutfitGenerationTask {
    private final String outfitId;
    private final List<String> prompts;
    private final List<String> requestIds;
    private final List<String> generatedImageUrls;
    private final List<String> errorMessages;
    private final AtomicInteger completedCount = new AtomicInteger(0);

    public OutfitGenerationTask(String outfitId, List<String> prompts) {
        this.outfitId = outfitId;
        this.prompts = new ArrayList<>(prompts);
        this.requestIds = new ArrayList<>(prompts.size());
        this.generatedImageUrls = new ArrayList<>(prompts.size());
        this.errorMessages = new ArrayList<>(prompts.size());

        // Initialize with nulls
        for (int i = 0; i < prompts.size(); i++) {
            requestIds.add(null);
            generatedImageUrls.add(null);
            errorMessages.add(null);
        }
    }

    public void setRequestId(int index, String requestId) {
        if (index >= 0 && index < requestIds.size()) {
            requestIds.set(index, requestId);
        }
    }

    public void setImageComplete(int index, String imageUrl) {
        if (index >= 0 && index < generatedImageUrls.size()) {
            generatedImageUrls.set(index, imageUrl);
            completedCount.incrementAndGet();
        }
    }

    public void setImageError(int index, String errorMessage) {
        if (index >= 0 && index < errorMessages.size()) {
            errorMessages.set(index, errorMessage);
            completedCount.incrementAndGet();
        }
    }

    public boolean isComplete() {
        return completedCount.get() >= prompts.size();
    }

    public OutfitGenerationStatus getStatus() {
        boolean isComplete = isComplete();
        List<String> currentUrls = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < prompts.size(); i++) {
            String imageUrl = generatedImageUrls.get(i);
            String error = errorMessages.get(i);

            // Add current image URL (may be null)
            currentUrls.add(imageUrl);

            // Add errors if present
            if (error != null) {
                errors.add(String.format("Error with image %d: %s", i, error));
            }
        }

        return new OutfitGenerationStatus(isComplete, currentUrls, errors);
    }
}