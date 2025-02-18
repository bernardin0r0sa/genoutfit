package com.genoutfit.api.controller;

import com.genoutfit.api.model.OutfitRequestDto;
import com.genoutfit.api.model.OutfitResponseDto;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.service.OutfitGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outfits")
@RequiredArgsConstructor
@Slf4j
public class OutfitController {
    private final OutfitGenerationService outfitGenerationService;

    /**
     * Initiates generation of a new outfit
     */
    @PostMapping("/generate")
    public ResponseEntity<OutfitResponseDto> generateOutfit(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OutfitRequestDto request) {

        try {
            OutfitResponseDto response = outfitGenerationService.initiateOutfitGeneration(
                    userPrincipal.getId(), request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating outfit: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate outfit", e);
        }
    }
}