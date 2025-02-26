package com.genoutfit.api.controller;

import com.genoutfit.api.model.*;
import com.genoutfit.api.service.OutfitGenerationService;
import com.genoutfit.api.service.OutfitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Controller
@RequestMapping("/api/outfits")
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

            // Create three placeholder cards
            List<PlaceholderOutfit> placeholders = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                placeholders.add(new PlaceholderOutfit(
                        response.getId(),
                        i,
                        "/assets/images/placeholder.jpg",
                        occasion.getDisplayName()
                ));
            }

            model.addAttribute("placeholders", placeholders);

            // Return the placeholders fragment
            return "fragments/outfit-placeholders :: outfit-placeholders";
        } catch (Exception e) {
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

            // Create three placeholder cards
            List<PlaceholderOutfit> placeholders = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                placeholders.add(new PlaceholderOutfit(
                        response.getId(),
                        i,
                        "/assets/images/placeholder.jpg",
                        randomOccasion.getDisplayName()
                ));
            }

            model.addAttribute("placeholders", placeholders);

            // Return the placeholders fragment
            return "fragments/outfit-placeholders :: outfit-placeholders";
        } catch (Exception e) {
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
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Model model) {

        try {
            // Get outfits filtered by occasion
            List<OutfitResponseDto> outfits;

            if (occasion != null) {
                outfits = outfitService.getOutfits(userPrincipal.getId(), occasion, "newest");
                // Update the filter button text with the selected occasion
                model.addAttribute("selectedOccasion", occasion.getDisplayName());
            } else {
                outfits = outfitService.getOutfits(userPrincipal.getId(), null, "newest");
                // Reset filter button text
                model.addAttribute("selectedOccasion", "All Occasions");
            }

            model.addAttribute("outfits", outfits);

            if (outfits.isEmpty()) {
                return "fragments/outfits :: no-outfits";
            } else {
                return "fragments/outfits :: outfit-grid";
            }
        } catch (Exception e) {
            // Handle error case
            model.addAttribute("errorMessage", "Failed to filter outfits: " + e.getMessage());
            return "fragments/error :: filter-error";
        }
    }
}