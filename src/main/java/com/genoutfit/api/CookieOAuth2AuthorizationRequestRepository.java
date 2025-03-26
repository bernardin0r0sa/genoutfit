package com.genoutfit.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

        private static final Logger log = LoggerFactory.getLogger(CookieOAuth2AuthorizationRequestRepository.class);

        @Autowired
        private OAuth2FlowDebugger debugger;

        @Override
        public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
            log.error("LOAD AUTHORIZATION REQUEST CALLED");
            debugger.logOAuth2Flow(request);

            String requestId = request.getParameter("requestId");
            log.error("RequestId from parameters: {}", requestId);

            if (requestId != null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    OAuth2AuthorizationRequest authRequest =
                            (OAuth2AuthorizationRequest) session.getAttribute(requestId);

                    log.error("Authorization Request Found: {}", authRequest != null);
                    return authRequest;
                }
            }

            log.error("No Authorization Request Found");
            return null;
        }

        @Override
        public void saveAuthorizationRequest(
                OAuth2AuthorizationRequest authorizationRequest,
                HttpServletRequest request,
                HttpServletResponse response
        ) {
            log.error("SAVE AUTHORIZATION REQUEST CALLED");
            debugger.logOAuth2Flow(request);

            if (authorizationRequest == null) {
                log.error("Authorization Request is NULL");
                return;
            }

            String requestId = UUID.randomUUID().toString();
            log.error("Generated RequestId: {}", requestId);

            HttpSession session = request.getSession(true);
            session.setAttribute(requestId, authorizationRequest);

            log.error("Authorization Request saved in session");
            log.error("Session ID: {}", session.getId());
        }

        @Override
        public OAuth2AuthorizationRequest removeAuthorizationRequest(
                HttpServletRequest request,
                HttpServletResponse response
        ) {
            log.error("REMOVE AUTHORIZATION REQUEST CALLED");
            debugger.logOAuth2Flow(request);

            String requestId = request.getParameter("requestId");
            if (requestId != null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    OAuth2AuthorizationRequest authRequest =
                            (OAuth2AuthorizationRequest) session.getAttribute(requestId);
                    session.removeAttribute(requestId);
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
