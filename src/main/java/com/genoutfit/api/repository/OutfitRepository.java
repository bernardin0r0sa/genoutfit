package com.genoutfit.api.repository;

import com.genoutfit.api.model.Occasion;
import com.genoutfit.api.model.Outfit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, String> {
    List<Outfit> findByUserId(String userId);
    List<Outfit> findByUserIdAndOccasion(String userId, Occasion occasion);

    List<Outfit> findByUserIdOrderByCreatedAtDesc(String userId, PageRequest of);

    List<Outfit> findByUserIdAndFavoriteTrue(String userId);

    List<Outfit> findByUserIdAndFavoriteTrueAndOccasion(String userId, Occasion occasion);

    List<Outfit> findByUserIdAndOccasionOrderByCreatedAtAsc(String userId, Occasion occasion);

    List<Outfit> findByUserIdAndOccasionOrderByCreatedAtDesc(String userId, Occasion occasion);

    List<Outfit> findByUserIdOrderByCreatedAtAsc(String userId);

    List<Outfit> findByUserIdOrderByCreatedAtDesc(String userId);

    Page<Outfit> findByUserId(String userId, Pageable pageable);

    Page<Outfit> findByUserIdAndOccasion(String userId, Occasion occasion, Pageable pageable);
}