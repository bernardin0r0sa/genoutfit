package com.genoutfit.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "programmatic_pages")
@Data
public class ProgrammaticPage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String pageType; // e.g., "plus-size-outfits", "outfits", "for-women"

    @Column(nullable = false)
    private int searchVolume;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgrammaticPageImage> images;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProgrammaticPageRelated> relatedPages;

    // Getters and Setters
}