package com.rehearse.api.domain.questionset.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.questionset.dto.*;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.service.InternalQuestionSetService;
import com.rehearse.api.domain.questionset.service.QuestionSetService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.rehearse.api.global.config.TestSecurityConfig;
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

@WebMvcTest(value = QuestionSetController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = InternalApiKeyFilter.class))
@Import(TestSecurityConfig.class)
class QuestionSetControllerTest {

    private static final String BASE_URL =
            "/api/v1/interviews/{interviewId}/question-sets/{questionSetId}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QuestionSetService questionSetService;

    @MockitoBean
    private InternalQuestionSetService internalQuestionSetService;

    // ----------------------------------------------------------------
    // POST /answers
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /answers - 답변 저장 성공 시 201을 반환한다")
    void saveAnswers_success() throws Exception {
        willDoNothing().given(questionSetService).saveAnswers(eq(1L), any());

        String requestBody = """
                {
                    "answers": [
                        {"questionId": 10, "startMs": 0, "endMs": 5000}
                    ]
                }
                """;

        mockMvc.perform(post(BASE_URL + "/answers", 5L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /answers - answers 빈 배열 전송 시 400을 반환한다")
    void saveAnswers_emptyAnswers_returns400() throws Exception {
        String requestBody = """
                {
                    "answers": []
                }
                """;

        mockMvc.perform(post(BASE_URL + "/answers", 5L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ----------------------------------------------------------------
    // POST /upload-url
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /upload-url - Presigned URL 생성 성공 시 200과 uploadUrl을 반환한다")
    void generateUploadUrl_success() throws Exception {
        UploadUrlResponse response = UploadUrlResponse.builder()
                .uploadUrl("https://s3.example.com/presigned")
                .s3Key("videos/5/qs_1.webm")
                .fileMetadataId(100L)
                .build();
        given(questionSetService.generateUploadUrl(eq(5L), eq(1L), any())).willReturn(response);

        String requestBody = """
                {
                    "contentType": "video/webm"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/upload-url", 5L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadUrl").value("https://s3.example.com/presigned"))
                .andExpect(jsonPath("$.data.s3Key").value("videos/5/qs_1.webm"))
                .andExpect(jsonPath("$.data.fileMetadataId").value(100));
    }

    // ----------------------------------------------------------------
    // GET /status
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /status - 분석 상태 조회 성공 시 200과 analysisStatus를 반환한다")
    void getStatus_success() throws Exception {
        QuestionSetStatusResponse response = QuestionSetStatusResponse.builder()
                .id(1L)
                .analysisStatus(AnalysisStatus.ANALYZING)
                .build();
        given(questionSetService.getStatus(1L)).willReturn(response);

        mockMvc.perform(get(BASE_URL + "/status", 5L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.analysisStatus").value("ANALYZING"));
    }

    // ----------------------------------------------------------------
    // GET /feedback
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /feedback - 피드백 조회 성공 시 200과 피드백 데이터를 반환한다")
    void getFeedback_success() throws Exception {
        QuestionSetFeedbackResponse response = QuestionSetFeedbackResponse.builder()
                .id(50L)
                .questionSetComment("전반적으로 좋은 답변입니다.")
                .streamingUrl("https://s3.example.com/streaming")
                .fallbackUrl("https://s3.example.com/fallback")
                .timestampFeedbacks(List.of())
                .build();
        given(questionSetService.getFeedback(1L)).willReturn(response);

        mockMvc.perform(get(BASE_URL + "/feedback", 5L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(50))
                .andExpect(jsonPath("$.data.questionSetComment").value("전반적으로 좋은 답변입니다."))
                .andExpect(jsonPath("$.data.streamingUrl").value("https://s3.example.com/streaming"));
    }

    // ----------------------------------------------------------------
    // GET /questions-with-answers
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /questions-with-answers - 질문과 답변 타임스탬프 조회 성공 시 200을 반환한다")
    void getQuestionsWithAnswers_success() throws Exception {
        QuestionsWithAnswersResponse.QuestionWithAnswer item =
                QuestionsWithAnswersResponse.QuestionWithAnswer.builder()
                        .questionId(10L)
                        .questionType("MAIN")
                        .questionText("Java의 GC 동작 원리를 설명하세요.")
                        .startMs(0L)
                        .endMs(5000L)
                        .build();
        QuestionsWithAnswersResponse response = QuestionsWithAnswersResponse.builder()
                .questions(List.of(item))
                .build();
        given(questionSetService.getQuestionsWithAnswers(1L)).willReturn(response);

        mockMvc.perform(get(BASE_URL + "/questions-with-answers", 5L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions[0].questionId").value(10))
                .andExpect(jsonPath("$.data.questions[0].questionType").value("MAIN"));
    }

    // ----------------------------------------------------------------
    // 404 케이스
    // ----------------------------------------------------------------

    @Test
    @DisplayName("GET /status - 존재하지 않는 질문세트 조회 시 404를 반환한다")
    void getStatus_notFound_returns404() throws Exception {
        given(questionSetService.getStatus(999L))
                .willThrow(new BusinessException(QuestionSetErrorCode.NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/status", 5L, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_SET_001"));
    }

    @Test
    @DisplayName("GET /feedback - 피드백 미존재 시 404를 반환한다")
    void getFeedback_feedbackNotFound_returns404() throws Exception {
        given(questionSetService.getFeedback(1L))
                .willThrow(new BusinessException(QuestionSetErrorCode.FEEDBACK_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/feedback", 5L, 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_SET_003"));
    }
}
