package com.genoutfit.api.controller;

import com.genoutfit.api.model.SubscriptionPlan;
import com.genoutfit.api.model.User;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.model.UserSubscription;
import com.genoutfit.api.repository.UserSubscriptionRepository;
import com.genoutfit.api.service.StripeService;
import com.genoutfit.api.service.UserService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {
    private final StripeService stripeService;
    private final UserService userService;
    private final UserSubscriptionRepository subscriptionRepository;

    /**
     * Create checkout session for trial (one-time purchase)
     */
    @PostMapping("/checkout/trial")
    public ResponseEntity<Map<String, String>> createTrialCheckout(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userService.getCurrentUser(userPrincipal);
            String checkoutUrl = stripeService.createTrialCheckoutSession(user.getEmail(), user.getId());

            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (Exception e) {
            log.error("Error creating trial checkout: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create checkout session for subscription (basic or premium)
     */
    @PostMapping("/checkout/{plan}")
    public ResponseEntity<Map<String, String>> createSubscriptionCheckout(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String plan) {
        try {
            User user = userService.getCurrentUser(userPrincipal);
            SubscriptionPlan subscriptionPlan = SubscriptionPlan.valueOf(plan.toUpperCase());

            String checkoutUrl = stripeService.createSubscriptionCheckoutSession(
                    user.getEmail(), user.getId(), subscriptionPlan);

            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (Exception e) {
            log.error("Error creating subscription checkout: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current subscription details
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userService.getCurrentUser(userPrincipal);
            UserSubscription subscription = subscriptionRepository.findById(user.getId()).orElse(null);

            Map<String, Object> response = new HashMap<>();

            if (subscription != null) {
                response.put("active", subscription.isActive());
                response.put("plan", subscription.getPlan().name());
                response.put("remainingOutfits", subscription.getRemainingOutfits());
                response.put("nextBillingDate", subscription.getNextBillingDate());
                response.put("subscriptionStart", subscription.getSubscriptionStart());
            } else {
                response.put("active", false);
                response.put("plan", null);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting subscription status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cancel subscription
     */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelSubscription(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userService.getCurrentUser(userPrincipal);
            stripeService.cancelSubscription(user.getId());

            return ResponseEntity.ok(Map.of("message", "Subscription cancelled successfully"));
        } catch (Exception e) {
            log.error("Error cancelling subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * New endpoint to handle subscription upgrades
     * To be added to SubscriptionController.java
     */
    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeSubscription(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("plan") String planName) throws Exception {

        try {
            // Validate plan
            SubscriptionPlan plan = SubscriptionPlan.valueOf(planName.toUpperCase());

            // Get current user
            User user = userService.getCurrentUser(userPrincipal);

            // Get current subscription
            UserSubscription currentSubscription = subscriptionRepository.findById(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("No subscription found"));

            // Verify not downgrading or same plan
            if (currentSubscription.getPlan() == plan) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "You are already on this plan"
                ));
            }

            // Make sure not downgrading (PREMIUM > BASIC > TRIAL)
            if ((currentSubscription.getPlan() == SubscriptionPlan.PREMIUM) ||
                    (currentSubscription.getPlan() == SubscriptionPlan.BASIC && plan == SubscriptionPlan.TRIAL)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cannot downgrade plan through this flow"
                ));
            }

            // Update user with selected plan for upgrade
            user.setSelectedPlan(plan);
            userService.saveUser(user);

            // Create checkout URL for the new plan
            String checkoutUrl = stripeService.createSubscriptionCheckoutSession(
                    user.getEmail(), user.getId(), plan);

            return ResponseEntity.ok(Map.of(
                    "checkoutUrl", checkoutUrl
            ));
        } catch (Exception e) {
            log.error("Error upgrading subscription: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to upgrade: " + e.getMessage()
            ));
        }
    }

    /**
     * Stripe webhook handler

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = stripeService.constructEvent(payload, sigHeader);
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

            switch (event.getType()) {
                case "checkout.session.completed":
                    if (stripeObject instanceof Session session) {
                        stripeService.handleCheckoutSessionCompleted(session);
                    }
                    break;
                case "customer.subscription.updated":
                case "customer.subscription.deleted":
                    if (stripeObject instanceof Subscription subscription) {
                        stripeService.handleSubscriptionUpdated(subscription);
                    }
                    break;
            }

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error handling webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }
    */
}