package com.genoutfit.api.controller;

import com.genoutfit.api.model.ProgrammaticPage;
import com.genoutfit.api.service.ProgrammaticPageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
public class ProgrammaticPageController {
    private final ProgrammaticPageService programmaticPageService;

    public ProgrammaticPageController(ProgrammaticPageService programmaticPageService) {
        this.programmaticPageService = programmaticPageService;
    }

    @GetMapping("/plus-size-outfits/**")
    public String getPlusSizePage(HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {

        return handleProgrammaticPage(request,"plus-size-outfits",model,redirectAttributes);

    }

    @GetMapping("/women-outfits/**")
    public String getWomenOutfitsPage(HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {

        return handleProgrammaticPage(request,"women-outfits",model,redirectAttributes);

    }
    @GetMapping("/outfit-ideas/**")
    public String getOutfitsPage(HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {

        return handleProgrammaticPage(request,"outfit-ideas",model,redirectAttributes);

    }


    public String handleProgrammaticPage(HttpServletRequest request, @PathVariable("category") String category, Model model, RedirectAttributes redirectAttributes) {


        String path = request.getRequestURI().substring(("/" + category).length());
        if (path.startsWith("/")) path = path.substring(1);
        String fullSlug = category + "/" + path;

        String finalPath = path;

        return programmaticPageService.getPageBySlug(fullSlug)
                .map(page -> {
                    // Extract keyword from the slug and add it to the model
                    String keyword = extractKeywordFromSlug(finalPath);
                    String introduction = generateDynamicIntroduction(keyword, null);
                    model.addAttribute("dynamicIntroduction", introduction);
                    model.addAttribute("keyword", keyword);
                    model.addAttribute("page", page);
                    model.addAttribute("relatedPages", programmaticPageService.getRelatedPages(page));
                    return "programmatic-page";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Page not found");
                    return "redirect:/" + category;
                });
    }



    /**
     * Extract a human-readable keyword from a slug
     * Examples:
     * "wedding-guest-outfits" -> "wedding guest"
     * "all-white-outfit" -> "all white"
     * "funeral" -> "funeral"
     * "rave-outfit-inspo" -> "rave"
     */
    private String extractKeywordFromSlug(String slug) {
        // Remove common suffix patterns
        String cleanSlug = slug.replaceAll("(?:-outfit|-outfits|-inspo|-ideas|-inspiration|-looks|-style|-styles)$", "");

        // For simple single-word keywords, just return them
        if (!cleanSlug.contains("-")) return cleanSlug;

        // For multi-word keywords, convert to readable format
        String readableKeyword = cleanSlug.replace("-", " ");

        // Limit to first 3 words for conciseness (adjust as needed)
        String[] words = readableKeyword.split("\\s+");
        if (words.length > 3) {
            return String.join(" ", Arrays.copyOfRange(words, 0, 3));
        }

        return readableKeyword;
    }

    private String generateDynamicIntroduction(String keyword, String fallbackIntro) {
        if (keyword == null || keyword.isEmpty()) {
            return fallbackIntro;
        }

        // Check if keyword already contains "outfit"
        boolean containsOutfit = keyword.toLowerCase().contains("outfit");

        // Construct the format strings based on whether keyword already contains "outfit"
        String firstPart = containsOutfit ? "Looking for the perfect %s? You've come to the right place!"
                : "Looking for the perfect %s outfit? You've come to the right place!";

        String secondPart = containsOutfit ? "but we've curated some amazing %s inspiration just for you."
                : "but we've curated some amazing %s outfit inspiration just for you.";

        String thirdPart = containsOutfit ? "these %s ideas combine current trends"
                : "these %s outfit ideas combine current trends";

        return String.format(
                firstPart
                        + " Finding stylish outfit ideas that match your personal taste and body type can be challenging, "
                        + secondPart
                        + " Whether you're preparing for a special occasion or simply want to refresh your wardrobe, "
                        + thirdPart
                        + " with timeless elements that will help you look and feel your best.",
                keyword, keyword, keyword
        );
    }

}