package com.rehearse.api.domain.questionset.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.service.InternalQuestionSetService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = InternalQuestionSetController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = InternalApiKeyFilter.class))
class InternalQuestionSetControllerTest {

    private static final String BASE_URL =
            "/api/internal/interviews/{interviewId}/question-sets/{questionSetId}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InternalQuestionSetService internalQuestionSetService;

    // ----------------------------------------------------------------
    // PUT /progress
    // ----------------------------------------------------------------

    @Test
    @DisplayName("PUT /progress - 분석 진행 상태 업데이트 성공 시 200을 반환한다")
    void updateProgress_success() throws Exception {
        willDoNothing().given(internalQuestionSetService).updateProgress(eq(1L), any());

        String requestBody = """
                {
                    "progress": "EXTRACTING"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/progress", 5L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PUT /progress - progress 누락 시 400을 반환한다")
    void updateProgress_missingProgress_returns400() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(put(BASE_URL + "/progress", 5L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ----------------------------------------------------------------
    // GET /answers
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /answers - 답변 목록 조회 성공 시 200과 답변 리스트를 반환한다")
    void getAnswers_success() throws Exception {
        // InternalQuestionSetService.getAnswers()는 List<QuestionSetAnswer>를 반환하고
        // 컨트롤러에서 AnswerResponse::from 으로 매핑하므로, 여기서는 빈 목록을 반환하게 stub한다.
        given(internalQuestionSetService.getAnswers(1L)).willReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/answers", 5L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ----------------------------------------------------------------
    // POST /feedback
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /feedback - 피드백 저장 성공 시 200을 반환한다")
    void saveFeedback_success() throws Exception {
        willDoNothing().given(internalQuestionSetService).saveFeedback(eq(1L), any());

        String requestBody = """
                {
                    "questionSetScore": 85,
                    "questionSetComment": "전반적으로 좋은 답변입니다.",
                    "timestampFeedbacks": [
                        {
                            "answerType": "MAIN",
                            "startMs": 0,
                            "endMs": 5000,
                            "verbalScore": 80,
                            "verbalComment": "명확한 설명"
                        }
                    ]
                }
                """;

        mockMvc.perform(post(BASE_URL + "/feedback", 5L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /feedback - questionSetScore 누락 시 400을 반환한다")
    void saveFeedback_missingScore_returns400() throws Exception {
        String requestBody = """
                {
                    "questionSetComment": "코멘트만 있고 점수 없음"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/feedback", 5L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /feedback - questionSetComment 누락 시 400을 반환한다")
    void saveFeedback_missingComment_returns400() throws Exception {
        String requestBody = """
                {
                    "questionSetScore": 85
                }
                """;

        mockMvc.perform(post(BASE_URL + "/feedback", 5L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ----------------------------------------------------------------
    // POST /retry-analysis
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /retry-analysis - 분석 재시도 성공 시 200을 반환한다")
    void retryAnalysis_success() throws Exception {
        willDoNothing().given(internalQuestionSetService).retryAnalysis(1L);

        mockMvc.perform(post(BASE_URL + "/retry-analysis", 5L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ----------------------------------------------------------------
    // 404 케이스
    // ----------------------------------------------------------------

    @Test
    @DisplayName("PUT /progress - 존재하지 않는 질문세트 ID로 요청 시 404를 반환한다")
    void updateProgress_notFound_returns404() throws Exception {
        willThrow(new BusinessException(QuestionSetErrorCode.NOT_FOUND))
                .given(internalQuestionSetService).updateProgress(eq(999L), any());

        String requestBody = """
                {
                    "progress": "EXTRACTING"
                }
                """;

        mockMvc.perform(put(BASE_URL + "/progress", 5L, 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_SET_001"));
    }

    @Test
    @DisplayName("POST /retry-analysis - 존재하지 않는 질문세트 ID로 요청 시 404를 반환한다")
    void retryAnalysis_notFound_returns404() throws Exception {
        willThrow(new BusinessException(QuestionSetErrorCode.NOT_FOUND))
                .given(internalQuestionSetService).retryAnalysis(999L);

        mockMvc.perform(post(BASE_URL + "/retry-analysis", 5L, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_SET_001"));
    }
}
