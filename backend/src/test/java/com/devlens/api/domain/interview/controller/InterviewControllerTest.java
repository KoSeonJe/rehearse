package com.devlens.api.domain.interview.controller;

import com.devlens.api.domain.interview.dto.InterviewResponse;
import com.devlens.api.domain.interview.dto.QuestionResponse;
import com.devlens.api.domain.interview.dto.UpdateStatusResponse;
import com.devlens.api.domain.interview.entity.*;
import com.devlens.api.domain.interview.service.InterviewService;
import com.devlens.api.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterviewController.class)
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterviewService interviewService;

    @Test
    @DisplayName("POST /api/v1/interviews - 면접 세션 생성 성공 (201)")
    void createInterview_success() throws Exception {
        // given
        InterviewResponse response = createMockInterviewResponse();
        given(interviewService.createInterview(any(), any())).willReturn(response);

        String requestJson = """
                {
                    "position": "BACKEND",
                    "level": "JUNIOR",
                    "interviewTypes": ["CS_FUNDAMENTAL"],
                    "durationMinutes": 30
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        // when & then
        mockMvc.perform(multipart("/api/v1/interviews")
                        .file(requestPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.position").value("BACKEND"))
                .andExpect(jsonPath("$.data.level").value("JUNIOR"))
                .andExpect(jsonPath("$.data.interviewTypes[0]").value("CS_FUNDAMENTAL"))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/interviews - interviewTypes 빈 배열 시 400")
    void createInterview_emptyInterviewTypes() throws Exception {
        String requestJson = """
                {
                    "position": "BACKEND",
                    "level": "JUNIOR",
                    "interviewTypes": []
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(multipart("/api/v1/interviews")
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews - level 누락 시 400")
    void createInterview_missingLevel() throws Exception {
        String requestJson = """
                {
                    "position": "BACKEND",
                    "interviewTypes": ["CS_FUNDAMENTAL"]
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(multipart("/api/v1/interviews")
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews - Claude API 실패 시 502")
    void createInterview_claudeApiFail() throws Exception {
        given(interviewService.createInterview(any(), any()))
                .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "AI_001", "Claude API 호출 실패"));

        String requestJson = """
                {
                    "position": "BACKEND",
                    "level": "JUNIOR",
                    "interviewTypes": ["CS_FUNDAMENTAL"],
                    "durationMinutes": 30
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(multipart("/api/v1/interviews")
                        .file(requestPart))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_001"))
                .andExpect(jsonPath("$.message").value("Claude API 호출 실패"));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id} - 조회 성공 (200)")
    void getInterview_success() throws Exception {
        InterviewResponse response = createMockInterviewResponse();
        given(interviewService.getInterview(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/interviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.questions.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id} - 존재하지 않는 세션 404")
    void getInterview_notFound() throws Exception {
        given(interviewService.getInterview(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/interviews/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERVIEW_001"));
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{id}/status - 상태 변경 성공 (200)")
    void updateStatus_success() throws Exception {
        UpdateStatusResponse response = UpdateStatusResponse.builder().id(1L).status(InterviewStatus.IN_PROGRESS).build();
        given(interviewService.updateStatus(eq(1L), any())).willReturn(response);

        String requestBody = """
                {
                    "status": "IN_PROGRESS"
                }
                """;

        mockMvc.perform(patch("/api/v1/interviews/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{id}/status - 잘못된 상태 전이 409")
    void updateStatus_invalidTransition() throws Exception {
        given(interviewService.updateStatus(eq(1L), any()))
                .willThrow(new BusinessException(HttpStatus.CONFLICT, "INTERVIEW_002", "잘못된 상태 전이입니다."));

        String requestBody = """
                {
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(patch("/api/v1/interviews/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INTERVIEW_002"));
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{id}/status - status 누락 시 400")
    void updateStatus_missingStatus() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(patch("/api/v1/interviews/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/follow-up - questionContent 누락 시 400")
    void generateFollowUp_missingQuestionContent() throws Exception {
        String requestBody = """
                {
                    "answerText": "답변입니다."
                }
                """;

        mockMvc.perform(post("/api/v1/interviews/1/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/follow-up - answerText 누락 시 400")
    void generateFollowUp_missingAnswerText() throws Exception {
        String requestBody = """
                {
                    "questionContent": "질문입니다."
                }
                """;

        mockMvc.perform(post("/api/v1/interviews/1/follow-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private InterviewResponse createMockInterviewResponse() {
        QuestionResponse q1 = QuestionResponse.builder()
                .id(1L).content("HashMap과 TreeMap의 차이점은?").category("자료구조").order(1).build();
        QuestionResponse q2 = QuestionResponse.builder()
                .id(2L).content("프로세스와 스레드의 차이점은?").category("운영체제").order(2).build();

        return InterviewResponse.builder()
                .id(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .status(InterviewStatus.READY)
                .durationMinutes(30)
                .questions(List.of(q1, q2))
                .createdAt(LocalDateTime.of(2026, 3, 10, 14, 30, 0))
                .build();
    }
}
