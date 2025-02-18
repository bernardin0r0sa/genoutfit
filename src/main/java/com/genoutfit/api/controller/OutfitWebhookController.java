package com.genoutfit.api.controller;

import com.genoutfit.api.model.OutfitGenerationStatus;
import com.genoutfit.api.service.OutfitGenerationService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/outfits")
@RequiredArgsConstructor
@Slf4j
public class OutfitWebhookController {
    private final OutfitGenerationService outfitGenerationService;

    /**
     * Webhook endpoint for Fal.ai to call when image generation is complete
     */
    @PostMapping("/webhook/{outfitId}/{imageIndex}")
    public ResponseEntity<?> handleGenerationWebhook(
            @PathVariable String outfitId,
            @PathVariable int imageIndex,
            @RequestBody String payload) {

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

    /**
     * Get the current status of outfit generation
     */
    @GetMapping("/{outfitId}/status")
    public ResponseEntity<OutfitGenerationStatus> getGenerationStatus(
            @PathVariable String outfitId) {

        OutfitGenerationStatus status = outfitGenerationService.getOutfitGenerationStatus(outfitId);
        return ResponseEntity.ok(status);
    }
}