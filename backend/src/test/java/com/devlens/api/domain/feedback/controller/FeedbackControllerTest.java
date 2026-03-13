package com.devlens.api.domain.feedback.controller;

import com.devlens.api.domain.feedback.dto.FeedbackListResponse;
import com.devlens.api.domain.feedback.dto.FeedbackResponse;
import com.devlens.api.domain.feedback.entity.FeedbackCategory;
import com.devlens.api.domain.feedback.entity.FeedbackSeverity;
import com.devlens.api.domain.feedback.service.FeedbackService;
import com.devlens.api.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/feedbacks - 피드백 생성 성공 (201)")
    void generateFeedback_success() throws Exception {
        // given
        FeedbackListResponse response = createMockFeedbackListResponse();
        given(feedbackService.generateFeedback(eq(1L), any())).willReturn(response);

        String requestBody = """
                {
                    "answers": [
                        {
                            "questionIndex": 0,
                            "questionContent": "HashMap과 TreeMap의 차이점은?",
                            "answerText": "HashMap은 해시 기반이고 TreeMap은 트리 기반입니다.",
                            "nonVerbalSummary": "시선 안정적",
                            "voiceSummary": "목소리 안정적"
                        }
                    ]
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/interviews/1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.interviewId").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.feedbacks").isArray())
                .andExpect(jsonPath("$.data.feedbacks[0].content").value("답변이 다소 추상적입니다."));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id}/feedbacks - 피드백 조회 성공 (200)")
    void getFeedbacks_success() throws Exception {
        // given
        FeedbackListResponse response = createMockFeedbackListResponse();
        given(feedbackService.getFeedbacks(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/interviews/1/feedbacks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.interviewId").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.feedbacks[0].category").value("CONTENT"));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id}/feedbacks - 면접 세션 없음 (404)")
    void getFeedbacks_notFound() throws Exception {
        // given
        given(feedbackService.getFeedbacks(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/interviews/999/feedbacks"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERVIEW_001"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/feedbacks - answers 빈 배열 시 400")
    void generateFeedback_emptyAnswers() throws Exception {
        String requestBody = """
                {
                    "answers": []
                }
                """;

        mockMvc.perform(post("/api/v1/interviews/1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/feedbacks - answers 누락 시 400")
    void generateFeedback_missingAnswers() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(post("/api/v1/interviews/1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private FeedbackListResponse createMockFeedbackListResponse() {
        FeedbackResponse feedbackResponse = FeedbackResponse.builder()
                .id(1L)
                .timestampSeconds(15.5)
                .category(FeedbackCategory.CONTENT)
                .severity(FeedbackSeverity.SUGGESTION)
                .content("답변이 다소 추상적입니다.")
                .suggestion("구체적인 예시를 들어 설명하세요.")
                .build();

        return FeedbackListResponse.builder()
                .interviewId(1L)
                .feedbacks(List.of(feedbackResponse))
                .totalCount(1)
                .build();
    }
}
