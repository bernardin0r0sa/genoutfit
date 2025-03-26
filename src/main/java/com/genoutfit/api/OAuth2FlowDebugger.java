package com.genoutfit.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Enumeration;

@Component
public class OAuth2FlowDebugger {
    private static final Logger log = LoggerFactory.getLogger(OAuth2FlowDebugger.class);

    public void logOAuth2Flow(HttpServletRequest request) {
        log.error("OAuth2 Flow Debug:");
        log.error("Request URL: {}", request.getRequestURL());
        log.error("User Agent: {}", request.getHeader("User-Agent"));
        log.error("Request Method: {}", request.getMethod());
        log.error("Query String: {}", request.getQueryString());

        // Log all parameters
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            log.error("Param - {}: {}", paramName, request.getParameter(paramName));
        }

        // Log session information
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.error("Session ID: {}", session.getId());
            Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String attrName = attributeNames.nextElement();
                log.error("Session Attribute - {}: {}", attrName, session.getAttribute(attrName));
            }
        }
    }
}