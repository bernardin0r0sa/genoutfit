package com.genoutfit.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "outfit_references")
@Data
@NoArgsConstructor
public class OutfitReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", unique = true)
    private String postId;

    @Column(name = "influencer_id")
    private Long influencerId;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "gender_style", length = 50)
    private String genderStyle;

    @Column(name = "formality_level")
    private Integer formalityLevel;

    @Column(name = "season", length = 50)
    private String season;

    @Column(name = "outfit_description", columnDefinition = "TEXT")
    private String outfitDescription;

    @Column(name = "body_types", columnDefinition = "JSON")
    private String bodyTypes;

    @Column(name = "occasions", columnDefinition = "JSON")
    private String occasions;

    @Column(name = "outfit_types", columnDefinition = "JSON")
    private String outfitTypes;

    @Column(name = "colors", columnDefinition = "JSON")
    private String colors;

    @Column(name = "patterns", columnDefinition = "JSON")
    private String patterns;

    @Column(name = "style_keywords", columnDefinition = "JSON")
    private String styleKeywords;

    @Column(name = "clothing_pieces", columnDefinition = "JSON")
    private String clothingPieces;

    @Column(name = "raw_analysis", columnDefinition = "TEXT")
    private String rawAnalysis;
}