package com.genoutfit.api.repository;

import com.genoutfit.api.model.Occasion;
import com.genoutfit.api.model.Outfit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, String> {
    List<Outfit> findByUserId(String userId);
    List<Outfit> findByUserIdAndOccasion(String userId, Occasion occasion);
}