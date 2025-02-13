package com.genoutfit.api.controller;

import com.genoutfit.api.model.ProfileDto;
import com.genoutfit.api.model.User;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.service.StripeService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.genoutfit.api.service.UserService;

import java.util.Map;

import static com.genoutfit.api.model.OnboardingStatus.*;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    private final UserService userService;
    private final StripeService stripeService;

    @GetMapping("/status")
    public ResponseEntity<?> getOnboardingStatus(@AuthenticationPrincipal UserPrincipal userPrincipal) throws Exception {
        User user = userService.getCurrentUser(userPrincipal);

        return ResponseEntity.ok(Map.of(
                "status", user.getOnboardingStatus(),
                "nextStep", getNextStep(user),
                "isProfileComplete", user.getOnboardingStatus().ordinal() >= PROFILE_COMPLETED.ordinal(),
                "isPremiumUser", user.isPremiumUser()
        ));
    }

    @PostMapping("/profile")
    public ResponseEntity<?> completeProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ProfileDto profileDto) throws Exception {

        User user = userService.updateProfile(userPrincipal.getId(), profileDto);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "nextStep", "/preview"
        ));
    }

    @PostMapping("/preview-complete")
    public ResponseEntity<?> completePreview(@AuthenticationPrincipal UserPrincipal userPrincipal) throws Exception {
        User user = userService.getCurrentUser(userPrincipal);
        user.setOnboardingStatus(PAYMENT_PENDING);
        userService.saveUser(user);

        // Create Stripe checkout session
        String checkoutUrl = stripeService.createCheckoutSession(user.getEmail(), user.getId());

        return ResponseEntity.ok(Map.of(
                "checkoutUrl", checkoutUrl
        ));
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<?> stripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) throws Exception {
        Event event = stripeService.constructEvent(payload, sigHeader);

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getData().getObject();
            String userId = session.getMetadata().get("userId");

            userService.activatePremiumUser(userId);
        }

        return ResponseEntity.ok().build();
    }

    private String getNextStep(User user) {
        return switch (user.getOnboardingStatus()) {
            case NEW -> "/onboarding/profile";
            case PROFILE_COMPLETED -> "/onboarding/preview";
            case PAYMENT_PENDING -> "/onboarding/payment";
            case COMPLETED -> "/dashboard";
        };
    }
}