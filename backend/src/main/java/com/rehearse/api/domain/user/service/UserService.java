package com.rehearse.api.domain.user.service;

import com.rehearse.api.domain.auth.exception.AuthErrorCode;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreate(OAuthProvider provider, String providerId,
                             String email, String name, String profileImage) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .map(user -> {
                    user.updateProfile(name, profileImage);
                    return user;
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .provider(provider)
                                .providerId(providerId)
                                .email(email)
                                .name(name)
                                .profileImage(profileImage)
                                .role(UserRole.USER)
                                .build()
                ));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }
}
