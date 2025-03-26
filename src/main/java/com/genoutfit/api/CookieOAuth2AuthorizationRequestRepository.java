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

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Component
public class CookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final String SESSION_AUTHORIZATION_REQUEST_KEY = "OAUTH2_AUTH_REQUEST";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String requestId = request.getParameter("requestId"); // Get requestId from URL

        if (requestId != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                OAuth2AuthorizationRequest authRequest = (OAuth2AuthorizationRequest) session.getAttribute(requestId);
                if (authRequest != null) {
                    System.out.println("Authorization request found in session for requestId: " + requestId);
                    return authRequest;
                }
            }
        }
        System.out.println("No authorization request found for requestId: " + requestId);
        return null;
    }


    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            return;
        }
        System.out.println("::CookieOAuth2AuthorizationRequestRepository::");
        System.out.println("::saveAuthorizationRequest::");
        // Generate a unique requestId
        String requestId = UUID.randomUUID().toString();
        System.out.println("requestId:"+requestId);

        // Store authorization request in session using requestId
        request.getSession().setAttribute(requestId, authorizationRequest);
        System.out.println("request.getSession().setAttribute(requestId, authorizationRequest):");

        // Modify the redirect URI to include the requestId
        String redirectUri = authorizationRequest.getRedirectUri() + "?requestId=" + requestId;
        System.out.println("redirectUri:"+redirectUri);


        try {
            response.sendRedirect(redirectUri); // Redirect with requestId
        } catch (IOException e) {
            System.out.println("IOException:"+e.getMessage());
            throw new RuntimeException("Failed to redirect", e);
        }
    }



    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        String requestId = request.getParameter("requestId");
        if (requestId != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                OAuth2AuthorizationRequest authRequest = (OAuth2AuthorizationRequest) session.getAttribute(requestId);
                session.removeAttribute(requestId); // Remove from session
                System.out.println("Authorization request removed from session for requestId: " + requestId);
                return authRequest;
            }
        }
        return null;
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
