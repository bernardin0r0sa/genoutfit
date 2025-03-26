package com.genoutfit.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.web.util.WebUtils;
import java.util.Base64;

@Component
public class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final String SESSION_AUTHORIZATION_REQUEST_KEY = "OAUTH2_AUTH_REQUEST";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        System.out.println("::CookieOAuth2AuthorizationRequestRepository::");
        System.out.println("::loadAuthorizationRequest::");

        // Try to get the cookie from the request
        Cookie cookie = WebUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);

        if (cookie == null || cookie.getValue().isEmpty()) {
            System.out.println("Cookie is NULL or empty. Checking session...");
            return getAuthorizationRequestFromSession(request);
        }

        try {
            System.out.println("Cookie found: " + cookie.getValue());
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(Base64.getDecoder().decode(cookie.getValue()));
        } catch (Exception e) {
            System.out.println("Error decoding cookie: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            return;
        }

        // Store in session FIRST
        HttpSession session = request.getSession(true); // Ensure session exists
        session.setAttribute(SESSION_AUTHORIZATION_REQUEST_KEY, authorizationRequest);
        System.out.println("Authorization request saved in SESSION.");

        try {
            // Also store in cookie as a backup
            String encodedValue = Base64.getEncoder().encodeToString(SerializationUtils.serialize(authorizationRequest));
            Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, encodedValue);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(COOKIE_EXPIRE_SECONDS);
            cookie.setSecure(true);
            cookie.setAttribute("SameSite", "None");
            response.addCookie(cookie);

            System.out.println("Authorization request saved in COOKIE.");
        } catch (Exception e) {
            System.out.println("Error saving authorization request in cookie: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            // Remove from both cookie and session
            removeCookie(response);
            removeAuthorizationRequestFromSession(request);
        }
        return authorizationRequest;
    }

    private OAuth2AuthorizationRequest getAuthorizationRequestFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            System.out.println("Session ID: " + session.getId()); // Log session ID
            OAuth2AuthorizationRequest authRequest = (OAuth2AuthorizationRequest) session.getAttribute(SESSION_AUTHORIZATION_REQUEST_KEY);
            if (authRequest != null) {
                System.out.println("Authorization request FOUND in session.");
                return authRequest;
            }
        }
        System.out.println("No authorization request found in session.");
        return null;
    }


    private void removeAuthorizationRequestFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_AUTHORIZATION_REQUEST_KEY);
            System.out.println("Authorization request removed from session.");
        }
    }

    private void removeCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Delete the cookie
        cookie.setSecure(true);
        response.addCookie(cookie);
        System.out.println("Authorization request cookie removed.");
    }
}
