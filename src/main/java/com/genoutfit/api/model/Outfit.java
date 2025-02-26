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

    private String style;

    @Column(name = "is_favorite")
    private boolean favorite = false;

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

    // Updated toString to avoid circular reference issues
    @Override
    public String toString() {
        return "Outfit{" +
                "id='" + id + '\'' +
                ", userId='" + (user != null ? user.getId() : "null") + '\'' +
                ", occasion=" + occasion +
                ", style='" + style + '\'' +
                ", favorite=" + favorite +
                ", imageUrls=" + imageUrls +
                ", createdAt=" + createdAt +
                '}';
    }
}