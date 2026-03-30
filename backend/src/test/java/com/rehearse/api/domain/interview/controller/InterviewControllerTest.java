package com.rehearse.api.domain.interview.controller;

import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.dto.UpdateStatusResponse;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.service.FollowUpService;
import com.rehearse.api.domain.interview.service.InterviewCreationService;
import com.rehearse.api.domain.interview.service.InterviewQueryService;
import com.rehearse.api.domain.interview.service.InterviewService;
import com.rehearse.api.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.rehearse.api.global.config.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterviewController.class)
@Import(TestSecurityConfig.class)
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterviewCreationService interviewCreationService;

    @MockitoBean
    private InterviewQueryService interviewQueryService;

    @MockitoBean
    private InterviewService interviewService;

    @MockitoBean
    private FollowUpService followUpService;

    @MockitoBean(name = "vtExecutor")
    private java.util.concurrent.Executor vtExecutor;

    @Test
    @DisplayName("POST /api/v1/interviews - 면접 세션 생성 성공 (201)")
    void createInterview_success() throws Exception {
        InterviewResponse response = createMockInterviewResponse();
        given(interviewCreationService.createInterview(any(), any())).willReturn(response);

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.position").value("BACKEND"))
                .andExpect(jsonPath("$.data.level").value("JUNIOR"))
                .andExpect(jsonPath("$.data.interviewTypes[0]").value("CS_FUNDAMENTAL"))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.questionGenerationStatus").value("PENDING"));
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
    @DisplayName("GET /api/v1/interviews/{id} - 조회 성공 (200)")
    void getInterview_success() throws Exception {
        InterviewResponse response = createMockInterviewResponse();
        given(interviewQueryService.getInterview(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/interviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.questionGenerationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id} - 존재하지 않는 세션 404")
    void getInterview_notFound() throws Exception {
        given(interviewQueryService.getInterview(999L))
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
        String requestJson = """
                {
                    "questionSetId": 1,
                    "answerText": "답변입니다."
                }
                """;

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, requestJson.getBytes());

        mockMvc.perform(multipart("/api/v1/interviews/1/follow-up")
                        .file(requestPart))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/retry-questions - 재시도 성공 (200)")
    void retryQuestionGeneration_success() throws Exception {
        InterviewResponse response = createMockInterviewResponse();
        given(interviewService.retryQuestionGeneration(1L)).willReturn(response);

        mockMvc.perform(post("/api/v1/interviews/1/retry-questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    private InterviewResponse createMockInterviewResponse() {
        return InterviewResponse.builder()
                .id(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .status(InterviewStatus.READY)
                .questionGenerationStatus(QuestionGenerationStatus.PENDING)
                .durationMinutes(30)
                .questionSets(Collections.emptyList())
                .createdAt(LocalDateTime.of(2026, 3, 10, 14, 30, 0))
                .build();
    }
}
