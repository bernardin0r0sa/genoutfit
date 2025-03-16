package com.genoutfit.api.controller;

import com.genoutfit.api.model.*;
import com.genoutfit.api.repository.UserSubscriptionRepository;
import com.genoutfit.api.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@Slf4j
public class OutfitController {

    @Autowired
    UserService userService;
    @Autowired
    OutfitService outfitService;

    @Autowired
    OccasionImageService occasionImageService;

    @Autowired
    UserSubscriptionRepository subscriptionRepository;

    @Autowired
    StripeService stripeService;

    @GetMapping("/")
    public String landingPage(Model model, HttpServletRequest request, Authentication authentication) {
        // Check if user is already authenticated
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            try {
                // Get the user principal
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                User user = userService.getCurrentUser(userPrincipal);

                // Check if user is a paid user and has completed onboarding
                if (user.isPremiumUser() && user.getOnboardingStatus() == OnboardingStatus.COMPLETED) {
                    // User is paid and has completed onboarding, redirect to dashboard
                    return "redirect:/dashboard";
                } else {
                    // User is authenticated but either not paid or not completed onboarding
                    // Determine the next step in the onboarding process
                    String nextStep = getNextStep(user);
                    return "redirect:" + nextStep;
                }
            } catch (Exception e) {
                // If there's an error, log it and continue to landing page
                log.error("Error checking user status: {}", e.getMessage());
            }
        }

        // Not authenticated or error occurred, show landing page
        model.addAllAttributes(createOpenGraphData(
                "OutfitGenerator - AI-Powered Fashion Recommendations",
                request.getRequestURL().toString(),
                "/assets/images/homepage-banner.jpg",
                "Get personalized outfit recommendations tailored to your style, body type, and occasion"
        ));
        return "landingpage";
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

            // Get personalized occasion images based on user characteristics
            Map<String, String> occasionImages = occasionImageService.getPersonalizedImages(user);
            model.addAttribute("occasionImages", occasionImages);

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

            // Get subscription information
            Optional<UserSubscription> userSubscriptionOpt = subscriptionRepository.findById(user.getId());

            if (userSubscriptionOpt.isPresent()) {
                UserSubscription userSubscription = userSubscriptionOpt.get();
                Map<String, Object> subscriptionData = new HashMap<>();

                // Map subscription details to the model
                subscriptionData.put("planName", userSubscription.getPlan().getDisplayName());
                subscriptionData.put("status", userSubscription.isActive() ? "Active" : "Inactive");
                subscriptionData.put("remainingOutfits", userSubscription.getRemainingOutfits());
                subscriptionData.put("nextBillingDate", userSubscription.getNextBillingDate());
                subscriptionData.put("renewalDate", userSubscription.getNextBillingDate());

                model.addAttribute("subscription", subscriptionData);

                //Stripe Customer Portal
                String billingPortal= stripeService.createCustomerPortalSession(userSubscription.getStripeCustomerId());

                model.addAttribute("billingPortal", billingPortal);


            } else {
                // No subscription found
                model.addAttribute("subscription", null);
            }

            if (userSubscriptionOpt.isPresent()) {
                UserSubscription subscription = userSubscriptionOpt.get();
                if (subscription.getStripeCustomerId() != null) {
                    // Generate a session-specific portal URL
                    String portalUrl = stripeService.createCustomerPortalSession(subscription.getStripeCustomerId());
                    model.addAttribute("stripePortalUrl", portalUrl);
                }
            }

            // Set header titles
            model.addAttribute("headerTitle", "Account Settings");
            model.addAttribute("headerSubtitle", "Manage Profile");

            // Set active page for navigation
            model.addAttribute("activePage", "account");

            // Set content fragment
            model.addAttribute("content", "fragments/account");

            return "home";
        } catch (Exception e) {
            log.error("Error in account page: ", e);
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

    /**
     * Helper method to determine the next step in onboarding process
     */
    private String getNextStep(User user) {
        return switch (user.getOnboardingStatus()) {
            case NEW -> "/";  // If user is new and has no plan, go to landing page
            case PLAN_SELECTED -> "/onboarding/profile";
            case PROFILE_COMPLETED, PAYMENT_PENDING -> "/onboarding/payment";
            case COMPLETED -> "/dashboard";
        };
    }

    @GetMapping("/terms")
    public String termsOfService() {
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacyPolicy() {
        return "privacy";
    }
}
