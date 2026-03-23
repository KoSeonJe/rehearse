package com.rehearse.api.domain.auth.dto;

import com.rehearse.api.domain.user.entity.User;

public record UserResponse(
        Long id,
        String email,
        String name,
        String profileImage,
        String provider,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImage(),
                user.getProvider().name(),
                user.getRole().name()
        );
    }
}
