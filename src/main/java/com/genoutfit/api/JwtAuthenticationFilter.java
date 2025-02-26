package com.genoutfit.api;

import com.genoutfit.api.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Remove @Component since we're creating this as a bean in SecurityConfig
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String userId = tokenProvider.getUserIdFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserById(userId);

                System.out.println(userDetails.getAuthorities());

                // Add this debug line
               // logger.debug((Object) "User authorities: {}", (Throwable) userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        logger.debug("Extracting JWT from request...");

        // First try to get from Authorization header
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Authorization header: " + (bearerToken != null ? "Present" : "Not present"));

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            logger.debug("Returning JWT from Authorization header");
            return bearerToken.substring(7);
        }

        // If not in header, check cookies
        Cookie[] cookies = request.getCookies();
        logger.debug("Cookies: " + (cookies != null ? cookies.length + " cookies found" : "No cookies found"));

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                logger.debug("Cookie found: " + cookie.getName() + " = " +
                        (cookie.getValue() != null ? cookie.getValue().substring(0, Math.min(10, cookie.getValue().length())) + "..." : "null"));

                if ("authToken".equals(cookie.getName())) {
                    logger.debug("authToken cookie found");
                    return cookie.getValue();
                }
            }
        } else {
            // Try to manually parse cookie header as a fallback
            logger.debug("getCookies() returned null, trying to parse Cookie header manually");
            String cookieHeader = request.getHeader("Cookie");
            if (cookieHeader != null) {
                logger.debug("Cookie header found: " + cookieHeader);

                String[] cookiesManually = cookieHeader.split(";");
                for (String cookie : cookiesManually) {
                    cookie = cookie.trim();
                    logger.debug("Parsing cookie string: " + cookie);

                    if (cookie.startsWith("authToken=")) {
                        String token = cookie.substring("authToken=".length());
                        logger.debug("authToken found in header with value: " +
                                (token.length() > 10 ? token.substring(0, 10) + "..." : token));
                        return token;
                    }
                }
            } else {
                logger.debug("No Cookie header found");
            }
        }

        logger.debug("No JWT found in request");
        return null;
    }
}