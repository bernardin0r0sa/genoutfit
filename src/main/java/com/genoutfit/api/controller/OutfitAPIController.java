package com.genoutfit.api.controller;

import com.genoutfit.api.model.OutfitGenerationStatus;
import com.genoutfit.api.model.OutfitRequestDto;
import com.genoutfit.api.model.OutfitResponseDto;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.service.OutfitGenerationService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outfits")
@RequiredArgsConstructor
@Slf4j
public class OutfitAPIController {
    @Autowired
    private final OutfitGenerationService outfitGenerationService;

    @Value("${API_WEBHOOK_KEY}")
    private String apiWebhookKey;

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

    /**
     * Get the current status of outfit generation
     */
    @GetMapping("/{outfitId}/status")
    public ResponseEntity<OutfitGenerationStatus> getGenerationStatus(
            @PathVariable String outfitId) {

        OutfitGenerationStatus status = outfitGenerationService.getOutfitGenerationStatus(outfitId);
        return ResponseEntity.ok(status);
    }

    /**
     * Webhook endpoint for Fal.ai to call when image generation is complete
     */
    @PostMapping("/webhook/{outfitId}/{imageIndex}")
    public ResponseEntity<?> handleGenerationWebhook(
            @PathVariable String outfitId,
            @PathVariable int imageIndex,
            @RequestBody String payload,
            @RequestParam("apiKey") String requestApiKey) {

        // Validate the API key
        if (!apiWebhookKey.equals(requestApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid API key");
        }

        try {
            log.info("Received webhook for outfit {} image {}", outfitId, imageIndex);
            JsonObject result = JsonParser.parseString(payload).getAsJsonObject();
            outfitGenerationService.handleImageGenerationWebhook(outfitId, imageIndex, result);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }



}