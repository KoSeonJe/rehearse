package com.rehearse.api.global.security.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("test-secret-key-for-jwt-token-provider-test-must-be-256-bits!".getBytes());
    private static final long EXPIRATION_MS = 3600000L;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("토큰 생성 시 userId와 role이 claim에 포함된다")
    void createToken_success_containsUserIdAndRole() {
        // given
        Long userId = 1L;
        String role = "USER";

        // when
        String token = jwtTokenProvider.createToken(userId, role);

        // then
        Claims claims = jwtTokenProvider.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    @DisplayName("토큰 생성 시 email과 name은 포함되지 않는다")
    void createToken_success_doesNotContainEmailOrName() {
        // given
        String token = jwtTokenProvider.createToken(1L, "USER");

        // when
        Claims claims = jwtTokenProvider.parseToken(token);

        // then
        assertThat(claims.get("email")).isNull();
        assertThat(claims.get("name")).isNull();
    }

    @Test
    @DisplayName("유효한 토큰 검증 시 true를 반환한다")
    void validateToken_validToken_returnsTrue() {
        // given
        String token = jwtTokenProvider.createToken(1L, "USER");

        // when
        boolean result = jwtTokenProvider.validateToken(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰 검증 시 false를 반환한다")
    void validateToken_invalidToken_returnsFalse() {
        // given
        String invalidToken = "invalid.jwt.token";

        // when
        boolean result = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰 검증 시 false를 반환한다")
    void validateToken_differentKey_returnsFalse() {
        // given
        String anotherSecret = Base64.getEncoder()
                .encodeToString("another-secret-key-for-testing-different-signing-key!!!!!".getBytes());
        JwtTokenProvider anotherProvider = new JwtTokenProvider(anotherSecret, EXPIRATION_MS);
        String token = anotherProvider.createToken(1L, "USER");

        // when
        boolean result = jwtTokenProvider.validateToken(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 false를 반환한다")
    void validateToken_expiredToken_returnsFalse() {
        // given
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1000L);
        String token = expiredProvider.createToken(1L, "USER");

        // when
        boolean result = jwtTokenProvider.validateToken(token);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getUserId로 토큰에서 userId를 추출한다")
    void getUserId_validToken_returnsUserId() {
        // given
        String token = jwtTokenProvider.createToken(42L, "USER");

        // when
        Long userId = jwtTokenProvider.getUserId(token);

        // then
        assertThat(userId).isEqualTo(42L);
    }
}
