package com.genoutfit.api;

import com.genoutfit.api.model.OnboardingStatus;
import com.genoutfit.api.model.SubscriptionPlan;
import com.genoutfit.api.model.User;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static com.genoutfit.api.model.OnboardingStatus.*;

@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider tokenProvider;
    private final String redirectUri;

    @Autowired
    private UserService userService;


    @Value("${BASE_URL}/dashboard")
    private String dashboardUrl;

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider tokenProvider,
            @Value("${BASE_URL}/oauth2/callback/google")  String redirectUri) {
        this.tokenProvider = tokenProvider;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        System.out.println("::OAuth2AuthenticationSuccessHandler::");
        System.out.println(":onAuthenticationSuccess:");


        // Log request details
        log.info("OAuth2 Authentication Request Details:");
        log.info("User Agent: {}", request.getHeader("User-Agent"));
        log.info("Remote Address: {}", request.getRemoteAddr());
        log.info("Request URL: {}", request.getRequestURL());



        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String token = tokenProvider.createToken(userPrincipal);

            System.out.println(":token:"+token);

            log.info("Enter OAuth Sucess");


            // Detect mobile device
            boolean isMobileDevice = isMobileDevice(request);

            System.out.println(":isMobileDevice:"+isMobileDevice);


            // Get user and check onboarding status
            User user = userService.getUserById(userPrincipal.getId());

            System.out.println(":user:"+user.toString());


            // Retrieve the plan from the session
            HttpSession session = request.getSession(false);
            String plan = (session != null) ? (String) session.getAttribute("selectedPlan") : null;

            // Remove the plan from session after retrieving it
            if (session != null) {

                System.out.println(":Tem session:");

                session.removeAttribute("selectedPlan");
            }


            // If user is NEW and we have a plan, set it and update status
            if (user.getOnboardingStatus() == OnboardingStatus.NEW && plan != null) {
                System.out.println(":If user is NEW and we have a plan, set it and update status:");
                try {
                    SubscriptionPlan selectedPlan = SubscriptionPlan.valueOf(plan.toUpperCase());
                    user.setSelectedPlan(selectedPlan);
                    user.setOnboardingStatus(OnboardingStatus.PLAN_SELECTED);
                    userService.saveUser(user);
                    System.out.println(":userService.saveUser(user):");

                } catch (IllegalArgumentException e) {
                    System.out.println(":error:"+e.getMessage());
                    log.warn("Invalid plan provided during OAuth: " + plan);
                }
            }

            // Determine next step based on user's onboarding status
            String nextStep = getNextStep(user);

            // Set authentication cookie
            Cookie authCookie = new Cookie("authToken", token);
            authCookie.setPath("/");
            authCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            authCookie.setHttpOnly(true);

            System.out.println(":Cookie:"+authCookie);


            // Add SameSite and Secure attributes for better cross-platform support
            response.addHeader("Set-Cookie",
                    authCookie.getName() + "=" + authCookie.getValue() +
                            "; Path=" + authCookie.getPath() +
                            "; Max-Age=" + authCookie.getMaxAge() +
                            "; HttpOnly" +
                            "; SameSite=None" +  // Changed from Lax to None for mobile
                            "; Secure"  // Still keep Secure flag
            );
            System.out.println(":response:"+response.toString());

            // Construct redirect URL with token and status
            String redirectUrl;
            if (isMobileDevice) {
                // For mobile, add more context to the redirect
                redirectUrl = nextStep +
                        "?token=" + token +
                        "&status=" + user.getOnboardingStatus().name() +
                        "&mobile=true";
            } else {
                // For web, standard redirect
                redirectUrl = nextStep + "?token=" + token;
            }

            System.out.println("OAuth Redirect URL: " + redirectUrl);

            // Log the redirect for debugging
            log.info("OAuth Redirect URL: " + redirectUrl);

            //clearOAuth2Cookies(request, response);

            // Perform redirect
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());

            log.error("OAuth Authentication Error", e);
            // Extremely detailed error logging
            log.error("OAuth2 Authentication Failure", e);
            log.error("Exception Class: {}", e.getClass().getName());
            log.error("Exception Message: {}", e.getMessage());

            response.sendRedirect("/login?error=authentication_failed");
        }
    }

    // Mobile device detection method
    private boolean isMobileDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");

        log.info("Device Detection:");
        log.info("User Agent: {}", userAgent);
        log.info("Is Mobile Device: {}", true);

        return userAgent != null && (
                userAgent.contains("Android") ||
                        userAgent.contains("webOS") ||
                        userAgent.contains("iPhone") ||
                        userAgent.contains("iPad") ||
                        userAgent.contains("iPod") ||
                        userAgent.contains("BlackBerry")
        );
    }

    private String getNextStep(User user) {
        return switch (user.getOnboardingStatus()) {
            case NEW -> "/";  // If user is new and has no plan, go to landing page
            case PLAN_SELECTED -> "/onboarding/profile";
            case PROFILE_COMPLETED, PAYMENT_PENDING -> "/onboarding/payment";
            case COMPLETED -> "/dashboard";
        };
    }

    // Helper method to extract plan from request or session
    private String extractPlanParameter(HttpServletRequest request) {
        // Try to get from request parameter
        String plan = request.getParameter("plan");

        // If not in request, try to get from session
        if (plan == null && request.getSession() != null) {
            plan = (String) request.getSession().getAttribute("selectedPlan");
        }

        // Check cookies for plan parameter
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                logger.info("Cookie: {} = {}");
                if ("selectedPlan".equals(cookie.getName())) {
                    plan = cookie.getValue();
                }
            }
        }


        return plan;
    }

   /* private void clearOAuth2Cookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }*/

}