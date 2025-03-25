package com.genoutfit.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "programmatic_page_related")
@Data
public class ProgrammaticPageRelated {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private ProgrammaticPage page;

    @Column(nullable = false)
    private String relatedSlug;

    // Getters and Setters
}