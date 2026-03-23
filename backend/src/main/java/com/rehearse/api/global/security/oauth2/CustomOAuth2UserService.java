package com.rehearse.api.global.security.oauth2;

import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuthProvider provider;
        String providerId;
        String email;
        String name;
        String profileImage;

        if ("github".equals(registrationId)) {
            provider = OAuthProvider.GITHUB;
            providerId = String.valueOf(attributes.get("id"));
            email = (String) attributes.get("email");
            name = (String) attributes.getOrDefault("name", attributes.get("login"));
            profileImage = (String) attributes.get("avatar_url");
        } else if ("google".equals(registrationId)) {
            provider = OAuthProvider.GOOGLE;
            providerId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            profileImage = (String) attributes.get("picture");
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        }

        if (email == null || email.isBlank()) {
            email = provider.name().toLowerCase() + "_" + providerId + "@rehearse.local";
        }

        if (name == null || name.isBlank()) {
            name = provider.name().toLowerCase() + "_" + providerId;
        }

        User user = userService.findOrCreate(provider, providerId, email, name, profileImage);
        log.info("OAuth2 로그인 성공: provider={}, email={}", provider, email);

        return new CustomOAuth2User(user, attributes);
    }
}
