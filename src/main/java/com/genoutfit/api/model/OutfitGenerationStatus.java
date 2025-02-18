package com.genoutfit.api.model;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Response object for outfit generation status endpoints
 */
@Data
@AllArgsConstructor
public class OutfitGenerationStatus {
    private boolean isComplete;
    private List<String> imageUrls;
    private List<String> errors;
}