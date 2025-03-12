package com.genoutfit.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genoutfit.api.model.*;
import com.genoutfit.api.repository.OutfitReferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.genoutfit.api.model.Occasion.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutfitReferenceService {
    private final OutfitReferenceRepository outfitReferenceRepository;
    private final ObjectMapper objectMapper;

    /**
     * Search for outfits matching the given criteria
     */
    public List<OutfitVector> search(Map<String, Object> criteria, int limit) {
        try {
            // Extract and normalize search criteria
            String gender = criteria.containsKey("gender") ?
                    normalizeGender(criteria.get("gender").toString()) : null;

            String bodyTypeStr = criteria.containsKey("bodyType") ?
                    criteria.get("bodyType").toString() : "MEDIUM";
            String bodyTypeJson = String.format("\"%s\"", bodyTypeStr.toLowerCase().replace("_", "-"));

            String occasionStr = criteria.containsKey("occasion") ?
                    criteria.get("occasion").toString() : "CASUAL_OUTING";
            String occasionJson = String.format("\"%s\"", normalizeOccasion(occasionStr));

            List<OutfitReference> outfits;

            // Check if style preferences are provided
            if (criteria.containsKey("style") && criteria.get("style") != null) {
                List<String> styles = extractStyles(criteria.get("style"));
                String stylesJson = objectMapper.writeValueAsString(styles);

                // Get preferred style (first one) if available
                String preferredStyle = styles.isEmpty() ? "\"\"" : String.format("\"%s\"", styles.get(0));

                // Default formality level is 3 (middle)
                Integer formality = 3;

                outfits = outfitReferenceRepository.findWithStylePreferences(
                        gender,
                        bodyTypeJson,
                        occasionJson,
                        stylesJson,
                        preferredStyle,
                        formality,
                        limit
                );
            } else {
                // Use simple search if no style preferences
                outfits = outfitReferenceRepository.findByBasicCriteria(
                        gender,
                        bodyTypeJson,
                        occasionJson,
                        limit
                );
            }

            log.info("Found {} matching outfits", outfits.size());

            // Convert outfit references to outfit vectors
            return outfits.stream()
                    .map(this::convertToOutfitVector)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error searching outfits: {}", e.getMessage(), e);

            // Instead of throwing an exception, return an empty list
            // This allows the fallback system to take over
            return Collections.emptyList();
        }
    }

    /**
     * Extract styles from criteria
     */
    private List<String> extractStyles(Object styleObj) {
        List<String> styles = new ArrayList<>();

        try {
            if (styleObj instanceof List) {
                styles = ((List<?>) styleObj).stream()
                        .map(Object::toString)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
            } else if (styleObj instanceof String) {
                String stylesStr = (String) styleObj;
                // Handle case where styles might be in string format "[style1, style2]"
                if (stylesStr.startsWith("[") && stylesStr.endsWith("]")) {
                    stylesStr = stylesStr.substring(1, stylesStr.length() - 1);
                    styles = Arrays.stream(stylesStr.split(","))
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
                } else {
                    styles = Collections.singletonList(stylesStr.toLowerCase());
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting styles: {}", e.getMessage());
        }

        return styles;
    }

    /**
     * Convert OutfitReference to OutfitVector
     */
    private OutfitVector convertToOutfitVector(OutfitReference reference) {
        try {
            // Parse clothing pieces from JSON
            Map<String, List<String>> clothingPieces = parseClothingPieces(reference.getClothingPieces());

            // Parse colors from JSON
            List<String> colors = parseJsonArray(reference.getColors());

            // Get primary outfit type
            List<String> outfitTypes = parseJsonArray(reference.getOutfitTypes());
            String primaryStyle = outfitTypes.isEmpty() ? null : outfitTypes.get(0);

            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("postId", reference.getPostId());
            metadata.put("influencerId", reference.getInfluencerId());
            metadata.put("season", reference.getSeason());
            metadata.put("formality", reference.getFormalityLevel());
            metadata.put("genderStyle", reference.getGenderStyle());
            metadata.put("description", reference.getOutfitDescription());

            // Include additional metadata
            Map<String, Object> additionalMetadata = new HashMap<>();
            additionalMetadata.put("bodyTypes", parseJsonArray(reference.getBodyTypes()));
            additionalMetadata.put("occasions", parseJsonArray(reference.getOccasions()));
            additionalMetadata.put("patterns", parseJsonArray(reference.getPatterns()));
            additionalMetadata.put("styleKeywords", parseJsonArray(reference.getStyleKeywords()));

            return OutfitVector.builder()
                    .id(reference.getId().toString())
                    .clothingPieces(clothingPieces)
                    .colors(colors)
                    .style(primaryStyle)
                    .metadata(metadata)
                    .additionalMetadata(additionalMetadata)
                    .build();

        } catch (Exception e) {
            log.error("Error converting outfit reference to vector: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert outfit reference", e);
        }
    }

    /**
     * Parse clothing pieces from JSON string
     */
    private Map<String, List<String>> parseClothingPieces(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }

        try {
            // Parse the JSON into a Map
            Map<String, Object> rawMap = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() {});

            // Convert to expected format
            Map<String, List<String>> result = new HashMap<>();

            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value == null) {
                    continue;
                }

                // Handle different value types
                if (value instanceof List) {
                    // Already a list, just cast and use
                    result.put(key, convertObjectListToStringList((List<?>) value));
                } else if (value instanceof String) {
                    // Single string, wrap in list
                    result.put(key, Collections.singletonList((String) value));
                }
            }

            return result;

        } catch (Exception e) {
            log.warn("Error parsing clothing pieces JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Parse JSON array string to list of strings
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Error parsing JSON array: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Convert list of objects to list of strings
     */
    private List<String> convertObjectListToStringList(List<?> objects) {
        return objects.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    /**
     * Normalize gender string to match database values
     */
    private String normalizeGender(String gender) {
        if (gender == null) return null;

        gender = gender.toLowerCase();
        if (gender.equals("male")) return "masculine";
        if (gender.equals("female")) return "feminine";
        return gender;
    }

    /**
     * Normalize occasion string to match database values
     */
    private String normalizeOccasion(String occasion) {
        if (occasion == null) return null;

        // Keep as enum format for matching with JSON stored values
        return occasion;
    }

}