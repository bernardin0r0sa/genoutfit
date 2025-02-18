package com.genoutfit.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "prompt_templates")
@Data
public class PromptTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Occasion occasion;

    @Column(columnDefinition = "TEXT")
    private String basePrompt;

    @Enumerated(EnumType.STRING)
    private PromptStyle style;

    private String photographerReference;

    @Column(columnDefinition = "TEXT")
    private String styleNotes;

    private boolean isInstagramStyle = false;
}