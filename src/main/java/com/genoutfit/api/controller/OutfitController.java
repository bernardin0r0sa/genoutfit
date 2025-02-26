package com.genoutfit.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class OutfitController {

    @GetMapping("/")
    public String landingPage(Model model, HttpServletRequest request) {
        model.addAllAttributes(createOpenGraphData(
                "OutfitGenerator - AI-Powered Fashion Recommendations",
                request.getRequestURL().toString(),
                "/assets/images/homepage-banner.jpg",
                "Get personalized outfit recommendations tailored to your style, body type, and occasion"
        ));
        return "index";
    }

    @GetMapping("/dashboard")
    public String home(Model model, HttpServletRequest request) {
        model.addAllAttributes(createOpenGraphData(
                "OutfitGenerator - AI-Powered Fashion Recommendations",
                request.getRequestURL().toString(),
                "/assets/images/homepage-banner.jpg",
                "Get personalized outfit recommendations tailored to your style, body type, and occasion"
        ));
        return "home";
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
