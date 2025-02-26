package com.genoutfit.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_outfits", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "outfit_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteOutfit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "outfit_id", nullable = false)
    private String outfitId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FavoriteOutfit(String userId, String outfitId) {
        this.userId = userId;
        this.outfitId = outfitId;
    }
}
