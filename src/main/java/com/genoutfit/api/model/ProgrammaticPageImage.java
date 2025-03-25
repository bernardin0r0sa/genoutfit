package com.genoutfit.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "programmatic_page_images")
@Data
public class ProgrammaticPageImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private ProgrammaticPage page;

    @Column(nullable = false)
    private String imageUrl;

    // Getters and Setters
}