package com.genoutfit.api.model;

import java.util.List;

public enum PromptStyle {
    EDITORIAL("High-end fashion magazine style with artistic composition"),
    STREET("Authentic street photography with urban elements"),
    LIFESTYLE("Natural, candid moments in real-world settings"),
    CORPORATE("Professional business environment photography"),
    SOCIAL("Social media optimized style with contemporary appeal"),
    LUXURY("High-end luxury fashion photography"),
    CELEBRITY("Red carpet and celebrity portrait style"),
    RETRO("Vintage-inspired photography with classic elements"),
    CASUAL("Relaxed, everyday style photography");

    private final String description;

    PromptStyle(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // Helper method to get lighting recommendations
    public String getLightingRecommendation() {
        return switch (this) {
            case EDITORIAL -> "Dramatic studio lighting or carefully controlled natural light";
            case STREET -> "Natural urban lighting with emphasis on authentic environment";
            case LIFESTYLE -> "Soft natural lighting with minimal artificial enhancement";
            case CORPORATE -> "Clean, professional lighting with minimal shadows";
            case SOCIAL -> "Bright, flattering lighting optimized for social media";
            case LUXURY -> "Sophisticated lighting with attention to detail and mood";
            case CELEBRITY -> "Glamorous lighting with emphasis on subject";
            case RETRO -> "Period-appropriate lighting effects";
            case CASUAL -> "Natural, unposed lighting scenarios";
        };
    }

    // Helper method to get composition recommendations
    public String getCompositionGuidelines() {
        return switch (this) {
            case EDITORIAL -> "Strong artistic composition with fashion focus";
            case STREET -> "Dynamic urban compositions with environmental context";
            case LIFESTYLE -> "Natural framing with environmental storytelling";
            case CORPORATE -> "Clean, professional compositions with business context";
            case SOCIAL -> "Instagram-optimized framing with strong visual appeal";
            case LUXURY -> "Elegant compositions emphasizing luxury elements";
            case CELEBRITY -> "Classic portrait compositions with glamour focus";
            case RETRO -> "Period-inspired compositions with vintage elements";
            case CASUAL -> "Relaxed, natural compositions with authentic feel";
        };
    }

    // Helper method to get recommended aspect ratios
    public List<String> getRecommendedAspectRatios() {
        return switch (this) {
            case EDITORIAL -> List.of("4:5", "2:3");
            case STREET -> List.of("4:5", "1:1");
            case LIFESTYLE -> List.of("4:5", "16:9");
            case CORPORATE -> List.of("3:2", "4:3");
            case SOCIAL -> List.of("4:5", "1:1", "9:16");
            case LUXURY -> List.of("16:9", "2:3");
            case CELEBRITY -> List.of("2:3", "4:5");
            case RETRO -> List.of("1:1", "4:3");
            case CASUAL -> List.of("4:5", "1:1");
        };
    }

    // Helper method to check if a style is typically social media oriented
    public boolean isSocialMediaOriented() {
        return this == SOCIAL || this == LIFESTYLE || this == CASUAL;
    }

    // Helper method to get color treatment recommendations
    public String getColorTreatment() {
        return switch (this) {
            case EDITORIAL -> "Rich, fashion-forward color grading";
            case STREET -> "Urban-inspired color palette with strong contrasts";
            case LIFESTYLE -> "Natural, true-to-life colors";
            case CORPORATE -> "Professional, neutral color palette";
            case SOCIAL -> "Bright, vibrant colors optimized for social media";
            case LUXURY -> "Sophisticated color palette with rich tones";
            case CELEBRITY -> "Glamorous color treatment with emphasis on subject";
            case RETRO -> "Period-appropriate color treatment or vintage effects";
            case CASUAL -> "Natural, unfiltered color treatment";
        };
    }
}
