package com.rehearse.api.domain.servicefeedback.controller;

import com.rehearse.api.domain.servicefeedback.dto.FeedbackNeedCheckResponse;
import com.rehearse.api.domain.servicefeedback.service.ServiceFeedbackService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.security.config.SecurityConfig;
import com.rehearse.api.global.support.WithMockUserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ServiceFeedbackController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "app.admin.password=test-admin-password")
@DisplayName("ServiceFeedbackController - 서비스 피드백 API")
class ServiceFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServiceFeedbackService serviceFeedbackService;

    @Nested
    @DisplayName("POST /api/v1/service-feedbacks")
    class SubmitFeedback {

        @Test
        @DisplayName("인증 없이 호출하면 401")
        void submitFeedback_withoutAuth_returns401() throws Exception {
            // given
            String requestBody = """
                    {
                        "content": "서비스가 정말 도움이 되었습니다.",
                        "rating": 5,
                        "source": "AUTO_POPUP"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/service-feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUserId
        @DisplayName("content가 10자 미만이면 400")
        void submitFeedback_contentTooShort_returns400() throws Exception {
            // given
            String requestBody = """
                    {
                        "content": "짧은글",
                        "rating": 5,
                        "source": "AUTO_POPUP"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/service-feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @WithMockUserId
        @DisplayName("source 누락 시 400")
        void submitFeedback_missingSource_returns400() throws Exception {
            // given
            String requestBody = """
                    {
                        "content": "서비스가 정말 도움이 많이 되었습니다.",
                        "rating": 5
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/service-feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @WithMockUserId
        @DisplayName("정상 제출 시 201")
        void submitFeedback_validRequest_returns201() throws Exception {
            // given
            willDoNothing().given(serviceFeedbackService).submitFeedback(eq(1L), any());

            String requestBody = """
                    {
                        "content": "서비스가 정말 도움이 많이 되었습니다.",
                        "rating": 5,
                        "source": "AUTO_POPUP"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/service-feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUserId
        @DisplayName("rating 없이도 정상 제출된다 (optional)")
        void submitFeedback_withoutRating_returns201() throws Exception {
            // given
            willDoNothing().given(serviceFeedbackService).submitFeedback(eq(1L), any());

            String requestBody = """
                    {
                        "content": "서비스가 정말 도움이 많이 되었습니다.",
                        "source": "VOLUNTARY"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/service-feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/service-feedbacks/need-check")
    class CheckNeedsFeedback {

        @Test
        @DisplayName("인증 없이 호출하면 401")
        void checkNeedsFeedback_withoutAuth_returns401() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/service-feedbacks/need-check"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUserId
        @DisplayName("피드백 필요 시 needsFeedback=true 반환")
        void checkNeedsFeedback_whenNeeded_returnsTrue() throws Exception {
            // given
            given(serviceFeedbackService.checkNeedsFeedback(1L))
                    .willReturn(new FeedbackNeedCheckResponse(true));

            // when & then
            mockMvc.perform(get("/api/v1/service-feedbacks/need-check"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.needsFeedback").value(true));
        }

        @Test
        @WithMockUserId
        @DisplayName("피드백 불필요 시 needsFeedback=false 반환")
        void checkNeedsFeedback_whenNotNeeded_returnsFalse() throws Exception {
            // given
            given(serviceFeedbackService.checkNeedsFeedback(1L))
                    .willReturn(new FeedbackNeedCheckResponse(false));

            // when & then
            mockMvc.perform(get("/api/v1/service-feedbacks/need-check"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.needsFeedback").value(false));
        }
    }

}
