package com.rehearse.api.domain.user.service;

import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    private User createMockUser() {
        return User.builder()
                .email("test@example.com")
                .name("Test User")
                .profileImage("https://example.com/avatar.png")
                .provider(OAuthProvider.GITHUB)
                .providerId("12345")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("기존 사용자가 있으면 프로필을 업데이트하고 반환한다")
    void findOrCreate_existingUser_updatesProfile() {
        // given
        User existingUser = createMockUser();
        given(userRepository.findByProviderAndProviderId(OAuthProvider.GITHUB, "12345"))
                .willReturn(Optional.of(existingUser));

        // when
        User result = userService.findOrCreate(
                OAuthProvider.GITHUB, "12345", "test@example.com", "Updated Name", "https://new-avatar.png");

        // then
        assertThat(result).isSameAs(existingUser);
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getProfileImage()).isEqualTo("https://new-avatar.png");
        then(userRepository).should().findByProviderAndProviderId(OAuthProvider.GITHUB, "12345");
        then(userRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("신규 사용자이면 USER 권한으로 생성하고 반환한다")
    void findOrCreate_newUser_createsWithUserRole() {
        // given
        User savedUser = createMockUser();
        given(userRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, "67890"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        User result = userService.findOrCreate(
                OAuthProvider.GOOGLE, "67890", "new@example.com", "New User", null);

        // then
        assertThat(result).isNotNull();
        then(userRepository).should().save(any(User.class));
    }

    @Test
    @DisplayName("존재하는 사용자 ID로 조회하면 사용자를 반환한다")
    void findById_existingId_returnsUser() {
        // given
        User user = createMockUser();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        User result = userService.findById(1L);

        // then
        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회하면 BusinessException을 던진다")
    void findById_nonExistingId_throwsBusinessException() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }
}
