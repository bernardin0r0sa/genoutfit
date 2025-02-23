package com.genoutfit.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outfit_history")
@Data
@NoArgsConstructor
public class OutfitHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "outfit_id", nullable = false)
    private String outfitId;

    @Column(name = "occasion")
    @Enumerated(EnumType.STRING)
    private Occasion occasion;

    @Column(name = "shown_at")
    private LocalDateTime shownAt;

    // Constructor for convenience
    public OutfitHistory(String userId, String outfitId, Occasion occasion) {
        this.userId = userId;
        this.outfitId = outfitId;
        this.occasion = occasion;
        this.shownAt = LocalDateTime.now();
    }
}