package com.genoutfit.api.model;

public enum BodyType {
    MEDIUM("Medium (proportionate build)"),
    PETITE("Petite (smaller frame)"),
    ATHLETIC("Athletic (fit/toned build)"),
    CURVY("Curvy (hourglass shape)"),
    PLUS_SIZE("Plus-size (full-figured)"),
    SLIM("Slim (lean build)");

    private final String displayName;

    BodyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
