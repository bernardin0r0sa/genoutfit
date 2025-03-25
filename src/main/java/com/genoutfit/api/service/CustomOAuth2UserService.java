package com.genoutfit.api.service;

import com.genoutfit.api.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import com.genoutfit.api.repository.UserRepository;
import com.genoutfit.api.JwtTokenProvider;


@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {


        log.info("Starting OAuth2 User Load");
        log.info("Client Registration: {}", oAuth2UserRequest.getClientRegistration().getRegistrationId());

        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        log.info("OAuth2 User Attributes: {}", oAuth2User.getAttributes());

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("OAuth2 User Load Error", ex);
            log.error("Exception Details: {}", ex.getClass().getName());
            log.error("Exception Message: {}", ex.getMessage());

            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());

        String email = userInfo.getEmail();
        User user = userRepository.findByEmail(email)
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> registerNewUser(oAuth2UserRequest, userInfo));

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, GoogleOAuth2UserInfo userInfo) {
        User user = new User();
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(userInfo.getId());
        user.setName(userInfo.getName());
        user.setEmail(userInfo.getEmail());
        user.setOnboardingStatus(OnboardingStatus.NEW);
        return userRepository.save(user);
    }

    private User updateExistingUser(User user, GoogleOAuth2UserInfo userInfo) {
        user.setName(userInfo.getName());
        return userRepository.save(user);
    }
}
