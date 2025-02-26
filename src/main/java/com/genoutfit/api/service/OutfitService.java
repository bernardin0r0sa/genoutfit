package com.genoutfit.api.service;

import com.genoutfit.api.model.Occasion;
import com.genoutfit.api.model.Outfit;
import com.genoutfit.api.model.OutfitResponseDto;
import com.genoutfit.api.repository.FavoriteOutfitRepository;
import com.genoutfit.api.repository.OutfitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutfitService {
    @Autowired
    OutfitRepository outfitRepository;
   @Autowired
   FavoriteOutfitRepository favoriteOutfitRepository;

    /**
     * Get all outfits for a user with optional filtering
     */
    public List<Outfit> getRecentOutfits(String userId, int limit) {
        return outfitRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }

    /**
     * Get favorite outfits for a user
     */
    public List<Outfit> getFavoriteOutfits(String userId) {
        return outfitRepository.findByUserIdAndFavoriteTrue(userId);
    }

    /**
     * Get favorite outfits as DTOs
     */
    public List<OutfitResponseDto> getFavoriteOutfitsDto(String userId, Occasion occasion) {
        List<Outfit> outfits;
        
        if (occasion != null) {
            outfits = outfitRepository.findByUserIdAndFavoriteTrueAndOccasion(userId, occasion);
        } else {
            outfits = outfitRepository.findByUserIdAndFavoriteTrue(userId);
        }
        
        return outfits.stream()
                .map(this::mapToOutfitResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all outfits for a user with pagination and filtering
     */
    public Page<Outfit> getAllOutfits(String userId, int page, int size, Occasion occasion, String sort) {
        Pageable pageable;
        
        // Determine sort direction
        if ("oldest".equals(sort)) {
            pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        } else {
            // Default to newest first
            pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        }
        
        // Apply filters
        if (occasion != null) {
            return outfitRepository.findByUserIdAndOccasion(userId, occasion, pageable);
        } else {
            return outfitRepository.findByUserId(userId, pageable);
        }
    }

    /**
     * Get all outfits as DTOs with filtering
     */
    public List<OutfitResponseDto> getOutfits(String userId, Occasion occasion, String sort) {
        List<Outfit> outfits;
        
        // Determine sort and filtering
        if (occasion != null) {
            if ("oldest".equals(sort)) {
                outfits = outfitRepository.findByUserIdAndOccasionOrderByCreatedAtAsc(userId, occasion);
            } else {
                outfits = outfitRepository.findByUserIdAndOccasionOrderByCreatedAtDesc(userId, occasion);
            }
        } else {
            if ("oldest".equals(sort)) {
                outfits = outfitRepository.findByUserIdOrderByCreatedAtAsc(userId);
            } else {
                outfits = outfitRepository.findByUserIdOrderByCreatedAtDesc(userId);
            }
        }
        
        return outfits.stream()
                .map(this::mapToOutfitResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get outfit by ID
     */
    public Outfit getOutfitById(String outfitId, String userId) throws Exception {
        Outfit outfit = outfitRepository.findById(outfitId)
                .orElseThrow(() -> new Exception("Outfit not found"));
        
        // Ensure user has access to this outfit
        if (!outfit.getUser().getId().equals(userId)) {
            throw new Exception("Unauthorized access to outfit");
        }
        
        return outfit;
    }

    /**
     * Get outfit details as DTO
     */
    public OutfitResponseDto getOutfitDetailsDto(String outfitId, String userId) throws Exception {
        Outfit outfit = getOutfitById(outfitId, userId);
        return mapToOutfitResponse(outfit);
    }

    /**
     * Toggle favorite status of an outfit
     */
    @Transactional
    public boolean toggleFavorite(String outfitId, String userId) throws Exception {
        Outfit outfit = outfitRepository.findById(outfitId)
                .orElseThrow(() -> new Exception("Outfit not found"));
        
        // Ensure user has access to this outfit
        if (!outfit.getUser().getId().equals(userId)) {
            throw new Exception("Unauthorized access to outfit");
        }
        
        // Toggle favorite status
        boolean newStatus = !outfit.isFavorite();
        outfit.setFavorite(newStatus);
        outfitRepository.save(outfit);
        
        return newStatus;
    }

    /**
     * Map Outfit entity to OutfitResponseDto
     */
    private OutfitResponseDto mapToOutfitResponse(Outfit outfit) {
        Map<String, String> clothingDetails = null;
        
        // Parse clothing details if available
        if (outfit.getClothingDetails() != null && !outfit.getClothingDetails().isEmpty()) {
            clothingDetails = Map.of("description", outfit.getClothingDetails());
        }
        
        return OutfitResponseDto.builder()
                .id(outfit.getId())
                .imageUrls(outfit.getImageUrls())
                .clothingDetails(clothingDetails)
                .occasion(outfit.getOccasion().getDisplayName())
                .style(outfit.getStyle())
                .createdAt(outfit.getCreatedAt())
                .favorite(outfit.isFavorite())
                .build();
    }
}
