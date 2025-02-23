package com.genoutfit.api.repository;


import com.genoutfit.api.model.OutfitReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitReferenceRepository extends JpaRepository<OutfitReference, Long> {

    /**
     * Find outfits matching criteria using MySQL JSON functions
     */
    @Query(value = """
        SELECT * FROM outfit_references
        WHERE 
        (:gender IS NULL OR gender_style = :gender)
        AND JSON_CONTAINS(LOWER(body_types), LOWER(CAST(:bodyType AS JSON)))
        AND (
            JSON_CONTAINS(LOWER(occasions), LOWER(CAST(:occasion AS JSON)))
            OR JSON_CONTAINS(LOWER(occasions), '"casual"') AND :occasion = '"CASUAL_OUTING"'
            OR JSON_CONTAINS(LOWER(occasions), '"business"') AND :occasion = '"BUSINESS_CASUAL"'
            OR JSON_CONTAINS(LOWER(occasions), '"party"') AND :occasion = '"PARTY"'
            OR JSON_CONTAINS(LOWER(occasions), '"date"') AND :occasion = '"DATE_NIGHT"'
            OR JSON_CONTAINS(LOWER(occasions), '"formal"') AND :occasion = '"FORMAL_EVENT"'
            OR JSON_CONTAINS(LOWER(occasions), '"wedding"') AND :occasion = '"WEDDING_GUEST"'
            OR JSON_CONTAINS(LOWER(occasions), '"beach"') AND :occasion = '"BEACH_VACATION"'
        )
        LIMIT :limit
        """, nativeQuery = true)
    List<OutfitReference> findByBasicCriteria(
            @Param("gender") String gender,
            @Param("bodyType") String bodyType,
            @Param("occasion") String occasion,
            @Param("limit") int limit);

    /**
     * Find outfits matching criteria including style preferences
     */
    @Query(value = """
        SELECT * FROM outfit_references
        WHERE 
        (:gender IS NULL OR gender_style = :gender)
        AND JSON_CONTAINS(LOWER(body_types), LOWER(CAST(:bodyType AS JSON)))
        AND (
            JSON_CONTAINS(LOWER(occasions), LOWER(CAST(:occasion AS JSON)))
            OR JSON_CONTAINS(LOWER(occasions), '"casual"') AND :occasion = '"CASUAL_OUTING"'
            OR JSON_CONTAINS(LOWER(occasions), '"business"') AND :occasion = '"BUSINESS_CASUAL"'
            OR JSON_CONTAINS(LOWER(occasions), '"party"') AND :occasion = '"PARTY"'
            OR JSON_CONTAINS(LOWER(occasions), '"date"') AND :occasion = '"DATE_NIGHT"'
            OR JSON_CONTAINS(LOWER(occasions), '"formal"') AND :occasion = '"FORMAL_EVENT"'
            OR JSON_CONTAINS(LOWER(occasions), '"wedding"') AND :occasion = '"WEDDING_GUEST"'
            OR JSON_CONTAINS(LOWER(occasions), '"beach"') AND :occasion = '"BEACH_VACATION"'
        )
        AND (
            JSON_OVERLAPS(LOWER(outfit_types), LOWER(CAST(:styles AS JSON)))
            OR JSON_OVERLAPS(LOWER(style_keywords), LOWER(CAST(:styles AS JSON)))
        )
        ORDER BY 
        (
            CASE WHEN JSON_CONTAINS(LOWER(outfit_types), LOWER(CAST(:preferredStyle AS JSON))) THEN 3 ELSE 0 END +
            CASE WHEN formality_level = :formality THEN 2 ELSE 0 END + 
            CASE WHEN JSON_CONTAINS(LOWER(occasions), LOWER(CAST(:occasion AS JSON))) THEN 2 ELSE 1 END
        ) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<OutfitReference> findWithStylePreferences(
            @Param("gender") String gender,
            @Param("bodyType") String bodyType,
            @Param("occasion") String occasion,
            @Param("styles") String stylesJson,
            @Param("preferredStyle") String preferredStyle,
            @Param("formality") Integer formality,
            @Param("limit") int limit);
}
