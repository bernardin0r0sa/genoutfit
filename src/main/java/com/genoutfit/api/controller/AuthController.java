package com.genoutfit.api.controller;

import com.genoutfit.api.JwtTokenProvider;
import com.genoutfit.api.model.*;
import com.genoutfit.api.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request, @RequestParam(required = false) String plan) {
        model.addAttribute("content", "fragments/login");

        // Pass the plan parameter if it exists
        if (plan != null && !plan.isEmpty()) {
            model.addAttribute("selectedPlan", plan);
        }

        model.addAllAttributes(createOpenGraphData(
                "Log In - OutfitGenerator",
                request.getRequestURL().toString(),
                "/assets/images/login-banner.jpg",
                "Log in to your OutfitGenerator account"
        ));
        return "index";
    }

    @GetMapping("/onboard")
    public String register(Model model, HttpServletRequest request,
                           @RequestParam(required = false) String plan,
                           @RequestParam(required = false) Boolean onboarding,
                           Authentication authentication,
                           HttpSession session) {


        model.addAttribute("content", "fragments/register");

        // Store plan in session for use after registration if provided
        if (plan != null && !plan.isEmpty()) {
            session.setAttribute("selectedPlan", plan);
            model.addAttribute("selectedPlan", plan);
        }

        model.addAllAttributes(createOpenGraphData(
                "Sign Up - OutfitGenerator",
                request.getRequestURL().toString(),
                "/assets/images/signup-banner.jpg",
                "Create your OutfitGenerator account"
        ));

        return "index";
    }

    @PostMapping("/process-login")
    public String processLogin(@RequestParam String email,
                               @RequestParam String password,
                               @RequestParam(required = false) String planCode,
                               HttpServletResponse response,
                               RedirectAttributes redirectAttributes) throws Exception {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            System.out.println(":::AUTHCONTROLLER:::");
            System.out.println(":/process-login:");


            // Set security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            System.out.println(":SecurityContextHolder.getContext():");

            // Create JWT token
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String token = tokenProvider.createToken(userPrincipal);

            System.out.println(":token:"+token);

            // Set JWT as an HTTP-only cookie
            Cookie cookie = new Cookie("authToken", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(cookie);

            System.out.println(":cookie:"+cookie);

            // Check if plan code is provided
            if (planCode != null && !planCode.isEmpty()) {
                System.out.println(":has planCode:"+planCode);
                // Get user and update with selected plan
                User user = userService.getCurrentUser(userPrincipal);

                try {
                    SubscriptionPlan plan = SubscriptionPlan.valueOf(planCode.toUpperCase());
                    user.setSelectedPlan(plan);
                    user.setOnboardingStatus(OnboardingStatus.PLAN_SELECTED);
                    userService.saveUser(user);
                    System.out.println(":userService.saveUser(user):");
                    // Redirect to the appropriate next step in onboarding
                    return "redirect:/onboarding/profile";
                } catch (IllegalArgumentException e) {
                    System.out.println(":IllegalArgumentException:"+e.getMessage());

                    // Invalid plan, continue with normal flow
                }
            }
            System.out.println(":No plan Code:");

            // Check onboarding status and redirect accordingly
            User user = userService.getCurrentUser(userPrincipal);
            String nextStep = getNextStep(user);

            System.out.println(":nextStep:"+nextStep);

            // Redirect to the appropriate next step
            return "redirect:" + nextStep;

        } catch (AuthenticationException e) {
           System.out.println("Error on Authentication:"+e);
            return "redirect:/login?error=true";
        }
    }

    // Helper method to determine next step based on onboarding status
    private String getNextStep(User user) {
        return switch (user.getOnboardingStatus()) {
            case NEW -> "/";  // If user is new and has no plan, go to landing page
            case PLAN_SELECTED -> "/onboarding/profile";
            case PROFILE_COMPLETED, PAYMENT_PENDING -> "/onboarding/payment";
            case COMPLETED -> "/dashboard";
        };
    }

    private Map<String, String> createOpenGraphData(String title, String url, String imageUrl, String description) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("ogPageTitle", title);
        attributes.put("ogCurrentUrl", url);
        attributes.put("ogImageUrl", imageUrl);
        attributes.put("ogPageDescription", description);
        return attributes;
    }

    @GetMapping("/oauth2/authorize/google")
    public String authorizeGoogle(@RequestParam(required = false) String plan, HttpServletRequest request) {
        System.out.println(":/oauth2/authorize/google:");
        if (plan != null) {
            System.out.println(":plan:"+plan);
            request.getSession().setAttribute("selectedPlan", plan);
            System.out.println(":request.getSession().setAttribute:"+plan);
        }
        // Redirect to Spring Security's OAuth2 authorization endpoint
        System.out.println(":redirect:/oauth2/authorization/google:");
        return "redirect:/oauth2/authorization/google";
    }

}