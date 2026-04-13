package com.rehearse.api.domain.servicefeedback.controller;

import com.rehearse.api.domain.servicefeedback.dto.AdminFeedbackResponse;
import com.rehearse.api.domain.servicefeedback.entity.FeedbackSource;
import com.rehearse.api.domain.servicefeedback.service.ServiceFeedbackService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.security.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminFeedbackController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "app.admin.password=test-pass")
class AdminFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceFeedbackService serviceFeedbackService;

    @Nested
    @DisplayName("GET /api/v1/admin/feedbacks - 인증")
    class Authentication {

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - X-Admin-Password 헤더 없이 호출하면 401 반환")
        void getAdminFeedbacks_withoutPasswordHeader_returns401() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("ADMIN_001"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - 잘못된 비밀번호로 호출하면 401 반환")
        void getAdminFeedbacks_withWrongPassword_returns401() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks")
                            .header("X-Admin-Password", "wrong-password"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("ADMIN_001"));
        }

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - 올바른 비밀번호로 호출하면 200 반환")
        void getAdminFeedbacks_withCorrectPassword_returns200() throws Exception {
            // given
            given(serviceFeedbackService.getAdminFeedbacks(any()))
                    .willReturn(Page.empty());

            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks")
                            .header("X-Admin-Password", "test-pass"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/feedbacks - 페이지네이션")
    class Pagination {

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - 기본 페이지네이션 파라미터 (page=0, size=20) 사용")
        void getAdminFeedbacks_defaultPagination_usesPage0Size20() throws Exception {
            // given
            AdminFeedbackResponse item = createFeedbackResponse(1L);
            PageImpl<AdminFeedbackResponse> page = new PageImpl<>(
                    List.of(item), PageRequest.of(0, 20), 1);
            given(serviceFeedbackService.getAdminFeedbacks(any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks")
                            .header("X-Admin-Password", "test-pass"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.size").value(20));
        }

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - 커스텀 페이지네이션 파라미터 (page=2, size=10) 사용")
        void getAdminFeedbacks_customPagination_usesGivenParams() throws Exception {
            // given
            PageImpl<AdminFeedbackResponse> page = new PageImpl<>(
                    Collections.emptyList(), PageRequest.of(2, 10), 0);
            given(serviceFeedbackService.getAdminFeedbacks(any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks")
                            .header("X-Admin-Password", "test-pass")
                            .param("page", "2")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(serviceFeedbackService).should().getAdminFeedbacks(PageRequest.of(2, 10));
        }

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - size=0이면 size=1로 보정된다")
        void getAdminFeedbacks_sizeZero_clampedTo1() throws Exception {
            // given
            given(serviceFeedbackService.getAdminFeedbacks(any())).willReturn(Page.empty());

            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks")
                            .header("X-Admin-Password", "test-pass")
                            .param("size", "0"))
                    .andExpect(status().isOk());

            then(serviceFeedbackService).should().getAdminFeedbacks(PageRequest.of(0, 1));
        }

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - size 음수이면 size=1로 보정된다")
        void getAdminFeedbacks_negativeSize_clampedTo1() throws Exception {
            // given
            given(serviceFeedbackService.getAdminFeedbacks(any())).willReturn(Page.empty());

            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks")
                            .header("X-Admin-Password", "test-pass")
                            .param("size", "-5"))
                    .andExpect(status().isOk());

            then(serviceFeedbackService).should().getAdminFeedbacks(PageRequest.of(0, 1));
        }

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - size=100 초과이면 size=100으로 보정된다")
        void getAdminFeedbacks_sizeOver100_clampedTo100() throws Exception {
            // given
            given(serviceFeedbackService.getAdminFeedbacks(any())).willReturn(Page.empty());

            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks")
                            .header("X-Admin-Password", "test-pass")
                            .param("size", "200"))
                    .andExpect(status().isOk());

            then(serviceFeedbackService).should().getAdminFeedbacks(PageRequest.of(0, 100));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/feedbacks - 결과")
    class Result {

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - 피드백이 없으면 빈 Page 반환")
        void getAdminFeedbacks_noFeedbacks_returnsEmptyPage() throws Exception {
            // given
            given(serviceFeedbackService.getAdminFeedbacks(any()))
                    .willReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks")
                            .header("X-Admin-Password", "test-pass"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("GET /api/v1/admin/feedbacks - 피드백 목록 조회 시 응답 필드 확인")
        void getAdminFeedbacks_withFeedbacks_returnsCorrectFields() throws Exception {
            // given
            AdminFeedbackResponse item = createFeedbackResponse(1L);
            given(serviceFeedbackService.getAdminFeedbacks(any()))
                    .willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

            // when & then
            mockMvc.perform(get("/api/v1/admin/feedbacks")
                            .header("X-Admin-Password", "test-pass"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.content[0].userId").value(10))
                    .andExpect(jsonPath("$.data.content[0].userName").value("홍길동"))
                    .andExpect(jsonPath("$.data.content[0].userEmail").value("hong@test.com"))
                    .andExpect(jsonPath("$.data.content[0].content").value("서비스가 정말 좋습니다."))
                    .andExpect(jsonPath("$.data.content[0].rating").value(5))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }
    }

    private AdminFeedbackResponse createFeedbackResponse(Long id) {
        return new AdminFeedbackResponse(
                id,
                10L,
                "홍길동",
                "hong@test.com",
                "서비스가 정말 좋습니다.",
                5,
                FeedbackSource.AUTO_POPUP,
                3,
                LocalDateTime.of(2026, 4, 9, 12, 0, 0));
    }
}
