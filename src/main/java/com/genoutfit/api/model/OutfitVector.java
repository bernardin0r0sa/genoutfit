package com.genoutfit.api.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class OutfitVector {
    private String id;
    private Map<String, Object> metadata;
    private Map<String, List<String>> clothingPieces;
    private List<String> colors;
    private String style;
    private Map<String, Object> additionalMetadata;
}