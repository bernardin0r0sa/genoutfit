package com.genoutfit.api.controller;

import com.genoutfit.api.model.OnboardingStatus;
import com.genoutfit.api.model.SubscriptionPlan;
import com.genoutfit.api.model.User;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.service.StripeService;
import com.genoutfit.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/onboarding")
public class OnboardingController {

    @Autowired
    private UserService userService;

    @Autowired
    private StripeService stripeService;

    /**
     * After login/signup, set the selected plan for the user and redirect to profile
     */
    @GetMapping("/set-plan")
    public String setPlanAfterLogin(
            @RequestParam("plan") String planName,
            Authentication authentication,
            Model model) {

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                // Get the user
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                User user = userService.getCurrentUser(userPrincipal);

                // Set the selected plan
                SubscriptionPlan plan = SubscriptionPlan.valueOf(planName.toUpperCase());
                user.setSelectedPlan(plan);
                user.setOnboardingStatus(OnboardingStatus.PLAN_SELECTED);
                userService.saveUser(user);

                // Redirect to profile page
                return "redirect:/onboarding/profile";
            } catch (Exception e) {
                // If error, redirect to home
                return "redirect:/?error=InvalidPlan";
            }
        }

        // If not authenticated, redirect to login with plan parameter
        return "redirect:/login?plan=" + planName;
    }

    @GetMapping("/profile")
    public String showProfileForm(Model model, HttpServletRequest request, Authentication authentication) {
        // Check if user is authenticated and has selected a plan
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                User user = userService.getCurrentUser(userPrincipal);

                // If user hasn't selected a plan, redirect to home
                if (user.getSelectedPlan() == null) {
                    return "redirect:/";
                }

                model.addAttribute("user", user);
                model.addAttribute("selectedPlan", user.getSelectedPlan());
            } catch (Exception e) {
                // If error, continue without user data
            }
        }

        // Add style options to model
        model.addAttribute("styleOptions", Arrays.asList(
                "Casual", "Business", "Formal", "Streetwear",
                "Bohemian", "Minimalist", "Vintage", "Athletic"
        ));

        model.addAttribute("content", "fragments/profile");
        model.addAllAttributes(createOpenGraphData(
                "Complete Your Profile - OutfitGenerator",
                request.getRequestURL().toString(),
                "/assets/images/profile-banner.jpg",
                "Complete your profile to get personalized outfit recommendations"
        ));
        return "index";
    }

    @GetMapping("/payment")
    public String showPayment(Model model, HttpServletRequest request, Authentication authentication) throws Exception {
        // Check if user is authenticated and has completed profile
        if (authentication != null && authentication.isAuthenticated()) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userService.getCurrentUser(userPrincipal);

            // Update user status to PAYMENT_PENDING if coming from profile
            if (user.getOnboardingStatus() == OnboardingStatus.PROFILE_COMPLETED) {
                user.setOnboardingStatus(OnboardingStatus.PAYMENT_PENDING);
                userService.saveUser(user);
            }

            // Get the selected plan and create appropriate checkout URL
            String checkoutUrl;
            if (user.getSelectedPlan() == null) {
                // Default to BASIC if no plan is selected
                checkoutUrl = stripeService.createSubscriptionCheckoutSession(
                        user.getEmail(),
                        user.getId(),
                        SubscriptionPlan.BASIC
                );
            } else if (user.getSelectedPlan() == SubscriptionPlan.TRIAL) {
                checkoutUrl = stripeService.createTrialCheckoutSession(user.getEmail(), user.getId());
            } else {
                // For BASIC or PREMIUM subscriptions
                checkoutUrl = stripeService.createSubscriptionCheckoutSession(
                        user.getEmail(),
                        user.getId(),
                        user.getSelectedPlan()
                );
            }

            // Redirect to Stripe checkout
            return "redirect:" + checkoutUrl;
        }

        // If not authenticated, redirect to login
        return "redirect:/login";
    }

    @GetMapping("/success")
    public String showSuccess(Model model, HttpServletRequest request) {
        model.addAttribute("content", "fragments/success");
        model.addAllAttributes(createOpenGraphData(
                "Payment Successful - OutfitGenerator",
                request.getRequestURL().toString(),
                "/assets/images/success-banner.jpg",
                "Thank you for becoming a premium member"
        ));
        return "index";
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