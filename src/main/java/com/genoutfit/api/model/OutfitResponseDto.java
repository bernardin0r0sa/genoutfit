package com.genoutfit.api.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class OutfitResponseDto {
    private String id;
    private List<String> imageUrls;
    private Map<String, String> clothingDetails;
    private String occasion;
    private String style;
    private LocalDateTime createdAt;
}