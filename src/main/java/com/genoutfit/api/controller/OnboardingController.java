package com.genoutfit.api.controller;

import com.genoutfit.api.model.OnboardingStatus;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.*;

@Controller
@RequestMapping("/onboarding")
public class OnboardingController {

    @Autowired
    private UserService userService;
    @Autowired
    private StripeService stripeService;


    @GetMapping("/profile")
    public String showProfileForm(Model model, HttpServletRequest request, Authentication authentication) {
        // Add style options to model
        model.addAttribute("styleOptions", Arrays.asList(
                "Casual", "Business", "Formal", "Streetwear",
                "Bohemian", "Minimalist", "Vintage", "Athletic"
        ));

        model.addAttribute("content", "fragments/profile");
        model.addAllAttributes(Collections.singleton(createOpenGraphData(
                "Complete Your Profile - OutfitGenerator",
                request.getRequestURL().toString(),
                "/assets/images/profile-banner.jpg",
                "Complete your profile to get personalized outfit recommendations"
        )));
        return "index";
    }

    @GetMapping("/payment")
    public String showPayment(Model model, HttpServletRequest request, Authentication authentication) throws Exception {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userService.getCurrentUser(userPrincipal);

        // Update user status to PAYMENT_PENDING
        user.setOnboardingStatus(OnboardingStatus.PAYMENT_PENDING);
        userService.saveUser(user);

        // Create Stripe checkout session
        String checkoutUrl = stripeService.createCheckoutSession(user.getEmail(), user.getId());

        // Redirect to Stripe checkout
        return "redirect:" + checkoutUrl;
    }

    @GetMapping("/success")
    public String showSuccess(Model model, HttpServletRequest request) {
        model.addAttribute("content", "fragments/success");
        model.addAllAttributes(Collections.singleton(createOpenGraphData(
                "Payment Successful - OutfitGenerator",
                request.getRequestURL().toString(),
                "/assets/images/success-banner.jpg",
                "Thank you for becoming a premium member"
        )));
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