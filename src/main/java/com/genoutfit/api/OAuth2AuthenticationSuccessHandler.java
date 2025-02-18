package com.genoutfit.api;

import com.genoutfit.api.model.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider tokenProvider;
    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider tokenProvider,
            @Value("${BASE_URL}/oauth2/callback/google")  String redirectUri) {
        this.tokenProvider = tokenProvider;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, IOException {
        String targetUrl = redirectUri;

        if (response.isCommitted()) {
            return;
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.createToken(userPrincipal);

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}