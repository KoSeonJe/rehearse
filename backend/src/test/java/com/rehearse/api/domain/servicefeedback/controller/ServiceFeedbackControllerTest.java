package com.rehearse.api.domain.servicefeedback.controller;

import com.rehearse.api.domain.servicefeedback.dto.AdminFeedbackResponse;
import com.rehearse.api.domain.servicefeedback.dto.FeedbackNeedCheckResponse;
import com.rehearse.api.domain.servicefeedback.entity.FeedbackSource;
import com.rehearse.api.domain.servicefeedback.service.ServiceFeedbackService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.security.config.SecurityConfig;
import com.rehearse.api.global.support.WithMockUserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {ServiceFeedbackController.class, AdminFeedbackController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "app.admin.password=test-admin-password")
class ServiceFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServiceFeedbackService serviceFeedbackService;

    // ====== POST /api/v1/service-feedbacks ======

    @Test
    @DisplayName("POST /api/v1/service-feedbacks - 인증 없이 호출하면 401")
    void submitFeedback_withoutAuth_returns401() throws Exception {
        String requestBody = """
                {
                    "content": "서비스가 정말 도움이 되었습니다.",
                    "rating": 5,
                    "source": "AUTO_POPUP"
                }
                """;

        mockMvc.perform(post("/api/v1/service-feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUserId
    @DisplayName("POST /api/v1/service-feedbacks - content가 10자 미만이면 400")
    void submitFeedback_contentTooShort_returns400() throws Exception {
        String requestBody = """
                {
                    "content": "짧은글",
                    "rating": 5,
                    "source": "AUTO_POPUP"
                }
                """;

        mockMvc.perform(post("/api/v1/service-feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUserId
    @DisplayName("POST /api/v1/service-feedbacks - source 누락 시 400")
    void submitFeedback_missingSource_returns400() throws Exception {
        String requestBody = """
                {
                    "content": "서비스가 정말 도움이 많이 되었습니다.",
                    "rating": 5
                }
                """;

        mockMvc.perform(post("/api/v1/service-feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUserId
    @DisplayName("POST /api/v1/service-feedbacks - 정상 제출 시 201")
    void submitFeedback_validRequest_returns201() throws Exception {
        willDoNothing().given(serviceFeedbackService).submitFeedback(eq(1L), any());

        String requestBody = """
                {
                    "content": "서비스가 정말 도움이 많이 되었습니다.",
                    "rating": 5,
                    "source": "AUTO_POPUP"
                }
                """;

        mockMvc.perform(post("/api/v1/service-feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUserId
    @DisplayName("POST /api/v1/service-feedbacks - rating 없이도 정상 제출된다 (optional)")
    void submitFeedback_withoutRating_returns201() throws Exception {
        willDoNothing().given(serviceFeedbackService).submitFeedback(eq(1L), any());

        String requestBody = """
                {
                    "content": "서비스가 정말 도움이 많이 되었습니다.",
                    "source": "VOLUNTARY"
                }
                """;

        mockMvc.perform(post("/api/v1/service-feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ====== GET /api/v1/service-feedbacks/need-check ======

    @Test
    @DisplayName("GET /api/v1/service-feedbacks/need-check - 인증 없이 호출하면 401")
    void checkNeedsFeedback_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/service-feedbacks/need-check"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUserId
    @DisplayName("GET /api/v1/service-feedbacks/need-check - 피드백 필요 시 needsFeedback=true 반환")
    void checkNeedsFeedback_whenNeeded_returnsTrue() throws Exception {
        given(serviceFeedbackService.checkNeedsFeedback(1L))
                .willReturn(new FeedbackNeedCheckResponse(true));

        mockMvc.perform(get("/api/v1/service-feedbacks/need-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.needsFeedback").value(true));
    }

    @Test
    @WithMockUserId
    @DisplayName("GET /api/v1/service-feedbacks/need-check - 피드백 불필요 시 needsFeedback=false 반환")
    void checkNeedsFeedback_whenNotNeeded_returnsFalse() throws Exception {
        given(serviceFeedbackService.checkNeedsFeedback(1L))
                .willReturn(new FeedbackNeedCheckResponse(false));

        mockMvc.perform(get("/api/v1/service-feedbacks/need-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.needsFeedback").value(false));
    }

    // ====== GET /api/v1/admin/feedbacks ======

    @Test
    @DisplayName("GET /api/v1/admin/feedbacks - 비밀번호 없이 호출하면 401")
    void getAdminFeedbacks_withoutPassword_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/feedbacks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/feedbacks - 잘못된 비밀번호로 호출하면 401")
    void getAdminFeedbacks_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("X-Admin-Password", "wrong-password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/feedbacks - 올바른 비밀번호로 호출하면 200과 페이징 응답 반환")
    void getAdminFeedbacks_withValidPassword_returns200() throws Exception {
        AdminFeedbackResponse item = new AdminFeedbackResponse(
                1L, 10L, "홍길동", "hong@test.com",
                "좋은 서비스입니다. 많이 이용하겠습니다.", 5,
                FeedbackSource.AUTO_POPUP, 3,
                LocalDateTime.of(2026, 4, 9, 12, 0, 0));

        given(serviceFeedbackService.getAdminFeedbacks(any()))
                .willReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("X-Admin-Password", "test-admin-password")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].userName").value("홍길동"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
