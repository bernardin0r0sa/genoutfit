package com.genoutfit.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Update User entity to use Ethnicity instead of SkinTone
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String email;

    private String name;

    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider = AuthProvider.LOCAL;
    private String providerId;

    @Enumerated(EnumType.STRING)
    private OnboardingStatus onboardingStatus = OnboardingStatus.NEW;

    @Enumerated(EnumType.STRING)
    private Ethnicity ethnicity;

    @Enumerated(EnumType.STRING)
    private BodyType bodyType;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private int height;
    private int age;

    @ElementCollection
    private Set<String> stylePreferences = new HashSet<>();

    @ElementCollection
    private Set<String> colorPreferences = new HashSet<>();

    private boolean premiumUser = false;

    private LocalDateTime premiumExpiryDate;
}