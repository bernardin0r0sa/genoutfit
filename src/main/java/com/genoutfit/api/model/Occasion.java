package com.genoutfit.api.model;

public enum Occasion {
    DATE_NIGHT("Date Night"),
    OFFICE_PARTY("Office Party"),
    WEDDING_GUEST("Wedding Guest"),
    CASUAL_OUTING("Casual Outing"),
    FORMAL_EVENT("Formal Event"),
    BEACH_VACATION("Beach Vacation"),
    BUSINESS_CASUAL("Business Casual"),
    PARTY("Party"),
    GALA("Gala");

    private final String displayName;

    Occasion(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}