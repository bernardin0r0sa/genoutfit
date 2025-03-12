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

import java.time.LocalDateTime;
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
    private final ConcurrentHashMap<String, OutfitGenerationTask> pendingGenerations = new ConcurrentHashMap<>();

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

            // Get the outfit outside of the synchronized block
            Outfit outfit = outfitRepository.findById(outfitId)
                    .orElseThrow(() -> new RuntimeException("Outfit not found: " + outfitId));

            User user = outfit.getUser();
            String userId = user.getId();
            String occasion = outfit.getOccasion().name().toLowerCase();

            // Upload to R2 storage
            String permanentImageUrl = r2StorageService.uploadFile(falImageUrl, userId, occasion);
            log.info("Image stored permanently at: {}", permanentImageUrl);

            // Lock on the outfit ID when updating
            synchronized (outfitId.intern()) {
                // Reload the outfit to get latest state
                outfit = outfitRepository.findById(outfitId)
                        .orElseThrow(() -> new RuntimeException("Outfit not found: " + outfitId));

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
        return findOutfitsWithFallback(user, occasion);
        /*
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
        }*/

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


    /**
     * Find outfits with fallback policy:
     * 1. Try with user preferences
     * 2. If no results, try basic search
     * 3. If still no results, use generic prompts
     */
    private List<OutfitVector> findOutfitsWithFallback(User user, Occasion occasion) {
        log.info("Searching for outfits for user {} and occasion {}", user.getId(), occasion);
        List<OutfitVector> outfits = new ArrayList<>();

        // ATTEMPT 1: Search with all user preferences
        if (user.getStylePreferences() != null && !user.getStylePreferences().isEmpty()) {
            try {
                Map<String, Object> fullCriteria = new HashMap<>();
                fullCriteria.put("occasion", occasion.name());
                fullCriteria.put("gender", user.getGender().name());
                fullCriteria.put("bodyType", user.getBodyType().name());
                fullCriteria.put("style", new ArrayList<>(user.getStylePreferences()));

                outfits = outfitReferenceService.search(fullCriteria, 5);
                log.info("Found {} outfits using full user preferences", outfits.size());
            } catch (Exception e) {
                log.error("Error searching with preferences: {}", e.getMessage());
            }
        }

        // ATTEMPT 2: If no results, try basic search without style preferences
        if (outfits.isEmpty()) {
            try {
                Map<String, Object> basicCriteria = new HashMap<>();
                basicCriteria.put("occasion", occasion.name());
                basicCriteria.put("gender", user.getGender().name());
                basicCriteria.put("bodyType", user.getBodyType().name());

                outfits = outfitReferenceService.search(basicCriteria, 5);
                log.info("Found {} outfits using basic criteria (no style preferences)", outfits.size());
            } catch (Exception e) {
                log.error("Error searching with basic criteria: {}", e.getMessage());
            }
        }

        // ATTEMPT 3: If still no results, use generic fallback
        if (outfits.isEmpty()) {
            OutfitVector genericOutfit = createGenericOutfit(user, occasion);
            outfits.add(genericOutfit);
            log.info("Using generic outfit fallback for occasion {}", occasion);
        }

        return outfits;
    }

    /**
     * Create a generic outfit when no matching outfits are found
     */
    private OutfitVector createGenericOutfit(User user, Occasion occasion) {
        // Get generic clothing pieces for the occasion
        Map<String, List<String>> clothingPieces = getGenericClothingPieces(occasion, user.getGender());

        // Get suitable colors based on occasion
        List<String> colors = getGenericColors(occasion);

        // Create a unique ID for the generic outfit
        String id = "generic-" + occasion.name() + "-" + UUID.randomUUID().toString();

        // Create metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("isGeneric", "true");
        metadata.put("generatedFor", user.getId());
        metadata.put("occasion", occasion.name());
        metadata.put("gender", user.getGender().name());
        metadata.put("bodyType", user.getBodyType().name());
        metadata.put("season", getCurrentSeason());
        metadata.put("formality", getOccasionFormality(occasion));

        // Create additional metadata
        Map<String, Object> additionalMetadata = new HashMap<>();
        additionalMetadata.put("bodyTypes", List.of(user.getBodyType().name()));
        additionalMetadata.put("occasions", List.of(occasion.name().toLowerCase()));
        additionalMetadata.put("generatedAt", LocalDateTime.now().toString());

        // Build and return generic outfit vector
        return OutfitVector.builder()
                .id(id)
                .style(getGenericStyle(occasion))
                .clothingPieces(clothingPieces)
                .colors(colors)
                .metadata(metadata)
                .additionalMetadata(additionalMetadata)
                .build();
    }

    /**
     * Get generic clothing pieces for an occasion based on gender
     */
    private Map<String, List<String>> getGenericClothingPieces(Occasion occasion, Gender gender) {
        Map<String, List<String>> pieces = new HashMap<>();

        if (gender == Gender.FEMALE) {
            switch (occasion) {
                case DATE_NIGHT:
                    pieces.put("top", List.of("elegant blouse", "silk top"));
                    pieces.put("bottom", List.of("tailored pants", "midi skirt"));
                    pieces.put("outerwear", List.of("fitted blazer"));
                    pieces.put("accessories", List.of("statement earrings", "clutch purse"));
                    pieces.put("shoes", List.of("heeled sandals", "elegant pumps"));
                    break;
                case OFFICE_PARTY:
                    pieces.put("top", List.of("formal blouse", "elegant top"));
                    pieces.put("bottom", List.of("tailored pants", "pencil skirt"));
                    pieces.put("outerwear", List.of("statement blazer"));
                    pieces.put("accessories", List.of("simple necklace", "structured handbag"));
                    pieces.put("shoes", List.of("comfortable heels", "loafers"));
                    break;
                case WEDDING_GUEST:
                    pieces.put("dress", List.of("elegant midi dress", "formal maxi dress"));
                    pieces.put("accessories", List.of("statement jewelry", "small clutch"));
                    pieces.put("shoes", List.of("strappy heels", "elegant pumps"));
                    break;
                case CASUAL_OUTING:
                    pieces.put("top", List.of("casual t-shirt", "light sweater"));
                    pieces.put("bottom", List.of("jeans", "casual pants"));
                    pieces.put("outerwear", List.of("denim jacket", "light cardigan"));
                    pieces.put("accessories", List.of("casual bag", "simple jewelry"));
                    pieces.put("shoes", List.of("sneakers", "casual flats"));
                    break;
                case FORMAL_EVENT:
                    pieces.put("dress", List.of("formal gown", "elegant cocktail dress"));
                    pieces.put("accessories", List.of("statement jewelry", "evening clutch"));
                    pieces.put("shoes", List.of("formal heels", "designer pumps"));
                    break;
                case BEACH_VACATION:
                    pieces.put("top", List.of("light tank top", "beach cover-up"));
                    pieces.put("bottom", List.of("shorts", "flowy skirt"));
                    pieces.put("accessories", List.of("sun hat", "beach bag", "sunglasses"));
                    pieces.put("shoes", List.of("sandals", "espadrilles"));
                    break;
                case BUSINESS_CASUAL:
                    pieces.put("top", List.of("blouse", "button-down shirt"));
                    pieces.put("bottom", List.of("slacks", "knee-length skirt"));
                    pieces.put("outerwear", List.of("casual blazer", "cardigan"));
                    pieces.put("accessories", List.of("simple necklace", "professional bag"));
                    pieces.put("shoes", List.of("loafers", "low heels"));
                    break;
                case PARTY:
                    pieces.put("top", List.of("sequin top", "going-out blouse"));
                    pieces.put("bottom", List.of("stylish jeans", "statement skirt"));
                    pieces.put("accessories", List.of("statement jewelry", "clutch"));
                    pieces.put("shoes", List.of("heels", "fashionable boots"));
                    break;
                case GALA:
                    pieces.put("dress", List.of("formal gown", "floor-length dress"));
                    pieces.put("accessories", List.of("fine jewelry", "elegant clutch"));
                    pieces.put("shoes", List.of("formal heels", "designer shoes"));
                    break;
                default:
                    pieces.put("top", List.of("versatile blouse"));
                    pieces.put("bottom", List.of("classic pants"));
                    pieces.put("accessories", List.of("simple jewelry"));
                    pieces.put("shoes", List.of("versatile flats"));
            }
        } else { // MALE
            switch (occasion) {
                case DATE_NIGHT:
                    pieces.put("top", List.of("button-down shirt", "polo shirt"));
                    pieces.put("bottom", List.of("chinos", "dress pants"));
                    pieces.put("outerwear", List.of("blazer", "leather jacket"));
                    pieces.put("accessories", List.of("watch", "leather belt"));
                    pieces.put("shoes", List.of("dress shoes", "loafers"));
                    break;
                case OFFICE_PARTY:
                    pieces.put("top", List.of("dress shirt", "button-down"));
                    pieces.put("bottom", List.of("dress pants", "chinos"));
                    pieces.put("outerwear", List.of("blazer", "sport coat"));
                    pieces.put("accessories", List.of("tie", "pocket square"));
                    pieces.put("shoes", List.of("oxfords", "loafers"));
                    break;
                case WEDDING_GUEST:
                    pieces.put("top", List.of("dress shirt"));
                    pieces.put("bottom", List.of("suit pants"));
                    pieces.put("outerwear", List.of("suit jacket"));
                    pieces.put("accessories", List.of("tie", "pocket square", "cufflinks"));
                    pieces.put("shoes", List.of("formal dress shoes"));
                    break;
                case CASUAL_OUTING:
                    pieces.put("top", List.of("t-shirt", "casual button-down"));
                    pieces.put("bottom", List.of("jeans", "chinos"));
                    pieces.put("outerwear", List.of("casual jacket", "sweater"));
                    pieces.put("accessories", List.of("casual watch", "hat"));
                    pieces.put("shoes", List.of("sneakers", "casual boots"));
                    break;
                case FORMAL_EVENT:
                    pieces.put("top", List.of("formal dress shirt", "tuxedo shirt"));
                    pieces.put("bottom", List.of("formal suit pants", "tuxedo pants"));
                    pieces.put("outerwear", List.of("suit jacket", "tuxedo jacket"));
                    pieces.put("accessories", List.of("bow tie", "cufflinks"));
                    pieces.put("shoes", List.of("formal oxford shoes"));
                    break;
                case BEACH_VACATION:
                    pieces.put("top", List.of("linen shirt", "t-shirt"));
                    pieces.put("bottom", List.of("shorts", "linen pants"));
                    pieces.put("accessories", List.of("sunglasses", "hat"));
                    pieces.put("shoes", List.of("sandals", "boat shoes"));
                    break;
                case BUSINESS_CASUAL:
                    pieces.put("top", List.of("button-down shirt", "polo"));
                    pieces.put("bottom", List.of("chinos", "khakis"));
                    pieces.put("outerwear", List.of("sport coat", "cardigan"));
                    pieces.put("accessories", List.of("leather belt", "watch"));
                    pieces.put("shoes", List.of("loafers", "dress shoes"));
                    break;
                case PARTY:
                    pieces.put("top", List.of("stylish shirt", "graphic tee"));
                    pieces.put("bottom", List.of("jeans", "chinos"));
                    pieces.put("outerwear", List.of("bomber jacket", "denim jacket"));
                    pieces.put("accessories", List.of("watch", "casual accessories"));
                    pieces.put("shoes", List.of("stylish sneakers", "casual boots"));
                    break;
                case GALA:
                    pieces.put("top", List.of("tuxedo shirt", "formal dress shirt"));
                    pieces.put("bottom", List.of("tuxedo pants", "formal suit pants"));
                    pieces.put("outerwear", List.of("tuxedo jacket", "formal suit jacket"));
                    pieces.put("accessories", List.of("bow tie", "cufflinks", "pocket square"));
                    pieces.put("shoes", List.of("patent leather shoes", "formal oxfords"));
                    break;
                default:
                    pieces.put("top", List.of("versatile button-down"));
                    pieces.put("bottom", List.of("classic pants"));
                    pieces.put("accessories", List.of("simple watch"));
                    pieces.put("shoes", List.of("versatile leather shoes"));
            }
        }

        return pieces;
    }

    /**
     * Get generic colors based on occasion
     */
    private List<String> getGenericColors(Occasion occasion) {
        switch (occasion) {
            case DATE_NIGHT:
                return List.of("black", "burgundy", "navy", "red");
            case OFFICE_PARTY:
                return List.of("navy", "charcoal", "burgundy", "forest green");
            case WEDDING_GUEST:
                return List.of("navy", "dusty blue", "blush", "champagne", "emerald");
            case CASUAL_OUTING:
                return List.of("blue", "white", "gray", "beige");
            case FORMAL_EVENT:
                return List.of("black", "navy", "white", "gold", "silver");
            case BEACH_VACATION:
                return List.of("white", "blue", "coral", "turquoise", "yellow");
            case BUSINESS_CASUAL:
                return List.of("navy", "gray", "white", "light blue", "khaki");
            case PARTY:
                return List.of("black", "red", "silver", "gold", "purple");
            case GALA:
                return List.of("black", "navy", "emerald", "burgundy", "gold");
            default:
                return List.of("black", "navy", "white", "gray");
        }
    }

    /**
     * Get a generic style based on occasion
     */
    private String getGenericStyle(Occasion occasion) {
        switch (occasion) {
            case DATE_NIGHT:
                return "romantic";
            case OFFICE_PARTY:
                return "business casual";
            case WEDDING_GUEST:
                return "formal";
            case CASUAL_OUTING:
                return "casual";
            case FORMAL_EVENT:
                return "formal";
            case BEACH_VACATION:
                return "resort wear";
            case BUSINESS_CASUAL:
                return "business casual";
            case PARTY:
                return "party";
            case GALA:
                return "formal";
            default:
                return "casual";
        }
    }

    /**
     * Get current season based on month
     */
    private String getCurrentSeason() {
        int month = LocalDateTime.now().getMonthValue();

        if (month >= 3 && month <= 5) {
            return "spring";
        } else if (month >= 6 && month <= 8) {
            return "summer";
        } else if (month >= 9 && month <= 11) {
            return "fall";
        } else {
            return "winter";
        }
    }

    /**
     * Get formality level based on occasion (1-5 scale)
     */
    private int getOccasionFormality(Occasion occasion) {
        switch (occasion) {
            case FORMAL_EVENT:
            case GALA:
                return 5;
            case WEDDING_GUEST:
                return 4;
            case DATE_NIGHT:
            case OFFICE_PARTY:
                return 3;
            case BUSINESS_CASUAL:
                return 2;
            case CASUAL_OUTING:
            case BEACH_VACATION:
            case PARTY:
                return 1;
            default:
                return 2;
        }
    }

}