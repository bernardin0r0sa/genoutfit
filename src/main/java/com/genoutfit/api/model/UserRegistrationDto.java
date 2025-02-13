package com.genoutfit.api.model;

import lombok.Data;

import java.util.Set;

@Data
public class UserRegistrationDto {
    private String email;
    private String password;
    private Ethnicity ethnicity;
    private BodyType bodyType;
    private Gender gender;
    private int height;
    private int age;
    private Set<String> stylePreferences;
    private Set<String> colorPreferences;
}
