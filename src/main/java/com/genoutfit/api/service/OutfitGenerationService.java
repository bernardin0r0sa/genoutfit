package com.genoutfit.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genoutfit.api.RateLimiter;
import com.genoutfit.api.model.*;
import com.genoutfit.api.repository.OutfitRepository;
import com.genoutfit.api.repository.PromptTemplateRepository;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OutfitGenerationService {
    @Autowired
    private PineconeClient pineconeClient;

    @Autowired
    private FalAiClient falAiClient;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private UserService userService;

    @Autowired
    private OutfitRepository outfitRepository;

    @Autowired
    private PromptTemplateRepository promptTemplateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private R2StorageService r2StorageService;

    @Value("${app.generation.max-attempts}")
    private int maxAttempts;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.placeholder-image}")
    private String placeholderImageUrl;

    // Store pending outfit generations
    private final Map<String, OutfitGenerationTask> pendingGenerations = new ConcurrentHashMap<>();

    public OutfitResponseDto initiateOutfitGeneration(String userId, OutfitRequestDto request) throws Exception {
        User user = userService.getUserById(userId);
        List<OutfitVector> similarOutfits = findSimilarOutfits(user, request.getOccasion());
        List<String> prompts = generatePrompts(user, request.getOccasion(), similarOutfits);

        // Create outfit with placeholder images initially
        List<String> placeholderImages = new ArrayList<>();
        for (int i = 0; i < prompts.size(); i++) {
            placeholderImages.add(placeholderImageUrl);
        }

        Outfit outfit = createAndSaveOutfit(user, request.getOccasion(), placeholderImages, similarOutfits);

        // Start async image generation
        startAsyncImageGeneration(outfit.getId(), prompts);

        return mapToOutfitResponse(outfit);
    }

    @Async
    public void startAsyncImageGeneration(String outfitId, List<String> prompts) {
        OutfitGenerationTask task = new OutfitGenerationTask(outfitId, prompts);
        pendingGenerations.put(outfitId, task);

        // Start generating each image
        for (int i = 0; i < prompts.size(); i++) {
            int imageIndex = i;
            CompletableFuture.runAsync(() -> {
                try {
                    generateSingleImage(outfitId, prompts.get(imageIndex), imageIndex);
                } catch (Exception e) {
                    log.error("Error generating image {} for outfit {}: {}",
                            imageIndex, outfitId, e.getMessage());
                    task.setImageError(imageIndex, e.getMessage());
                }
            });
        }
    }

    private void generateSingleImage(String outfitId, String prompt, int imageIndex) throws Exception {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                // Acquire rate limit permission
                rateLimiter.acquirePermission();

                // Prepare webhook URL
                String webhookUrl = baseUrl + "/api/outfits/webhook/" + outfitId + "/" + imageIndex;

                // Prepare generation input
                Map<String, Object> generationInput = new HashMap<>();
                generationInput.put("prompt", prompt);
                generationInput.put("image_size", "portrait_4_3");
                generationInput.put("num_inference_steps", 28);
                generationInput.put("guidance_scale", 7.5);

                // Submit generation request
                JsonObject generationResult = falAiClient.submitToFalApi(
                        "fal-ai/flux/dev",
                        generationInput,
                        webhookUrl
                );

                // If we're using webhooks, we don't need to update here
                // The webhook controller will handle it
                log.info("Image generation request submitted for outfit {} image {}",
                        outfitId, imageIndex);

                // Store request ID for tracking
                String requestId = generationResult.get("request_id").getAsString();
                OutfitGenerationTask task = pendingGenerations.get(outfitId);
                if (task != null) {
                    task.setRequestId(imageIndex, requestId);
                }

                // Break out of retry loop on success
                break;

            } catch (Exception e) {
                log.error("Failed to generate image on attempt {}: {}", attempt + 1, e.getMessage());
                if (attempt == maxAttempts - 1) {
                    throw new RuntimeException("Failed to generate image after " + maxAttempts + " attempts");
                }
                // Add exponential backoff
                try {
                    Thread.sleep((long) Math.pow(2, attempt) * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Image generation interrupted", ie);
                }
            }
        }
    }

    public void handleImageGenerationWebhook(String outfitId, int imageIndex, JsonObject result) {
        try {
            // Extract image URL from result
            String falImageUrl = extractImageUrl(result);

            // Retrieve outfit and user information
            Outfit outfit = outfitRepository.findById(outfitId)
                    .orElseThrow(() -> new RuntimeException("Outfit not found: " + outfitId));

            User user = outfit.getUser();
            String userId = user.getId();
            String occasion = outfit.getOccasion().name().toLowerCase();

            // Upload the image to R2 storage to make it permanent
            String permanentImageUrl = r2StorageService.uploadFile(falImageUrl, userId, occasion);
            log.info("Image stored permanently at: {}", permanentImageUrl);

            // Update outfit in database with the permanent URL
            List<String> currentImages = outfit.getImageUrls();
            if (imageIndex < currentImages.size()) {
                currentImages.set(imageIndex, permanentImageUrl);
                outfit.setImageUrls(currentImages);
                outfitRepository.save(outfit);

                // Update generation task status
                OutfitGenerationTask task = pendingGenerations.get(outfitId);
                if (task != null) {
                    task.setImageComplete(imageIndex, permanentImageUrl);

                    // If all images are complete, remove from pending
                    if (task.isComplete()) {
                        pendingGenerations.remove(outfitId);
                    }
                }

                log.info("Updated image {} for outfit {}", imageIndex, outfitId);
            }
        } catch (Exception e) {
            log.error("Error handling webhook for outfit {} image {}: {}",
                    outfitId, imageIndex, e.getMessage());

            // Update task with error
            OutfitGenerationTask task = pendingGenerations.get(outfitId);
            if (task != null) {
                task.setImageError(imageIndex, e.getMessage());
            }
        }
    }

    public OutfitGenerationStatus getOutfitGenerationStatus(String outfitId) {
        OutfitGenerationTask task = pendingGenerations.get(outfitId);
        if (task == null) {
            // Check if outfit exists and all images are non-placeholder
            try {
                Outfit outfit = outfitRepository.findById(outfitId)
                        .orElseThrow(() -> new RuntimeException("Outfit not found"));

                boolean allImagesGenerated = outfit.getImageUrls().stream()
                        .noneMatch(url -> url.equals(placeholderImageUrl));

                if (allImagesGenerated) {
                    return new OutfitGenerationStatus(true, outfit.getImageUrls(), Collections.emptyList());
                } else {
                    // Task is missing but outfit has placeholders - probably an error
                    return new OutfitGenerationStatus(false, outfit.getImageUrls(),
                            Collections.singletonList("Generation in progress"));
                }
            } catch (Exception e) {
                return new OutfitGenerationStatus(false, Collections.emptyList(),
                        Collections.singletonList("Outfit not found"));
            }
        }

        return task.getStatus();
    }

    private String getUserDescription(User user) {
        StringBuilder description = new StringBuilder();

        description.append(user.getGender().name().toLowerCase())
                .append(" model with ")
                .append(user.getEthnicity().getAIPromptDescription())
                .append(", ")
                .append(user.getBodyType().getDisplayName().toLowerCase());

        if (user.getHeight() > 0) {
            description.append(", approximately ")
                    .append(user.getHeight())
                    .append("cm tall");
        }

        return description.toString();
    }

    private List<String> generatePrompts(User user, Occasion occasion, List<OutfitVector> similarOutfits) {
        String userDescription = getUserDescription(user);
        String clothingDescription = formatClothingPieces(similarOutfits.get(0).getClothingPieces());

        List<PromptTemplate> templates = promptTemplateRepository.findByOccasion(occasion);

        return templates.stream()
                .map(template -> formatPrompt(template, userDescription, clothingDescription))
                .collect(Collectors.toList());
    }

    private String formatPrompt(PromptTemplate template, String userDescription, String clothingDescription) {
        String prompt = template.getBasePrompt()
                .replace("{USER_DESCRIPTION}", userDescription)
                .replace("{CLOTHING_DESCRIPTION}", clothingDescription);

        if (template.getPhotographerReference() != null) {
            prompt += " Style inspired by " + template.getPhotographerReference() + ".";
        }

        if (template.isInstagramStyle()) {
            prompt += " Composition optimized for Instagram, with engaging social media aesthetic.";
        }

        return prompt + " " + template.getStyleNotes();
    }

    private List<OutfitVector> findSimilarOutfits(User user, Occasion occasion) {
        Map<String, Object> searchCriteria = new HashMap<>();
        searchCriteria.put("occasion", occasion.name());
        searchCriteria.put("gender", user.getGender().name());
        searchCriteria.put("bodyType", user.getBodyType().name());

        // Add style preferences if available
        if (user.getStylePreferences() != null && !user.getStylePreferences().isEmpty()) {
            searchCriteria.put("style", new ArrayList<>(user.getStylePreferences()));
        }

        try {
            return pineconeClient.search(searchCriteria, 5);
        } catch (Exception e) {
            log.error("Error finding similar outfits: {}", e.getMessage());
            throw new RuntimeException("Failed to find similar outfits", e);
        }
    }

    private String formatClothingPieces(Map<String, List<String>> clothingPieces) {
        if (clothingPieces == null || clothingPieces.isEmpty()) {
            return "a stylish outfit";
        }

        StringBuilder description = new StringBuilder();

        // Start with main pieces
        if (clothingPieces.containsKey("top")) {
            description.append(String.join(" and ", clothingPieces.get("top")));
        }

        if (clothingPieces.containsKey("bottom")) {
            if (description.length() > 0) description.append(" paired with ");
            description.append(String.join(" and ", clothingPieces.get("bottom")));
        }

        // Add outerwear if present
        if (clothingPieces.containsKey("outerwear")) {
            description.append(", topped with ")
                    .append(String.join(" and ", clothingPieces.get("outerwear")));
        }

        // Add shoes
        if (clothingPieces.containsKey("shoes")) {
            description.append(", complemented by ")
                    .append(String.join(" and ", clothingPieces.get("shoes")));
        }

        // Add accessories
        if (clothingPieces.containsKey("accessories")) {
            description.append(", accessorized with ")
                    .append(String.join(", ", clothingPieces.get("accessories")));
        }

        return description.toString();
    }

    private String extractImageUrl(JsonObject result) {
        try {
            return result.getAsJsonArray("images")
                    .get(0)
                    .getAsJsonObject()
                    .get("url")
                    .getAsString();
        } catch (Exception e) {
            log.error("Failed to extract image URL from response: {}", result);
            throw new RuntimeException("Invalid response format from Fal.ai", e);
        }
    }

    private Outfit createAndSaveOutfit(User user, Occasion occasion, List<String> generatedImages,
                                       List<OutfitVector> similarOutfits) {
        try {
            Outfit outfit = new Outfit();
            outfit.setUser(user);
            outfit.setOccasion(occasion);
            outfit.setImageUrls(generatedImages);

            // Set clothing details from the most similar outfit
            if (!similarOutfits.isEmpty()) {
                OutfitVector primaryOutfit = similarOutfits.get(0);
                outfit.setClothingDetails(formatClothingPieces(primaryOutfit.getClothingPieces()));
                outfit.setVectorId(primaryOutfit.getId());
            }

            // Store the prompts used for reference
            List<PromptTemplate> templates = promptTemplateRepository.findByOccasion(occasion);
            String promptsJson = objectMapper.writeValueAsString(
                    templates.stream()
                            .map(PromptTemplate::getBasePrompt)
                            .collect(Collectors.toList())
            );
            outfit.setPromptsUsed(promptsJson);

            return outfitRepository.save(outfit);

        } catch (Exception e) {
            log.error("Error saving outfit: {}", e.getMessage());
            throw new RuntimeException("Failed to save outfit", e);
        }
    }

    private OutfitResponseDto mapToOutfitResponse(Outfit outfit) {
        return OutfitResponseDto.builder()
                .id(outfit.getId())
                .imageUrls(outfit.getImageUrls())
                .occasion(outfit.getOccasion().getDisplayName())
                .clothingDetails(Map.of("description", outfit.getClothingDetails()))
                .createdAt(outfit.getCreatedAt())
                .build();
    }
}