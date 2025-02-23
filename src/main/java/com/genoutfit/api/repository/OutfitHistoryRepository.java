package com.genoutfit.api.repository;

import com.genoutfit.api.model.Occasion;
import com.genoutfit.api.model.OutfitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface OutfitHistoryRepository extends JpaRepository<OutfitHistory, Long> {
    // Find all outfits shown to a user for a specific occasion
    List<OutfitHistory> findByUserIdAndOccasion(String userId, Occasion occasion);

    // Check if a specific outfit has been shown to a user
    boolean existsByUserIdAndOutfitId(String userId, String outfitId);

    // Find all outfit IDs shown to a user
    @Query("SELECT oh.outfitId FROM OutfitHistory oh WHERE oh.userId = :userId")
    List<String> findOutfitIdsByUserId(@Param("userId") String userId);

    void deleteByUserId(String userId);
}