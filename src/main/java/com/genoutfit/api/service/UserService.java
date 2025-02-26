package com.genoutfit.api.service;

import com.genoutfit.api.model.OnboardingStatus;
import com.genoutfit.api.model.ProfileDto;
import com.genoutfit.api.model.User;
import com.genoutfit.api.model.UserPrincipal;
import com.genoutfit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getCurrentUser(UserPrincipal userPrincipal) throws Exception {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new Exception("User" + userPrincipal.getId()));
    }

    public User updateProfile(String userId, ProfileDto profileDto) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User"+userId));

        user.setEthnicity(profileDto.getEthnicity());
        user.setBodyType(profileDto.getBodyType());
        user.setGender(profileDto.getGender());
        user.setHeight(profileDto.getHeight());
        user.setAge(profileDto.getAge());
        user.setStylePreferences(profileDto.getStylePreferences());
        user.setColorPreferences(profileDto.getColorPreferences());
        user.setOnboardingStatus(OnboardingStatus.PROFILE_COMPLETED);

        return userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public void activatePremiumUser(String userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User"+userId));

        user.setPremiumUser(true);
        user.setOnboardingStatus(OnboardingStatus.COMPLETED);
        user.setPremiumExpiryDate(LocalDateTime.now().plusYears(1));

        userRepository.save(user);
    }

    public User getUserById(String userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User"+userId));
        return user;
    }

    public Object getSubscriptionDetails(String id) {
        return null;
    }
}