package com.genoutfit.api;

import com.genoutfit.api.model.User;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static com.genoutfit.api.model.OnboardingStatus.*;

@Component
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
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, IOException {

        if (response.isCommitted()) {
            return;
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.createToken(userPrincipal);

        // Set token as cookie for future requests
        Cookie authCookie = new Cookie("authToken", token);
        authCookie.setPath("/");
        authCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        authCookie.setHttpOnly(true);
        response.addCookie(authCookie);

        try {
            // Get user and check onboarding status
            User user = userService.getUserById(userPrincipal.getId());
            String nextStep = getNextStep(user);

            // Add token as URL parameter for first request (will be picked up by filter)
            String targetUrl = nextStep + "?token=" + token;

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            logger.error("Error determining redirect URL", e);
            getRedirectStrategy().sendRedirect(request, response, "/error");
        }
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