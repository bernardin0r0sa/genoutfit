package com.genoutfit.api;

import com.genoutfit.api.service.CustomOAuth2UserService;
import com.genoutfit.api.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    private final JwtTokenProvider tokenProvider;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/process-login").permitAll()
                        .requestMatchers("/", "/home", "/login", "/register", "/auth/**", "/oauth2/**").permitAll()

                        // Public API endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // Plan selection endpoints (must be public)
                        .requestMatchers("/api/onboarding/select-plan").permitAll()
                        .requestMatchers("/onboarding/set-plan").permitAll()
                        .requestMatchers("/onboarding/confirm-plan").permitAll()


                        // Onboarding endpoints - require authentication
                        .requestMatchers("/api/onboarding/set-plan-after-login").authenticated()
                        .requestMatchers("/api/onboarding/profile").authenticated()
                        .requestMatchers("/api/onboarding/proceed-to-payment").authenticated()
                        .requestMatchers("/onboarding/profile", "/onboarding/payment").authenticated()

                        // Stripe webhook (must be public)
                        .requestMatchers("/api/onboarding/webhook/stripe").permitAll()

                        // Success page (requires authentication)
                        .requestMatchers("/onboarding/success").authenticated()

                        // Protected API endpoints (requires PAID_USER role for full access)
                        .requestMatchers("/api/outfits/**").hasRole("PAID_USER")

                        // Account/billing pages (requires authentication)
                        .requestMatchers("/account").authenticated()
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/subscription/**").authenticated()

                        // All other requests need authentication
                        .anyRequest().authenticated()
                )
                // Disable form login since we're using a REST API
                .formLogin(formLogin -> formLogin.disable())
                .oauth2Login(oauth2Login -> oauth2Login
                        .authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint
                                .baseUri("/oauth2/authorize")
                        )
                        .redirectionEndpoint(redirectionEndpoint -> redirectionEndpoint
                                .baseUri("/oauth2/callback/*")
                        )
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )
                // Add custom authentication entry point for handling authentication errors
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\""
                                    + authException.getMessage() + "\"}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}