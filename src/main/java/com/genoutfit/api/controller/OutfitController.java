package com.genoutfit.api.controller;

import com.genoutfit.api.model.Occasion;
import com.genoutfit.api.model.Outfit;
import com.genoutfit.api.model.User;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.service.OutfitGenerationService;
import com.genoutfit.api.service.OutfitService;
import com.genoutfit.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class OutfitController {

    @Autowired
    UserService userService;
    @Autowired
    OutfitService outfitService;

    @GetMapping("/")
    public String landingPage(Model model, HttpServletRequest request) {
        model.addAllAttributes(createOpenGraphData(
                "OutfitGenerator - AI-Powered Fashion Recommendations",
                request.getRequestURL().toString(),
                "/assets/images/homepage-banner.jpg",
                "Get personalized outfit recommendations tailored to your style, body type, and occasion"
        ));
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model, HttpServletRequest request) {
        try {
            // Get user
            User user = userService.getCurrentUser(userPrincipal);
            model.addAttribute("user", user);

            // Get recent outfits (latest 10)
            List<Outfit> recentOutfits = outfitService.getRecentOutfits(user.getId(), 10);
            model.addAttribute("outfits", recentOutfits);

            // Set active page for navigation
            model.addAttribute("activePage", "dashboard");

            // Add OpenGraph data
            model.addAllAttributes(createOpenGraphData(
                    "OutfitGenerator - AI-Powered Fashion Recommendations",
                    request.getRequestURL().toString(),
                    "/assets/images/homepage-banner.jpg",
                    "Get personalized outfit recommendations tailored to your style, body type, and occasion"
            ));

            model.addAttribute("content", "fragments/dashboard");


            return "home";
        } catch (Exception e) {
            // Handle errors - you might want to log the exception
            return "redirect:/error";
        }
    }
    @GetMapping("/favorites")
    public String favorites(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        try {
            // Get user
            User user = userService.getCurrentUser(userPrincipal);
            model.addAttribute("user", user);

            // Get favorite outfits
            List<Outfit> favoriteOutfits = outfitService.getFavoriteOutfits(user.getId());
            model.addAttribute("favoriteOutfits", favoriteOutfits);

            // Set header titles
            model.addAttribute("headerTitle", "Your Favorite Outfits");
            model.addAttribute("headerSubtitle", "Saved Looks");

            // Set active page for navigation
            model.addAttribute("activePage", "favorites");

            // Set content fragment
            model.addAttribute("content", "fragments/favorites");

            return "home";
        } catch (Exception e) {
            // Handle errors
            return "redirect:/error";
        }
    }

    @GetMapping("/outfits")
    public String outfits(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Occasion occasion,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {
        try {
            // Get user
            User user = userService.getCurrentUser(userPrincipal);
            model.addAttribute("user", user);

            // Get paginated outfits
            Page<Outfit> outfitsPage = outfitService.getAllOutfits(
                    user.getId(), page, 20, occasion, sort);

            model.addAttribute("allOutfits", outfitsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", outfitsPage.getTotalPages());

            // Set header titles
            model.addAttribute("headerTitle", "Your Generated Outfits");
            model.addAttribute("headerSubtitle", "Browse Collections");

            // Set active page for navigation
            model.addAttribute("activePage", "outfits");

            // Set content fragment
            model.addAttribute("content", "fragments/outfit-page");

            return "home";
        } catch (Exception e) {
            // Handle errors
            return "redirect:/error";
        }
    }

    @GetMapping("/account")
    public String account(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        try {
            // Get user
            User user = userService.getCurrentUser(userPrincipal);
            model.addAttribute("user", user);

            // Get subscription information if any
            model.addAttribute("subscription", userService.getSubscriptionDetails(user.getId()));

            // Set header titles
            model.addAttribute("headerTitle", "Account Settings");
            model.addAttribute("headerSubtitle", "Manage Profile");

            // Set active page for navigation
            model.addAttribute("activePage", "account");

            // Set content fragment
            model.addAttribute("content", "fragments/account");

            return "home";
        } catch (Exception e) {
            // Handle errors
            return "redirect:/error";
        }
    }

    private Map<String, String> createOpenGraphData(String title, String url, String imageUrl, String description) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("ogPageTitle", title);
        attributes.put("ogCurrentUrl", url);
        attributes.put("ogImageUrl", imageUrl);
        attributes.put("ogPageDescription", description);
        return attributes;
    }
}
