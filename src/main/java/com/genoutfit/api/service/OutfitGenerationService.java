package com.genoutfit.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genoutfit.api.RateLimiter;
import com.genoutfit.api.model.*;
import com.genoutfit.api.repository.OutfitHistoryRepository;
import com.genoutfit.api.repository.OutfitRepository;
import com.genoutfit.api.repository.PromptTemplateRepository;
import com.genoutfit.api.repository.UserSubscriptionRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OutfitGenerationService {
    @Autowired
    private OutfitReferenceService outfitReferenceService;

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

    @Autowired
    private OutfitHistoryRepository outfitHistoryRepository;
    @Autowired
    private UserSubscriptionRepository subscriptionRepository;


    @Value("${GENERATION_MAX_ATTEMPTS}")
    private int maxAttempts;

    @Value("${BASE_URL}")
    private String baseUrl;

    @Value("${PLACEHOLDER_IMAGE_URL}")
    private String placeholderImageUrl;

    @Value("${API_WEBHOOK_KEY}")
    private String apiWebhookKey;



    // Store pending outfit generations
    private final Map<String, OutfitGenerationTask> pendingGenerations = new ConcurrentHashMap<>();

    public OutfitResponseDto initiateOutfitGeneration(String userId, OutfitRequestDto request) throws Exception {
        User user = userService.getUserById(userId);

        // Check quota first
        if (!hasRemainingOutfitQuota(userId)) {
            throw new Exception("You've reached your outfit generation limit. Please upgrade your plan for more outfits.");
        }

        List<OutfitVector> similarOutfits = findSimilarOutfits(user, request.getOccasion());

        if (similarOutfits.isEmpty()) {
            throw new RuntimeException("No matching outfits found");
        }

        // Get unused outfits
        List<String> usedOutfitIds = outfitHistoryRepository.findOutfitIdsByUserId(userId);
        List<OutfitVector> availableOutfits = similarOutfits.stream()
                .filter(outfit -> !usedOutfitIds.contains(outfit.getId()))
                .collect(Collectors.toList());

        // If all outfits used, reset history and use all outfits
        if (availableOutfits.isEmpty()) {
            log.info("User {} has seen all outfits, resetting history", userId);
            try {
                resetUserOutfitHistory(userId);
                availableOutfits = similarOutfits;
            } catch (Exception e) {
                log.error("Error resetting user history, using all outfits anyway: {}", e.getMessage());
                availableOutfits = similarOutfits;
            }
        }

        // Select random outfit here, in the main method
        OutfitVector selectedOutfit = availableOutfits.get(
                new Random().nextInt(availableOutfits.size())
        );

        // Record this outfit as shown to user
        OutfitHistory history = new OutfitHistory(userId, selectedOutfit.getId(), request.getOccasion());
        outfitHistoryRepository.save(history);

        log.info("Selected outfit {} for user {}. Recorded in history.",
                selectedOutfit.getId(), userId);

        List<String> prompts = generatePrompts(user, request.getOccasion(), selectedOutfit);

        // Create outfit with placeholder images initially
        List<String> placeholderImages = new ArrayList<>();
        for (int i = 0; i < prompts.size(); i++) {
            placeholderImages.add(placeholderImageUrl);
        }

        Outfit outfit = createAndSaveOutfit(user, request.getOccasion(), placeholderImages, selectedOutfit);

        // Start async image generation
        startAsyncImageGeneration(outfit.getId(), prompts);

        // Decrement quota after successful generation
        useOutfitQuota(userId);

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
                rateLimiter.acquirePermission();

                String webhookUrl = baseUrl + "/api/outfits/webhook/" + outfitId + "/" + imageIndex+"?apiKey="+apiWebhookKey;

                Map<String, Object> generationInput = new HashMap<>();
                generationInput.put("prompt", prompt);
                generationInput.put("image_size", "portrait_4_3");
                generationInput.put("num_inference_steps", 28);
                generationInput.put("guidance_scale", 7.5);

                JsonObject generationResult = falAiClient.submitToFalApi(
                        "fal-ai/flux/dev",
                        generationInput,
                        webhookUrl
                );

                log.info("Image generation request submitted for outfit {} image {}",
                        outfitId, imageIndex);

                String requestId = generationResult.get("request_id").getAsString();
                OutfitGenerationTask task = pendingGenerations.get(outfitId);
                if (task != null) {
                    task.setRequestId(imageIndex, requestId);
                }

                break;

            } catch (Exception e) {
                log.error("Failed to generate image on attempt {}: {}", attempt + 1, e.getMessage());
                if (attempt == maxAttempts - 1) {
                    throw new RuntimeException("Failed to generate image after " + maxAttempts + " attempts");
                }
                Thread.sleep((long) Math.pow(2, attempt) * 1000);
            }
        }
    }

    public void handleImageGenerationWebhook(String outfitId, int imageIndex, JsonObject result) {
        try {
            String falImageUrl = extractImageUrl(result);

            Outfit outfit = outfitRepository.findById(outfitId)
                    .orElseThrow(() -> new RuntimeException("Outfit not found: " + outfitId));

            User user = outfit.getUser();
            String userId = user.getId();
            String occasion = outfit.getOccasion().name().toLowerCase();

            String permanentImageUrl = r2StorageService.uploadFile(falImageUrl, userId, occasion);
            log.info("Image stored permanently at: {}", permanentImageUrl);

            List<String> currentImages = outfit.getImageUrls();
            if (imageIndex < currentImages.size()) {
                currentImages.set(imageIndex, permanentImageUrl);
                outfit.setImageUrls(currentImages);
                outfitRepository.save(outfit);

                OutfitGenerationTask task = pendingGenerations.get(outfitId);
                if (task != null) {
                    task.setImageComplete(imageIndex, permanentImageUrl);

                    if (task.isComplete()) {
                        pendingGenerations.remove(outfitId);
                    }
                }

                log.info("Updated image {} for outfit {}", imageIndex, outfitId);
            }
        } catch (Exception e) {
            log.error("Error handling webhook for outfit {} image {}: {}",
                    outfitId, imageIndex, e.getMessage());

            OutfitGenerationTask task = pendingGenerations.get(outfitId);
            if (task != null) {
                task.setImageError(imageIndex, e.getMessage());
            }
        }
    }

    public OutfitGenerationStatus getOutfitGenerationStatus(String outfitId) {
        OutfitGenerationTask task = pendingGenerations.get(outfitId);
        if (task == null) {
            try {
                Outfit outfit = outfitRepository.findById(outfitId)
                        .orElseThrow(() -> new RuntimeException("Outfit not found"));

                boolean allImagesGenerated = outfit.getImageUrls().stream()
                        .noneMatch(url -> url.equals(placeholderImageUrl));

                // In your backend controller
                if (allImagesGenerated) {
                    return new OutfitGenerationStatus(true, outfit.getImageUrls(), Collections.emptyList());
                } else {
                    // Return empty errors list and use a separate status field
                    return new OutfitGenerationStatus(false, outfit.getImageUrls(), Collections.emptyList());
                }
            } catch (Exception e) {
                return new OutfitGenerationStatus(false, Collections.emptyList(),
                        Collections.singletonList("Outfit not found"));
            }
        }

        return task.getStatus();
    }

    private List<String> generatePrompts(User user, Occasion occasion, OutfitVector selectedOutfit) {

        // Generate prompts using selected outfit
        String userDescription = getUserDescription(user);
        String clothingDescription = formatClothingPieces(selectedOutfit.getClothingPieces());

        List<PromptTemplate> templates = promptTemplateRepository.findByOccasion(occasion);

        return templates.stream()
                .map(template -> formatPrompt(template, userDescription, clothingDescription))
                .collect(Collectors.toList());
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

    private List<OutfitVector> findSimilarOutfits(User user, Occasion occasion) {
        Map<String, Object> searchCriteria = new HashMap<>();
        searchCriteria.put("occasion", occasion.name());
        searchCriteria.put("gender", user.getGender().name());
        searchCriteria.put("bodyType", user.getBodyType().name());

        if (user.getStylePreferences() != null && !user.getStylePreferences().isEmpty()) {
            searchCriteria.put("style", new ArrayList<>(user.getStylePreferences()));
        }

        try {
            return outfitReferenceService.search(searchCriteria, 5);
        } catch (Exception e) {
            log.error("Error finding similar outfits: {}", e.getMessage());
            throw new RuntimeException("Failed to find similar outfits", e);
        }
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

    private String formatClothingPieces(Map<String, List<String>> clothingPieces) {
        if (clothingPieces == null || clothingPieces.isEmpty()) {
            return "a stylish outfit";
        }

        StringBuilder description = new StringBuilder();

        if (clothingPieces.containsKey("top")) {
            description.append(String.join(" and ", clothingPieces.get("top")));
        }

        if (clothingPieces.containsKey("bottom")) {
            if (description.length() > 0) description.append(" paired with ");
            description.append(String.join(" and ", clothingPieces.get("bottom")));
        }

        if (clothingPieces.containsKey("outerwear")) {
            description.append(", topped with ")
                    .append(String.join(" and ", clothingPieces.get("outerwear")));
        }

        if (clothingPieces.containsKey("shoes")) {
            description.append(", complemented by ")
                    .append(String.join(" and ", clothingPieces.get("shoes")));
        }

        if (clothingPieces.containsKey("accessories")) {
            description.append(", accessorized with ")
                    .append(String.join(", ", clothingPieces.get("accessories")));
        }

        return description.toString();
    }

    private String extractImageUrl(JsonObject result) {
        try {
            // Ensure "payload" exists
            if (!result.has("payload") || result.get("payload").isJsonNull()) {
                throw new RuntimeException("Missing or null 'payload' in response");
            }

            JsonObject payload = result.getAsJsonObject("payload");

            // Ensure "images" exists inside "payload"
            if (!payload.has("images") || payload.get("images").isJsonNull()) {
                throw new RuntimeException("Missing or null 'images' array in payload");
            }

            JsonArray images = payload.getAsJsonArray("images");

            // Ensure "images" array is not empty
            if (images.size() == 0) {
                throw new RuntimeException("Empty 'images' array in payload");
            }

            // Extract URL
            return images.get(0).getAsJsonObject().get("url").getAsString();
        } catch (Exception e) {
            log.error("Failed to extract image URL from response: {}", result);
            throw new RuntimeException("Invalid response format from Fal.ai", e);
        }
    }

    private Outfit createAndSaveOutfit(User user, Occasion occasion, List<String> generatedImages,
                                       OutfitVector selectedOutfit) {
        try {
            Outfit outfit = new Outfit();
            outfit.setUser(user);
            outfit.setOccasion(occasion);
            outfit.setImageUrls(generatedImages);

            // Use the selected outfit directly
            outfit.setClothingDetails(formatClothingPieces(selectedOutfit.getClothingPieces()));
            outfit.setVectorId(selectedOutfit.getId());

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

    // Method to get count of unused outfits
    public int getUnusedOutfitCount(String userId, Occasion occasion) throws Exception {
        List<OutfitVector> allOutfits = findSimilarOutfits(userService.getUserById(userId), occasion);
        List<String> usedOutfitIds = outfitHistoryRepository.findOutfitIdsByUserId(userId);

        int unusedCount = (int) allOutfits.stream()
                .filter(outfit -> !usedOutfitIds.contains(outfit.getId()))
                .count();

        log.info("User {} has {} unused outfits available for occasion {}",
                userId, unusedCount, occasion);

        return unusedCount;
    }

    // Method to reset user's outfit history
    @Transactional
    public void resetUserOutfitHistory(String userId) {
        outfitHistoryRepository.deleteByUserId(userId);
        log.info("Reset outfit history for user {}", userId);
    }

    // Debug method to compare histories between users
    public void debugOutfitHistory(String userId1, String userId2, String outfitId) {
        boolean user1HasSeen = outfitHistoryRepository.existsByUserIdAndOutfitId(userId1, outfitId);
        boolean user2HasSeen = outfitHistoryRepository.existsByUserIdAndOutfitId(userId2, outfitId);

        log.info("Outfit {} status: User1 has seen it: {}, User2 has seen it: {}",
                outfitId, user1HasSeen, user2HasSeen);
    }

    /**
     * Generate a random outfit by selecting a random occasion
     */
    public OutfitResponseDto initiateRandomOutfitGeneration(String userId, OutfitRequestDto request) throws Exception {
        // Choose a random occasion
        Occasion[] occasions = Occasion.values();
        Random random = new Random();
        Occasion randomOccasion = occasions[random.nextInt(occasions.length)];

        // Set the random occasion in the request
        request.setOccasion(randomOccasion);

        // Delegate to the standard outfit generation method
        return initiateOutfitGeneration(userId, request);
    }

    @Transactional(readOnly = true)
    public boolean hasRemainingOutfitQuota(String userId) {
        UserSubscription subscription = subscriptionRepository.findById(userId).orElse(null);

        if (subscription == null) {
            return false; // No subscription found
        }

        return subscription.isActive() && subscription.getRemainingOutfits() > 0;
    }

    /**
     * Check user's remaining quota
     */
    @Transactional(readOnly = true)
    public int getRemainingOutfitQuota(String userId) {
        UserSubscription subscription = subscriptionRepository.findById(userId).orElse(null);

        if (subscription == null) {
            return 0; // No subscription found
        }

        return subscription.isActive() ? subscription.getRemainingOutfits() : 0;
    }

    /**
     * Use one outfit quota
     */
    @Transactional
    public void useOutfitQuota(String userId) {
        UserSubscription subscription = subscriptionRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No active subscription found"));

        if (!subscription.isActive() || subscription.getRemainingOutfits() <= 0) {
            throw new IllegalStateException("No remaining outfit quota");
        }

        subscription.useOutfit();
        subscriptionRepository.save(subscription);
    }

}