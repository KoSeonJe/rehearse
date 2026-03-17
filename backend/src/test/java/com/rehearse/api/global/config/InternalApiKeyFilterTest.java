package com.rehearse.api.global.config;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class InternalApiKeyFilterTest {

    @Mock
    private FilterChain filterChain;

    private InternalApiKeyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalApiKeyFilter();
    }

    @Test
    @DisplayName("내부 API 경로가 아닌 요청은 필터를 통과한다")
    void doFilterInternal_nonInternalPath_passesThrough() throws ServletException, IOException {
        // given
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/interviews/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        then(filterChain).should().doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("API 키가 설정되지 않은 경우 내부 API 요청도 인증 없이 통과한다")
    void doFilterInternal_apiKeyNotConfigured_passesThrough() throws ServletException, IOException {
        // given
        ReflectionTestUtils.setField(filter, "internalApiKey", "");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/analysis/complete");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        then(filterChain).should().doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("올바른 API 키로 내부 API 요청 시 필터를 통과한다")
    void doFilterInternal_correctApiKey_passesThrough() throws ServletException, IOException {
        // given
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/analysis/complete");
        request.addHeader("X-Internal-Api-Key", "test-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        then(filterChain).should().doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("잘못된 API 키로 내부 API 요청 시 401을 반환한다")
    void doFilterInternal_wrongApiKey_returns401() throws ServletException, IOException {
        // given
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/analysis/complete");
        request.addHeader("X-Internal-Api-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        then(filterChain).should(never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("API 키 헤더 누락 시 내부 API 요청은 401을 반환한다")
    void doFilterInternal_missingApiKeyHeader_returns401() throws ServletException, IOException {
        // given
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/analysis/complete");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        then(filterChain).should(never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("401 응답 본문에 JSON 형식의 에러 메시지가 포함된다")
    void doFilterInternal_unauthorized_responseBodyContainsJsonError() throws ServletException, IOException {
        // given
        ReflectionTestUtils.setField(filter, "internalApiKey", "test-key");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/internal/analysis/complete");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString())
                .isEqualTo("{\"success\":false,\"code\":\"AUTH_001\",\"message\":\"유효하지 않은 내부 API 키입니다.\"}");
    }
}
