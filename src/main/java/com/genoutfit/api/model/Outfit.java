package com.genoutfit.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outfits")
@Data
@NoArgsConstructor
public class Outfit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Occasion occasion;

    @ElementCollection
    @CollectionTable(name = "outfit_images")
    private List<String> imageUrls = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String clothingDetails;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private String vectorId;

    @Column(columnDefinition = "TEXT")
    private String promptsUsed;
}

