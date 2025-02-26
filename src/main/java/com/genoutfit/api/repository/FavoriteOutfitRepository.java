package com.genoutfit.api.repository;

import com.genoutfit.api.model.FavoriteOutfit;
import com.genoutfit.api.model.Occasion;
import com.genoutfit.api.model.Outfit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface FavoriteOutfitRepository extends JpaRepository<FavoriteOutfit, Long> {
    List<FavoriteOutfit> findByUserId(String userId);
    boolean existsByUserIdAndOutfitId(String userId, String outfitId);
    void deleteByUserIdAndOutfitId(String userId, String outfitId);
}
