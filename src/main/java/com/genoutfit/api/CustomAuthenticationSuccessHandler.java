package com.genoutfit.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genoutfit.api.model.UserPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        System.out.println(":::CustomAuthenticationSuccessHandler::::");
        System.out.println(":::onAuthenticationSuccess::::");

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.createToken(userPrincipal);

        System.out.println("token:"+token);


        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", token);
        responseBody.put("user", Map.of(
                "id", userPrincipal.getId(),
                "email", userPrincipal.getEmail(),
                "roles", userPrincipal.getAuthorities()
        ));

        System.out.println("response:"+response.toString());
        System.out.println("responseBody:"+responseBody.toString());

        objectMapper.writeValue(response.getWriter(), responseBody);
    }
}