package com.genoutfit.api.controller;

import com.genoutfit.api.model.OnboardingStatus;
import com.genoutfit.api.model.ProfileDto;
import com.genoutfit.api.model.SubscriptionPlan;
import com.genoutfit.api.model.User;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.service.StripeService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.genoutfit.api.service.UserService;

import java.util.Map;

import static com.genoutfit.api.model.OnboardingStatus.*;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
@Slf4j
public class OnboardingAPIController {
    private final UserService userService;
    private final StripeService stripeService;

    @GetMapping("/status")
    public ResponseEntity<?> getOnboardingStatus(@AuthenticationPrincipal UserPrincipal userPrincipal) throws Exception {
        User user = userService.getCurrentUser(userPrincipal);

        return ResponseEntity.ok(Map.of(
                "status", user.getOnboardingStatus(),
                "nextStep", getNextStep(user),
                "isProfileComplete", user.getOnboardingStatus().ordinal() >= PROFILE_COMPLETED.ordinal(),
                "isPremiumUser", user.isPremiumUser(),
                "selectedPlan", user.getSelectedPlan() != null ? user.getSelectedPlan().name() : null
        ));
    }

    /**
     * First step: Select a subscription plan
     * This could be called from the landing page before login
     */
    @PostMapping("/select-plan")
    public ResponseEntity<?> selectPlan(
            @RequestParam("plan") String planName) {

        try {
            // Validate plan
            SubscriptionPlan plan = SubscriptionPlan.valueOf(planName.toUpperCase());

            // Store plan selection in session for later use
            // We'll retrieve it after user logs in or signs up

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "selectedPlan", plan.name(),
                    "nextStep", "/auth/login?plan=" + plan.name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid plan: " + planName
            ));
        }
    }

    /**
     * After login/signup, set the selected plan for the user
     */
    @PostMapping("/set-plan-after-login")
    public ResponseEntity<?> setPlanAfterLogin(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("plan") String planName) throws Exception {

        try {
            // Validate plan
            SubscriptionPlan plan = SubscriptionPlan.valueOf(planName.toUpperCase());

            // Update user with selected plan
            User user = userService.getCurrentUser(userPrincipal);
            user.setSelectedPlan(plan);
            user.setOnboardingStatus(PLAN_SELECTED);
            userService.saveUser(user);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "selectedPlan", plan.name(),
                    "nextStep", "/onboarding/profile"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to set plan: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<?> completeProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProfileDto profileDto) throws Exception {

        User user = userService.updateProfile(userPrincipal.getId(), profileDto);

        // Move directly to payment after profile completion
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "nextStep", "/onboarding/payment"
        ));
    }

    /**
     * Proceed to payment after profile completion
     */
    @PostMapping("/proceed-to-payment")
    public ResponseEntity<?> proceedToPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal, HttpServletRequest request) throws Exception {

        User user = userService.getCurrentUser(userPrincipal);
        user.setOnboardingStatus(PAYMENT_PENDING);
        userService.saveUser(user);

        // Get the selected plan (default to BASIC if not set)
        SubscriptionPlan plan = user.getSelectedPlan() != null
                ? user.getSelectedPlan()
                : SubscriptionPlan.BASIC;

        String checkoutUrl;

        // Create the appropriate checkout session based on plan
        if (plan == SubscriptionPlan.TRIAL) {
            checkoutUrl = stripeService.createTrialCheckoutSession(user.getEmail(), user.getId(), request);
        } else {
            // For BASIC or PREMIUM subscriptions
            checkoutUrl = stripeService.createSubscriptionCheckoutSession(user.getEmail(), user.getId(), plan, request);
        }

        return ResponseEntity.ok(Map.of(
                "checkoutUrl", checkoutUrl
        ));
    }

    /*@PostMapping("/webhook/stripe")
    public ResponseEntity<?> stripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) throws Exception {
        Event event = stripeService.constructEvent(payload, sigHeader);

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getData().getObject();
            String userId = session.getMetadata().get("userId");

            userService.activatePremiumUser(userId);
        }

        return ResponseEntity.ok().build();
    }*/

    private String getNextStep(User user) {
        return switch (user.getOnboardingStatus()) {
            case NEW -> "/"; // Start at landing page with plan selection
            case PLAN_SELECTED -> "/onboarding/profile";
            case PROFILE_COMPLETED -> "/onboarding/payment";
            case PAYMENT_PENDING -> "/onboarding/payment";
            case COMPLETED -> "/dashboard";
        };
    }
}