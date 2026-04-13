package com.rehearse.api.global.security.oauth2;

import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private OAuth2SuccessHandler handler;

    private static final String FRONTEND_URL = "https://rehearse.dev";

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(jwtTokenProvider);
        ReflectionTestUtils.setField(handler, "frontendUrl", FRONTEND_URL);
    }

    private Authentication createMockAuthentication() {
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .provider(OAuthProvider.GITHUB)
                .providerId("12345")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        CustomOAuth2User oAuth2User = new CustomOAuth2User(user, Map.of());
        return new UsernamePasswordAuthenticationToken(oAuth2User, null, oAuth2User.getAuthorities());
    }

    @Test
    @DisplayName("OAuth2 인증 성공 시 JWT 쿠키에 Secure와 SameSite 속성이 설정된다")
    void onAuthenticationSuccess_cookieHasSecureAndSameSite() throws Exception {
        // given
        given(jwtTokenProvider.createToken(1L, "USER")).willReturn("test-jwt-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = createMockAuthentication();

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        var setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("rehearse_token=test-jwt-token");
        assertThat(setCookieHeader).contains("Max-Age=604800");
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Secure");
        assertThat(setCookieHeader).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("OAuth2 인증 성공 시 기본 리다이렉트 URL은 /이다")
    void onAuthenticationSuccess_defaultRedirect() throws Exception {
        // given
        given(jwtTokenProvider.createToken(1L, "USER")).willReturn("test-jwt-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = createMockAuthentication();

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND_URL + "/");
    }

    @Test
    @DisplayName("redirect 파라미터가 /로 시작하면 해당 경로로 리다이렉트한다")
    void onAuthenticationSuccess_customRedirect() throws Exception {
        // given
        given(jwtTokenProvider.createToken(1L, "USER")).willReturn("test-jwt-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        request.setParameter("redirect", "/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = createMockAuthentication();

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND_URL + "/dashboard");
    }

    @Test
    @DisplayName("redirect 파라미터가 //로 시작하면 기본 경로로 리다이렉트한다")
    void onAuthenticationSuccess_protocolRelativeRedirect_blocked() throws Exception {
        // given
        given(jwtTokenProvider.createToken(1L, "USER")).willReturn("test-jwt-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        request.setParameter("redirect", "//evil.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = createMockAuthentication();

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND_URL + "/");
    }

    @Test
    @DisplayName("redirect 파라미터가 외부 URL이면 기본 경로로 리다이렉트한다")
    void onAuthenticationSuccess_externalRedirect_blocked() throws Exception {
        // given
        given(jwtTokenProvider.createToken(1L, "USER")).willReturn("test-jwt-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        request.setParameter("redirect", "https://evil.com");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Authentication authentication = createMockAuthentication();

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND_URL + "/");
    }
}
