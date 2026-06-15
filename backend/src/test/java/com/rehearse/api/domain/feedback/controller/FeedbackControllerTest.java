package com.rehearse.api.domain.feedback.controller;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.service.FeedbackService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.security.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = FeedbackController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedbackService feedbackService;

    @Test
    @DisplayName("POST /feedback - 유효한 요청이면 200을 반환하고 service.saveFeedback이 호출된다")
    void saveFeedback_returns_200_when_request_valid() throws Exception {
        willDoNothing().given(feedbackService).saveFeedback(eq(10L), any(SaveFeedbackRequest.class));

        String body = """
                {
                    "questionSetComment": "좋은 답변입니다",
                    "timestampFeedbacks": [],
                    "isVerbalCompleted": true,
                    "isNonverbalCompleted": true
                }
                """;

        mockMvc.perform(post("/api/internal/interviews/1/question-sets/10/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(feedbackService).should().saveFeedback(eq(10L), any(SaveFeedbackRequest.class));
    }

    @Test
    @DisplayName("POST /feedback - questionSetComment 누락 시 400을 반환한다")
    void saveFeedback_returns_400_when_questionSetComment_missing() throws Exception {
        String body = """
                {
                    "timestampFeedbacks": [],
                    "isVerbalCompleted": true,
                    "isNonverbalCompleted": true
                }
                """;

        mockMvc.perform(post("/api/internal/interviews/1/question-sets/10/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /feedback - TimestampFeedbackItem.startMs 누락 시 400을 반환한다")
    void saveFeedback_returns_400_when_timestamp_item_missing_required_field() throws Exception {
        String body = """
                {
                    "questionSetComment": "ok",
                    "timestampFeedbacks": [
                        {"endMs": 5000}
                    ],
                    "isVerbalCompleted": true,
                    "isNonverbalCompleted": true
                }
                """;

        mockMvc.perform(post("/api/internal/interviews/1/question-sets/10/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
