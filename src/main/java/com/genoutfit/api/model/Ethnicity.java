package com.genoutfit.api.model;

public enum Ethnicity {
    EAST_ASIAN("East Asian"),
    SOUTH_ASIAN("South Asian"),
    SOUTHEAST_ASIAN("Southeast Asian"),
    BLACK_LIGHT("Black - Light Skin"),
    BLACK_MEDIUM("Black - Medium Skin"),
    BLACK_DARK("Black - Dark Skin"),
    WHITE_FAIR("White - Fair Skin"),
    WHITE_MEDIUM("White - Medium Skin"),
    WHITE_OLIVE("White - Olive Skin"),
    MIDDLE_EASTERN("Middle Eastern"),
    LATINO_LIGHT("Latino - Light Skin"),
    LATINO_MEDIUM("Latino - Medium Skin"),
    LATINO_DARK("Latino - Dark Skin"),
    MIXED("Mixed Heritage");

    private final String displayName;

    Ethnicity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Helper method to get AI prompt description
    public String getAIPromptDescription() {
        return switch (this) {
            case EAST_ASIAN -> "East Asian features and complexion";
            case SOUTH_ASIAN -> "South Asian features and complexion";
            case SOUTHEAST_ASIAN -> "Southeast Asian features and complexion";
            case BLACK_LIGHT -> "Black model with light complexion";
            case BLACK_MEDIUM -> "Black model with medium brown complexion";
            case BLACK_DARK -> "Black model with dark complexion";
            case WHITE_FAIR -> "White model with fair complexion";
            case WHITE_MEDIUM -> "White model with medium complexion";
            case WHITE_OLIVE -> "White model with olive complexion";
            case MIDDLE_EASTERN -> "Middle Eastern features and complexion";
            case LATINO_LIGHT -> "Latino model with light complexion";
            case LATINO_MEDIUM -> "Latino model with medium complexion";
            case LATINO_DARK -> "Latino model with darker complexion";
            case MIXED -> "Mixed race features and complexion";
        };
    }
}