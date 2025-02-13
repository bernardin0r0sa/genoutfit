package com.genoutfit.api.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class ProfileDto {
    @NotNull
    private Ethnicity ethnicity;

    @NotNull
    private BodyType bodyType;

    @NotNull
    private Gender gender;

    @Min(1)
    private int height;

    @Min(13)
    private int age;

    private Set<String> stylePreferences;
    private Set<String> colorPreferences;
}