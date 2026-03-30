package com.rehearse.api.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.Cookie;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("test-secret-key-for-jwt-auth-filter-test-must-be-256-bits!".getBytes());

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
        SecurityContextHolder.clearContext();
    }

    private Claims createClaims(String userId, String role) {
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String token = Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    @Test
    @DisplayName("쿠키에서 JWT 토큰을 추출하여 인증을 설정한다")
    void doFilterInternal_cookieToken_setsAuthentication() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";
        Claims claims = createClaims("1", "USER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("rehearse_token", token));
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.parseToken(token)).willReturn(claims);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(1L);
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_USER");
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더에서 Bearer 토큰을 추출하여 인증을 설정한다")
    void doFilterInternal_bearerToken_setsAuthentication() throws ServletException, IOException {
        // given
        String token = "valid-jwt-token";
        Claims claims = createClaims("1", "USER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.parseToken(token)).willReturn(claims);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(1L);
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("쿠키와 Bearer 헤더 둘 다 있을 때 쿠키가 우선한다")
    void doFilterInternal_bothCookieAndBearer_cookieTakesPriority() throws ServletException, IOException {
        // given
        String cookieToken = "cookie-token";
        String bearerToken = "bearer-token";
        Claims claims = createClaims("42", "USER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("rehearse_token", cookieToken));
        request.addHeader("Authorization", "Bearer " + bearerToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.validateToken(cookieToken)).willReturn(true);
        given(jwtTokenProvider.parseToken(cookieToken)).willReturn(claims);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(42L);
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 없으면 인증을 설정하지 않고 필터 체인을 진행한다")
    void doFilterInternal_noToken_noAuthentication() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증을 설정하지 않고 필터 체인을 진행한다")
    void doFilterInternal_invalidToken_noAuthentication() throws ServletException, IOException {
        // given
        String token = "invalid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("rehearse_token", token));
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenProvider.validateToken(token)).willReturn(false);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
    }
}
