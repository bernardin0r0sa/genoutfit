package com.genoutfit.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.web.util.WebUtils;
import java.util.Base64;

@Component
public class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {

        System.out.println("::CookieOAuth2AuthorizationRequestRepository::");
        System.out.println("::loadAuthorizationRequest::");

        // Get the cookie from the request
        Cookie cookie = WebUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);



        // Check if the cookie exists and has a valid value
        if (cookie == null || cookie.getValue().isEmpty()) {
            System.out.println("cookie is NULL:");
            return null;
        }

        try {
            System.out.println("cookie is :"+ cookie.getValue());
            // Decode the Base64-encoded value and deserialize it
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(Base64.getDecoder().decode(cookie.getValue()));
        } catch (Exception e) {
            // Log error and return null if deserialization fails
            System.out.println("Erro cookie :"+ e);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            return;
        }
        // Ensure cookie exists before setting a value
        Cookie cookie = WebUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        if (cookie == null) {
            cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, ""); // Initialize an empty cookie
        }

        // Encode the authorization request before saving it
        String encodedValue = Base64.getEncoder().encodeToString(SerializationUtils.serialize(authorizationRequest));
        cookie.setValue(encodedValue);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(180); // Expire in 3 minutes
        cookie.setSecure(true); // Ensures cookies are only sent over HTTPS
        cookie.setAttribute("SameSite", "None"); // Allows cross-site authentication (important for OAuth2 on mobile)
        response.addCookie(cookie);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return loadAuthorizationRequest(request);
    }

    private OAuth2AuthorizationRequest deserialize(jakarta.servlet.http.Cookie cookie) {
        if (cookie == null) {
            return null;
        }
        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(Base64.getDecoder().decode(cookie.getValue()));
    }
}
