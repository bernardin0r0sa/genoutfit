package com.genoutfit.api.model;

import lombok.Data;

@Data
public class OutfitRequestDto {
    private Occasion occasion;
    private String customOccasion;
    private boolean usePreviousPreferences;
}