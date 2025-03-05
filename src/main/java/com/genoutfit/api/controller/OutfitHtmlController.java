package com.genoutfit.api.controller;

import com.genoutfit.api.model.*;
import com.genoutfit.api.service.OutfitGenerationService;
import com.genoutfit.api.service.OutfitService;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
@RequestMapping("/api/outfits")
@Slf4j
public class OutfitHtmlController {

    @Autowired
    private OutfitGenerationService outfitGenerationService;

    @Autowired
    private OutfitService outfitService;

    /**
     * Generate an outfit and return placeholder cards with tracking IDs
     */
    @PostMapping("/generate-ui")
    public String generateOutfitUI(
            @RequestParam Occasion occasion,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Model model) {

        try {
            // Create generation request
            OutfitRequestDto request = new OutfitRequestDto();
            request.setOccasion(occasion);
            request.setUsePreviousPreferences(true);

            // Generate the outfit (this initiates async generation)
            OutfitResponseDto response = outfitGenerationService.initiateOutfitGeneration(
                    userPrincipal.getId(), request);

            // Create placeholder card
            List<PlaceholderOutfit> placeholders = new ArrayList<>();
            placeholders.add(new PlaceholderOutfit(
                    response.getId(),
                    0,
                    "/assets/images/placeholder.png",
                    occasion.getDisplayName()
            ));
            model.addAttribute("placeholders", placeholders);

            // Return the placeholders fragment
            return "fragments/outfit-placeholders :: outfit-placeholders";
        } catch (Exception e) {
            log.error("Error generating outfit: {}", e.getMessage(), e);
            // Handle error case
            model.addAttribute("errorMessage", "Failed to generate outfit: " + e.getMessage());
            return "fragments/error :: generation-error";
        }
    }

    /**
     * Generate a random outfit
     */
    @PostMapping("/generate-ui/random")
    public String generateRandomOutfitUI(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Model model) {

        try {
            // Create a request for a random outfit
            OutfitRequestDto request = new OutfitRequestDto();
            request.setUsePreviousPreferences(true);

            // Pick a random occasion
            Occasion[] occasions = Occasion.values();
            Random random = new Random();
            Occasion randomOccasion = occasions[random.nextInt(occasions.length)];
            request.setOccasion(randomOccasion);

            // Generate a random outfit
            OutfitResponseDto response = outfitGenerationService.initiateOutfitGeneration(
                    userPrincipal.getId(), request);

            // Create placeholder card
            List<PlaceholderOutfit> placeholders = new ArrayList<>();
            placeholders.add(new PlaceholderOutfit(
                    response.getId(),
                    0,
                    "/assets/images/placeholder.png",
                    randomOccasion.getDisplayName()
            ));

            model.addAttribute("placeholders", placeholders);

            // Return the placeholders fragment
            return "fragments/outfit-placeholders :: outfit-placeholders";
        } catch (Exception e) {
            log.error("Error generating random outfit: {}", e.getMessage(), e);
            // Handle error case
            model.addAttribute("errorMessage", "Failed to generate outfit: " + e.getMessage());
            return "fragments/error :: generation-error";
        }
    }

    /**
     * Toggle favorite status of an outfit
     */
    @PostMapping("/{outfitId}/favorite")
    public String toggleFavorite(
            @PathVariable String outfitId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Model model) {

        try {
            boolean isFavorite = outfitService.toggleFavorite(outfitId, userPrincipal.getId());

            model.addAttribute("message", isFavorite ?
                    "Added to favorites!" : "Removed from favorites!");
            model.addAttribute("isFavorite", isFavorite);

            // Return the toast fragment
            return "fragments/toast :: favorite-toast";
        } catch (Exception e) {
            log.error("Error toggling favorite: {}", e.getMessage(), e);
            // Handle error case
            model.addAttribute("errorMessage", "Failed to update favorite status: " + e.getMessage());
            return "fragments/error :: favorite-error";
        }
    }

    /**
     * Filter outfits by occasion
     */
    @GetMapping("/filter")
    public String filterOutfits(
            @RequestParam(required = false) Occasion occasion,
            @RequestParam(defaultValue = "newest") String sort,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Model model) {

        try {
            // Get outfits filtered by occasion
            List<OutfitResponseDto> outfits;

            if (occasion != null) {
                outfits = outfitService.getOutfits(userPrincipal.getId(), occasion, sort);
            } else {
                outfits = outfitService.getOutfits(userPrincipal.getId(), null, sort);
            }

            model.addAttribute("outfits", outfits);

            if (outfits.isEmpty()) {
                return "fragments/filtered-outfits :: no-outfits";
            } else {
                return "fragments/filtered-outfits :: filtered-outfits";
            }
        } catch (Exception e) {
            log.error("Error filtering outfits: {}", e.getMessage(), e);
            // Handle error case
            model.addAttribute("errorMessage", "Failed to filter outfits: " + e.getMessage());
            return "fragments/error :: filter-error";
        }
    }

    @GetMapping("/favorites/filter")
    public String filterFavoriteOutfits(
            @RequestParam(required = false) Occasion occasion,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Model model) {

        try {
            // Get favorite outfits filtered by occasion
            List<OutfitResponseDto> favoriteOutfits = outfitService.getFavoriteOutfitsDto(userPrincipal.getId(), occasion);

            model.addAttribute("outfits", favoriteOutfits);

            if (favoriteOutfits.isEmpty()) {
                return "fragments/filtered-outfits :: no-outfits";
            } else {
                return "fragments/filtered-outfits :: filtered-outfits";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to filter favorite outfits: " + e.getMessage());
            return "fragments/error :: filter-error";
        }
    }

    /**
     * Get outfit generation status
     */
    @GetMapping("/{outfitId}/status")
    @ResponseBody
    public Map<String, Object> getOutfitStatus(
            @PathVariable String outfitId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        OutfitGenerationStatus status = outfitGenerationService.getOutfitGenerationStatus(outfitId);

        Map<String, Object> response = new HashMap<>();
        response.put("complete", status.isComplete());
        response.put("imageUrls", status.getImageUrls());
        response.put("errors", status.getErrors());

        // Try to get outfit details if complete
        if (status.isComplete() && (status.getErrors() == null || status.getErrors().isEmpty())) {
            try {
                OutfitResponseDto outfit = outfitService.getOutfitDetailsDto(outfitId, userPrincipal.getId());
                response.put("occasion", outfit.getOccasion());
                response.put("date", outfit.getCreatedAt());
                response.put("favorite", outfit.isFavorite());
            } catch (Exception e) {
                log.warn("Error getting outfit details for status: {}", e.getMessage());
            }
        }

        return response;
    }

    /**
     * Helper class for placeholders
     */
    @Data
    @AllArgsConstructor
    public static class PlaceholderOutfit {
        private String outfitId;
        private int index;
        private String placeholderImage;
        private String occasion;
    }
}