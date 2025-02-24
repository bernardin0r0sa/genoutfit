package com.genoutfit.api.controller;

import com.genoutfit.api.JwtTokenProvider;
import com.genoutfit.api.model.*;
import com.genoutfit.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request) {
        model.addAttribute("content", "login :: login");
        model.addAllAttributes(createOpenGraphData(
                "Log In - OutfitGenerator",
                request.getRequestURL().toString(),
                "/assets/images/login-banner.jpg",
                "Log in to your OutfitGenerator account"
        ));
        return "index";
    }

    @GetMapping("/register")
    public String register(Model model, HttpServletRequest request) {
        model.addAttribute("content", "register :: register");
        model.addAllAttributes(createOpenGraphData(
                "Sign Up - OutfitGenerator",
                request.getRequestURL().toString(),
                "/assets/images/signup-banner.jpg",
                "Create your OutfitGenerator account"
        ));
        return "index";
    }

    private Map<String, String> createOpenGraphData(String title, String url, String imageUrl, String description) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("ogPageTitle", title);
        attributes.put("ogCurrentUrl", url);
        attributes.put("ogImageUrl", imageUrl);
        attributes.put("ogPageDescription", description);
        return attributes;
    }
}