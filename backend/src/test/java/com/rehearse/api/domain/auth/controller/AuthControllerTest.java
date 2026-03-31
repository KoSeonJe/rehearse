package com.rehearse.api.domain.auth.controller;

import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.service.UserService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.security.config.SecurityConfig;
import com.rehearse.api.global.security.jwt.JwtTokenProvider;
import com.rehearse.api.global.security.oauth2.CustomOAuth2UserService;
import com.rehearse.api.global.security.oauth2.OAuth2FailureHandler;
import com.rehearse.api.global.security.oauth2.OAuth2SuccessHandler;
import org.junit.jupiter.api.DisplayName;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = InternalApiKeyFilter.class)
)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private OAuth2FailureHandler oAuth2FailureHandler;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    private User createMockUser() {
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .profileImage("https://example.com/avatar.png")
                .provider(OAuthProvider.GITHUB)
                .providerId("12345")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private UsernamePasswordAuthenticationToken createAuth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - 인증된 사용자 정보를 반환한다")
    void getMe_authenticated_returnsUserInfo() throws Exception {
        // given
        User user = createMockUser();
        given(userService.findById(1L)).willReturn(user);

        // when & then
        mockMvc.perform(get("/api/v1/auth/me")
                        .with(authentication(createAuth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("Test User"))
                .andExpect(jsonPath("$.data.provider").value("GITHUB"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - 인증되지 않으면 401을 반환한다")
    void getMe_unauthenticated_returns401() throws Exception {
        // SecurityConfig에서 /api/v1/auth/** permitAll → 컨트롤러 도달 → userId null → 401
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - 쿠키를 삭제한다")
    void logout_success_clearsCookie() throws Exception {
        // when & then
        var result = mockMvc.perform(post("/api/v1/auth/logout")
                        .secure(true)
                        .with(authentication(createAuth(1L)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        var cookie = result.getResponse().getCookie("rehearse_token");
        org.assertj.core.api.Assertions.assertThat(cookie).isNotNull();
        org.assertj.core.api.Assertions.assertThat(cookie.getMaxAge()).isZero();
        org.assertj.core.api.Assertions.assertThat(cookie.isHttpOnly()).isTrue();
        org.assertj.core.api.Assertions.assertThat(cookie.getSecure()).isTrue();
    }
}
