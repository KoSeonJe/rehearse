package com.rehearse.api.domain.questionset.controller;

import com.rehearse.api.domain.question.dto.AnswersResponse;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.service.InternalQuestionSetService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.global.security.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = InternalQuestionSetController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
class InternalQuestionSetControllerTest {

    private static final String BASE_URL =
            "/api/internal/interviews/{interviewId}/question-sets/{questionSetId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InternalQuestionSetService internalQuestionSetService;

    @Nested
    @DisplayName("PUT /progress 엔드포인트")
    class UpdateProgress {

        @Test
        @DisplayName("분석 진행 상태 업데이트 성공 시 200을 반환한다")
        void updateProgress_success() throws Exception {
            // given
            willDoNothing().given(internalQuestionSetService).updateProgress(eq(1L), any());

            String requestBody = """
                    {
                        "status": "EXTRACTING"
                    }
                    """;

            // when & then
            mockMvc.perform(put(BASE_URL + "/progress", 5L, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("status 누락 시 400을 반환한다")
        void updateProgress_missingProgress_returns400() throws Exception {
            // given
            String requestBody = "{}";

            // when & then
            mockMvc.perform(put(BASE_URL + "/progress", 5L, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("존재하지 않는 질문세트 ID로 요청 시 404를 반환한다")
        void updateProgress_notFound_returns404() throws Exception {
            // given
            willThrow(new BusinessException(QuestionSetErrorCode.NOT_FOUND))
                    .given(internalQuestionSetService).updateProgress(eq(999L), any());

            String requestBody = """
                    {
                        "status": "EXTRACTING"
                    }
                    """;

            // when & then
            mockMvc.perform(put(BASE_URL + "/progress", 5L, 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("QUESTION_SET_001"));
        }
    }

    @Nested
    @DisplayName("GET /answers 엔드포인트")
    class GetAnswers {

        @Test
        @DisplayName("답변 목록 조회 성공 시 200과 analysisStatus + 답변 리스트를 반환한다")
        void getAnswers_success() throws Exception {
            // given
            AnswersResponse answersResponse = AnswersResponse.builder()
                    .analysisStatus("PENDING")
                    .position("BACKEND")
                    .level("JUNIOR")
                    .answers(List.of())
                    .build();
            given(internalQuestionSetService.getAnswersResponse(5L, 1L)).willReturn(answersResponse);

            // when & then
            mockMvc.perform(get(BASE_URL + "/answers", 5L, 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.analysisStatus").value("PENDING"))
                    .andExpect(jsonPath("$.data.position").value("BACKEND"))
                    .andExpect(jsonPath("$.data.level").value("JUNIOR"))
                    .andExpect(jsonPath("$.data.answers").isArray());
        }
    }

    @Nested
    @DisplayName("POST /feedback 엔드포인트")
    class SaveFeedback {

        @Test
        @DisplayName("피드백 저장 성공 시 200을 반환한다")
        void saveFeedback_success() throws Exception {
            // given
            willDoNothing().given(internalQuestionSetService).saveFeedback(eq(1L), any());

            String requestBody = """
                    {
                        "questionSetComment": "전반적으로 좋은 답변입니다.",
                        "timestampFeedbacks": [
                            {
                                "questionId": 10,
                                "startMs": 0,
                                "endMs": 5000,
                                "verbalComment": {"positive": "명확한 설명", "negative": null, "suggestion": null},
                                "eyeContactLevel": "GOOD",
                                "postureLevel": "AVERAGE"
                            }
                        ]
                    }
                    """;

            // when & then
            mockMvc.perform(post(BASE_URL + "/feedback", 5L, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("questionSetComment 누락 시 400을 반환한다")
        void saveFeedback_missingComment_returns400() throws Exception {
            // given
            String requestBody = """
                    {
                    }
                    """;

            // when & then
            mockMvc.perform(post(BASE_URL + "/feedback", 5L, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("PUT /convert-status 엔드포인트")
    class UpdateConvertStatus {

        @Test
        @DisplayName("정상 요청 시 200 OK를 반환한다")
        void updateConvertStatus_success() throws Exception {
            // given
            willDoNothing().given(internalQuestionSetService).updateConvertStatus(eq(1L), any());

            String requestBody = """
                    {
                        "status": "PROCESSING"
                    }
                    """;

            // when & then
            mockMvc.perform(put(BASE_URL + "/convert-status", 5L, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("status 누락 시 400을 반환한다")
        void updateConvertStatus_missingStatus_returns400() throws Exception {
            // given
            String requestBody = "{}";

            // when & then
            mockMvc.perform(put(BASE_URL + "/convert-status", 5L, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /retry-analysis 엔드포인트")
    class RetryAnalysis {

        @Test
        @DisplayName("분석 재시도 성공 시 200을 반환한다")
        void retryAnalysis_success() throws Exception {
            // given
            willDoNothing().given(internalQuestionSetService).retryAnalysis(1L);

            // when & then
            mockMvc.perform(post(BASE_URL + "/retry-analysis", 5L, 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 질문세트 ID로 요청 시 404를 반환한다")
        void retryAnalysis_notFound_returns404() throws Exception {
            // given
            willThrow(new BusinessException(QuestionSetErrorCode.NOT_FOUND))
                    .given(internalQuestionSetService).retryAnalysis(999L);

            // when & then
            mockMvc.perform(post(BASE_URL + "/retry-analysis", 5L, 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("QUESTION_SET_001"));
        }
    }
}
