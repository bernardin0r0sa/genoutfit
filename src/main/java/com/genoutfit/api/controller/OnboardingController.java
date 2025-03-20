package com.genoutfit.api.controller;

import com.genoutfit.api.JwtTokenProvider;
import com.genoutfit.api.model.*;
import com.genoutfit.api.repository.UserSubscriptionRepository;
import com.genoutfit.api.service.StripeService;
import com.genoutfit.api.service.UserService;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserSubscriptionRepository subscriptionRepository;

    @GetMapping("/set-plan")
    public String setPlanAfterLogin(
            @RequestParam("plan") String planName,
            Authentication authentication,
            Model model,
            HttpSession session,
            HttpServletResponse response) {

        // Store the plan in session in case authentication is required
        session.setAttribute("selectedPlan", planName);

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

        // If not authenticated, redirect to register with plan parameter instead of login
        // This ensures a completely new request is made to /register
        try {
            response.sendRedirect("/onboard?plan=" + URLEncoder.encode(planName, StandardCharsets.UTF_8.toString()));
            return null; // Return null since we've already sent the response
        } catch (IOException e) {
           // log.error("Error redirecting to register page: {}", e.getMessage());
            return "redirect:/?error=RedirectError";
        }
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
                        SubscriptionPlan.BASIC,
                        request
                );
            } else if (user.getSelectedPlan() == SubscriptionPlan.TRIAL) {
                checkoutUrl = stripeService.createTrialCheckoutSession(user.getEmail(), user.getId(),request);
            } else {
                // For BASIC or PREMIUM subscriptions
                checkoutUrl = stripeService.createSubscriptionCheckoutSession(
                        user.getEmail(),
                        user.getId(),
                        user.getSelectedPlan(),
                        request
                );
            }

            // Redirect to Stripe checkout
            return "redirect:" + checkoutUrl;
        }

        // If not authenticated, redirect to login
        return "redirect:/login";
    }

    @GetMapping("/success")
    public String showSuccess(
            @RequestParam("session_id") String sessionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            Model model,
            HttpServletRequest request) {

        try {
            // Get the user
            User user = userService.getCurrentUser(userPrincipal);

            // Get plan from the user's selected plan
            SubscriptionPlan plan = user.getSelectedPlan();
            if (plan == null) {
                // Default to BASIC if somehow no plan is selected
                plan = SubscriptionPlan.BASIC;
            }

            // Update subscription details using the Stripe session
            Session session = Session.retrieve(sessionId);

            // 1. Activate premium status
            user.setPremiumUser(true);
            user.setOnboardingStatus(OnboardingStatus.COMPLETED);
            userService.saveUser(user);

// 2. Create or update subscription record
            UserSubscription subscription = subscriptionRepository.findById(user.getId()).orElse(
                    new UserSubscription(
                            user.getId(),
                            plan,
                            session.getSubscription(),  // For subscription plans
                            session.getCustomer()
                    )
            );

// Always update these fields whether new or existing
            subscription.setPlan(plan);
            subscription.setRemainingOutfits(plan.getMonthlyOutfitQuota());
            subscription.setActive(true);

// Update subscription ID for recurring plans
            if (plan != SubscriptionPlan.TRIAL && session.getSubscription() != null) {
                subscription.setStripeSubscriptionId(session.getSubscription());

                // Set next billing date for subscriptions
                Subscription stripeSubscription = Subscription.retrieve(session.getSubscription());
                LocalDateTime nextBillingDate = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()),
                        ZoneId.systemDefault());
                subscription.setNextBillingDate(nextBillingDate);
            }

            subscriptionRepository.save(subscription);

            // Add the success model attributes
            model.addAttribute("content", "fragments/success");
            model.addAttribute("plan", plan.getDisplayName());
            model.addAttribute("outfitQuota", plan.getMonthlyOutfitQuota());
            model.addAllAttributes(createOpenGraphData(
                    "Payment Successful - OutfitGenerator",
                    request.getRequestURL().toString(),
                    "/assets/images/success-banner.jpg",
                    "Thank you for becoming a premium member"
            ));

            return "index";
        } catch (Exception e) {
            // Log error and redirect to error page
            e.printStackTrace();
            return "redirect:/error?message=payment-processing-failed";
        }
    }
    @GetMapping("/confirm-plan")
    public String confirmPlan(
            @RequestParam("userId") String userId,
            @RequestParam("plan") String planName,
            @RequestParam("token") String token,
            HttpServletResponse response) {

        try {
            // Verify the token is valid and contains the user ID
            if (!tokenProvider.validateToken(token)) {
                return "redirect:/login?error=InvalidToken";
            }

            String tokenUserId = tokenProvider.getUserIdFromToken(token);
            if (!tokenUserId.equals(userId)) {
                return "redirect:/login?error=InvalidUser";
            }

            // Get the user and update their plan
            User user = userService.getUserById(userId);
            SubscriptionPlan plan = SubscriptionPlan.valueOf(planName.toUpperCase());
            user.setSelectedPlan(plan);
            user.setOnboardingStatus(OnboardingStatus.PLAN_SELECTED);
            userService.saveUser(user);

            // Set the token as a cookie for future requests
            Cookie authCookie = new Cookie("authToken", token);
            authCookie.setPath("/");
            authCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            authCookie.setHttpOnly(true);
            response.addCookie(authCookie);

            // Redirect to profile page
            return "redirect:/onboarding/profile";
        } catch (Exception e) {
            return "redirect:/?error=InvalidPlan";
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

    private String getNextStep(User user) {
        return switch (user.getOnboardingStatus()) {
            case NEW -> "/";  // If user is new and has no plan, go to landing page
            case PLAN_SELECTED -> "/onboarding/profile";
            case PROFILE_COMPLETED, PAYMENT_PENDING -> "/onboarding/payment";
            case COMPLETED -> "/dashboard";
        };
    }
}